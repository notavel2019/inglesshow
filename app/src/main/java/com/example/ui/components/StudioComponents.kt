package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.TempoStep
import com.example.ui.theme.LocalStudioColors

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val studioColors = LocalStudioColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(1.dp, studioColors.hairline),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun ClarityReadinessRing(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 116.dp
) {
    val studioColors = LocalStudioColors.current
    val colorScheme = MaterialTheme.colorScheme
    val clamped = score.coerceIn(0, 100)
    val animatedSweep by animateFloatAsState(
        targetValue = (clamped / 100f) * 280f,
        animationSpec = tween(durationMillis = 750),
        label = "clarity_ring_sweep"
    )
    val ringColor = when {
        clamped >= 85 -> studioColors.masteryHigh
        clamped >= 65 -> studioColors.masteryMid
        clamped > 0 -> studioColors.masteryLow
        else -> colorScheme.primary
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Clarity readiness score $clamped out of 100" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = 11.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = studioColors.hairline.copy(alpha = 0.7f),
                startAngle = 130f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedSweep > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = 130f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (clamped > 0) "$clamped" else "—",
                style = MaterialTheme.typography.displayMedium,
                color = colorScheme.onSurface
            )
            Text(
                text = "CLARITY",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LiveOscillogramCanvas(
    amplitudes: List<Float>,
    isRecording: Boolean,
    isSpeakingReference: Boolean,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    val colorScheme = MaterialTheme.colorScheme
    val barColor = when {
        isRecording -> colorScheme.secondary
        isSpeakingReference -> colorScheme.primary
        else -> colorScheme.outline
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(1.dp, studioColors.hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val count = amplitudes.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.56f).coerceAtLeast(3f)
        val midY = size.height / 2f

        // Center reference baseline
        drawLine(
            color = studioColors.hairline,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.5f
        )

        amplitudes.forEachIndexed { idx, amp ->
            val h = (amp.coerceIn(0.06f, 1f) * size.height * 0.88f)
            val x = idx * slotWidth + (slotWidth - barWidth) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, midY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun SpeedStepperDeck(
    selectedSpeed: TempoStep,
    onSelectSpeed: (TempoStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROGRESSIVE CADENCE",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
            Text(
                text = selectedSpeed.caption,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.surfaceVariant)
                .border(1.dp, studioColors.hairline, RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TempoStep.entries.forEach { step ->
                val selected = step == selectedSpeed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) colorScheme.primary else Color.Transparent)
                        .clickable { onSelectSpeed(step) }
                        .testTag("speed_step_${step.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = step.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MasteryPill(
    score: Int,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    val (bg, fg, label) = when {
        score >= 85 -> Triple(studioColors.masteryHighContainer, studioColors.masteryHigh, "$score% Clear")
        score >= 65 -> Triple(studioColors.masteryMidContainer, studioColors.masteryMid, "$score% Building")
        score > 0 -> Triple(studioColors.masteryLowContainer, studioColors.masteryLow, "$score% Focus")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "New")
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg
        )
    }
}

@Composable
fun ScoreMetricTile(
    label: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    val studioColors = LocalStudioColors.current
    val accent = when {
        score >= 85 -> studioColors.masteryHigh
        score >= 65 -> studioColors.masteryMid
        else -> studioColors.masteryLow
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.dp, studioColors.hairline, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$score",
            style = MaterialTheme.typography.headlineMedium,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
