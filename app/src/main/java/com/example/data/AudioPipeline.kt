package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.sqrt

private const val TAG = "AudioPipeline"
private const val SAMPLE_RATE = 16_000
private const val MAX_RECORD_SECONDS = 15
private const val MIN_RECORD_BYTES = SAMPLE_RATE * 2 * 6 / 10 // 0.6s of 16kHz 16-bit mono

/**
 * Real Android audio engine powering:
 * 1. General American reference TTS playback with exact speech-rate stepping
 * 2. 16 kHz mono PCM microphone capture with live RMS oscillogram & in-memory WAV encoding
 * 3. Side-by-side local AudioTrack playback of the learner's captured take
 */
class AudioPipeline(
    context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val _isSpeakingReference = MutableStateFlow(false)
    val isSpeakingReference: StateFlow<Boolean> = _isSpeakingReference.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlayingRecording = MutableStateFlow(false)
    val isPlayingRecording: StateFlow<Boolean> = _isPlayingRecording.asStateFlow()

    private val _liveAmplitudes = MutableStateFlow(List(44) { 0.08f })
    val liveAmplitudes: StateFlow<List<Float>> = _liveAmplitudes.asStateFlow()

    private val _recordedDurationSec = MutableStateFlow(0f)
    val recordedDurationSec: StateFlow<Float> = _recordedDurationSec.asStateFlow()

    private var recordJob: Job? = null
    private var playbackJob: Job? = null
    private var activeRecorder: AudioRecord? = null
    private var pcmBuffer = ByteArrayOutputStream()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale.US)
                ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeakingReference.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeakingReference.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeakingReference.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeakingReference.value = false
                    }
                })
            } else {
                Log.w(TAG, "TextToSpeech initialization returned status=$status")
            }
        }
    }

    fun speakReference(text: String, speedMultiplier: Float) {
        stopUserPlayback()
        if (!ttsReady) {
            Log.w(TAG, "TTS not ready yet")
            return
        }
        tts?.setSpeechRate(speedMultiplier.coerceIn(0.55f, 1.25f))
        tts?.setPitch(1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voxform_ref_${System.currentTimeMillis()}")
    }

    fun stopReference() {
        tts?.stop()
        _isSpeakingReference.value = false
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        onMaxDurationReached: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        stopReference()
        stopUserPlayback()
        if (_isRecording.value) return

        val minBufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize * 2
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to construct AudioRecord", e)
            onError("Could not initialize microphone: ${e.localizedMessage ?: "hardware unavailable"}")
            return
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            onError("Microphone hardware could not be initialized. Please check audio permissions.")
            return
        }

        activeRecorder = recorder
        pcmBuffer = ByteArrayOutputStream()
        _recordedDurationSec.value = 0f
        _liveAmplitudes.value = List(44) { 0.08f }
        _isRecording.value = true

        recordJob = scope.launch(Dispatchers.IO) {
            val readBuffer = ShortArray(1024)
            val byteChunk = ByteArray(readBuffer.size * 2)
            val maxBytes = SAMPLE_RATE * 2 * MAX_RECORD_SECONDS
            try {
                recorder.startRecording()
                while (isActive && _isRecording.value) {
                    val readCount = recorder.read(readBuffer, 0, readBuffer.size)
                    if (readCount > 0) {
                        var sumSquares = 0.0
                        for (i in 0 until readCount) {
                            val sample = readBuffer[i]
                            sumSquares += sample.toDouble() * sample.toDouble()
                            byteChunk[i * 2] = (sample.toInt() and 0xFF).toByte()
                            byteChunk[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                        }
                        pcmBuffer.write(byteChunk, 0, readCount * 2)

                        val rms = sqrt(sumSquares / readCount)
                        val normalized = (rms / 6500.0).toFloat().coerceIn(0.06f, 1.0f)
                        val current = _liveAmplitudes.value
                        _liveAmplitudes.value = current.drop(1) + normalized
                        _recordedDurationSec.value = pcmBuffer.size().toFloat() / (SAMPLE_RATE * 2f)

                        if (pcmBuffer.size() >= maxBytes) {
                            _isRecording.value = false
                            val wav = encodeWav(pcmBuffer.toByteArray())
                            withContext(Dispatchers.Main) {
                                onMaxDurationReached(wav)
                            }
                            break
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Recording stream error", e)
                withContext(Dispatchers.Main) {
                    onError("Recording interrupted: ${e.localizedMessage ?: "audio error"}")
                }
            } finally {
                try {
                    if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        recorder.stop()
                    }
                } catch (_: Exception) {
                }
                recorder.release()
                activeRecorder = null
                _isRecording.value = false
            }
        }
    }

    suspend fun stopRecording(): Result<ByteArray> {
        _isRecording.value = false
        recordJob?.join()
        recordJob = null
        val pcmBytes = pcmBuffer.toByteArray()
        if (pcmBytes.size < MIN_RECORD_BYTES) {
            return Result.failure(IllegalStateException("Recording too short. Speak for at least 1 second and try again."))
        }
        return Result.success(encodeWav(pcmBytes))
    }

    fun playUserRecording(wavBytes: ByteArray) {
        stopReference()
        stopUserPlayback()
        if (wavBytes.size <= 44) return

        val pcmData = wavBytes.copyOfRange(44, wavBytes.size)
        _isPlayingRecording.value = true

        playbackJob = scope.launch(Dispatchers.IO) {
            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                track.play()
                var offset = 0
                val chunkSize = 2048
                while (isActive && offset < pcmData.size) {
                    val toWrite = minOf(chunkSize, pcmData.size - offset)
                    val written = track.write(pcmData, offset, toWrite)
                    if (written <= 0) break
                    offset += written
                }
                track.stop()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Playback error", e)
            } finally {
                try {
                    track?.release()
                } catch (_: Exception) {
                }
                _isPlayingRecording.value = false
            }
        }
    }

    fun stopUserPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _isPlayingRecording.value = false
    }

    fun release() {
        _isRecording.value = false
        recordJob?.cancel()
        stopUserPlayback()
        stopReference()
        tts?.shutdown()
        tts = null
    }

    private fun encodeWav(pcmData: ByteArray): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = SAMPLE_RATE * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalDataLen)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM chunk size
            putShort(1) // PCM format
            putShort(1) // Mono
            putInt(SAMPLE_RATE)
            putInt(byteRate)
            putShort(2) // Block align
            putShort(16) // Bits per sample
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmData.size)
        }.array()

        return ByteArray(header.size + pcmData.size).also { out ->
            System.arraycopy(header, 0, out, 0, header.size)
            System.arraycopy(pcmData, 0, out, header.size, pcmData.size)
        }
    }
}
