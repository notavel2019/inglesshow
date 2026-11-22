package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.AirflowType
import com.example.domain.ArticulationPose
import com.example.domain.GenAmCatalog
import com.example.domain.GenAmPhoneme
import com.example.ui.theme.LocalStudioColors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Interactive 2D Midsagittal Vocal-Tract Biomechanics Visualizer.
 * Animates tongue cubic-spline trajectory, jaw drop, lip rounding/aperture,
 * velum/soft-palate nasal valve, vocal fold vibration waves, airflow stream vectors,
 * and optional ghosted contrast overlays (e.g., /θ/ vs /s/).
 */
@Composable
fun MouthMechanicsCanvas(
    phoneme: GenAmPhoneme,
    contrastOverlay: Pair<String, ArticulationPose>?,
    showLabels: Boolean,
    loopingAnimation: Boolean,
    manualPhase: Float,
    modifier: Modifier = Modifier,
    canvasHeight: Dp = 270.dp
) {
    val studioColors = LocalStudioColors.current
    val colorScheme = MaterialTheme.colorScheme

    val infiniteTransition = rememberInfiniteTransition(label = "mouth_articulation_cycle")
    val autoPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycle_phase"
    )
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "airflow_phase"
    )

    val activePhase = if (loopingAnimation) autoPhase else manualPhase
    // Articulation envelope: 0..0.25 onset, 0.25..0.78 hold peak, 0.78..1.0 release
    val intensity = when {
        activePhase < 0.25f -> (activePhase / 0.25f).coerceIn(0f, 1f)
        activePhase <= 0.78f -> 1f
        else -> (1f - ((activePhase - 0.78f) / 0.22f)).coerceIn(0f, 1f)
    }

    val rest = GenAmCatalog.restPose
    val target = phoneme.pose
    val springSpec = spring<Float>(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)

    val tipX by animateFloatAsState(lerp(rest.tongueTipX, target.tongueTipX, intensity), springSpec, label = "tipX")
    val tipY by animateFloatAsState(lerp(rest.tongueTipY, target.tongueTipY, intensity), springSpec, label = "tipY")
    val bladeX by animateFloatAsState(lerp(rest.tongueBladeX, target.tongueBladeX, intensity), springSpec, label = "bladeX")
    val bladeY by animateFloatAsState(lerp(rest.tongueBladeY, target.tongueBladeY, intensity), springSpec, label = "bladeY")
    val dorsumX by animateFloatAsState(lerp(rest.tongueDorsumX, target.tongueDorsumX, intensity), springSpec, label = "dorsumX")
    val dorsumY by animateFloatAsState(lerp(rest.tongueDorsumY, target.tongueDorsumY, intensity), springSpec, label = "dorsumY")
    val rootX by animateFloatAsState(lerp(rest.tongueRootX, target.tongueRootX, intensity), springSpec, label = "rootX")
    val rootY by animateFloatAsState(lerp(rest.tongueRootY, target.tongueRootY, intensity), springSpec, label = "rootY")
    val jawDrop by animateFloatAsState(lerp(rest.jawDrop, target.jawDrop, intensity), springSpec, label = "jawDrop")
    val lipRound by animateFloatAsState(lerp(rest.lipRounding, target.lipRounding, intensity), springSpec, label = "lipRound")
    val velumOpen by animateFloatAsState(lerp(rest.velumOpen, target.velumOpen, intensity), springSpec, label = "velumOpen")

    val currentPose = target.copy(
        jawDrop = jawDrop,
        lipRounding = lipRound,
        tongueTipX = tipX,
        tongueTipY = tipY,
        tongueBladeX = bladeX,
        tongueBladeY = bladeY,
        tongueDorsumX = dorsumX,
        tongueDorsumY = dorsumY,
        tongueRootX = rootX,
        tongueRootY = rootY,
        velumOpen = velumOpen
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(canvasHeight)
            .clip(RoundedCornerShape(22.dp))
            .background(colorScheme.surface)
            .border(1.dp, studioColors.hairline, RoundedCornerShape(22.dp))
            .testTag("mouth_mechanics_canvas")
            .semantics {
                contentDescription = "2D vocal tract diagram for ${phoneme.name} ${phoneme.ipa}: ${phoneme.pose.contactLabel}"
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawStudioGrid(studioColors.hairline.copy(alpha = 0.45f))
            drawVocalTractAnatomy(
                pose = currentPose,
                outlineColor = studioColors.vocalTractOutline,
                cavityColor = studioColors.vocalTractFill,
                primaryTeal = colorScheme.primary,
                vermilion = colorScheme.secondary,
                amber = colorScheme.tertiary,
                flowPhase = flowPhase,
                intensity = intensity
            )
            if (contrastOverlay != null) {
                drawGhostContrastTongue(
                    pose = contrastOverlay.second,
                    ghostColor = colorScheme.primary
                )
            }
            drawLiveTongue(
                pose = currentPose,
                tongueFill = colorScheme.secondary.copy(alpha = 0.24f),
                tongueStroke = colorScheme.secondary
            )
            drawArticulationContactMarker(
                pose = currentPose,
                accent = colorScheme.secondary,
                intensity = intensity
            )
        }

        // Clean Studio Overlay Tags
        if (showLabels) {
            Text(
                text = "PALATE",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
            )
            Text(
                text = "LIPS & TEETH",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp, bottom = 54.dp)
            )
            Text(
                text = if (target.voiced) "VOICED (BUZZ)" else "VOICELESS",
                style = MaterialTheme.typography.labelSmall,
                color = if (target.voiced) colorScheme.primary else colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 12.dp)
            )
            Text(
                text = target.contactLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 12.dp)
            )
        }

        if (contrastOverlay != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.primaryContainer.copy(alpha = 0.92f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Dashed: ${contrastOverlay.first}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

private fun DrawScope.drawStudioGrid(gridColor: Color) {
    val stepX = size.width / 12f
    val stepY = size.height / 8f
    for (i in 1 until 12) {
        val x = i * stepX
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
    }
    for (j in 1 until 8) {
        val y = j * stepY
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    }
}

private fun DrawScope.drawVocalTractAnatomy(
    pose: ArticulationPose,
    outlineColor: Color,
    cavityColor: Color,
    primaryTeal: Color,
    vermilion: Color,
    amber: Color,
    flowPhase: Float,
    intensity: Float
) {
    val w = size.width
    val h = size.height
    val jawShift = pose.jawDrop * h * 0.13f
    val lipProtrude = pose.lipRounding * w * 0.045f

    // 1. Nasal Cavity background arch
    val nasalPath = Path().apply {
        moveTo(w * 0.20f, h * 0.24f)
        cubicTo(w * 0.34f, h * 0.10f, w * 0.66f, h * 0.10f, w * 0.80f, h * 0.28f)
        lineTo(w * 0.82f, h * 0.88f)
        lineTo(w * 0.64f, h * 0.88f)
        lineTo(w * 0.64f, h * 0.66f)
        lineTo(w * 0.22f, h * 0.66f + jawShift)
        close()
    }
    drawPath(nasalPath, color = cavityColor.copy(alpha = 0.65f))

    // 2. Hard Palate & Alveolar Ridge (upper roof of mouth)
    val palatePath = Path().apply {
        moveTo(w * 0.22f, h * 0.38f)
        // Alveolar ridge bump behind upper incisors
        cubicTo(w * 0.26f, h * 0.30f, w * 0.34f, h * 0.26f, w * 0.46f, h * 0.25f)
        // Hard palate dome
        cubicTo(w * 0.56f, h * 0.24f, w * 0.63f, h * 0.27f, w * 0.68f, h * 0.32f)
    }
    drawPath(
        path = palatePath,
        color = outlineColor,
        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 3. Soft Palate / Velum (hinges downward when velumOpen > 0 for nasal /ŋ/)
    val velumDropX = w * (0.76f - pose.velumOpen * 0.07f)
    val velumDropY = h * (0.34f + pose.velumOpen * 0.14f)
    val velumPath = Path().apply {
        moveTo(w * 0.68f, h * 0.32f)
        quadraticTo(w * 0.73f, h * 0.35f, velumDropX, velumDropY)
    }
    drawPath(
        path = velumPath,
        color = if (pose.velumOpen > 0.3f) primaryTeal else outlineColor,
        style = Stroke(width = 6f, cap = StrokeCap.Round)
    )

    // 4. Posterior Pharyngeal Wall & Upper Nasal Roof
    val wallPath = Path().apply {
        moveTo(w * 0.22f, h * 0.22f)
        cubicTo(w * 0.42f, h * 0.11f, w * 0.72f, h * 0.12f, w * 0.82f, h * 0.30f)
        lineTo(w * 0.83f, h * 0.88f)
    }
    drawPath(
        path = wallPath,
        color = outlineColor.copy(alpha = 0.55f),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )

    // 5. Upper & Lower Lips (protrude left when lipRounding is high)
    val upperLipPath = Path().apply {
        moveTo(w * 0.18f, h * 0.24f)
        cubicTo(w * 0.14f - lipProtrude, h * 0.34f, w * 0.11f - lipProtrude, h * 0.41f, w * 0.16f - lipProtrude * 0.5f, h * 0.43f)
        lineTo(w * 0.21f, h * 0.40f)
    }
    drawPath(
        path = upperLipPath,
        color = outlineColor,
        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val lowerLipY = h * 0.49f + jawShift
    val lowerLipPath = Path().apply {
        moveTo(w * 0.21f, lowerLipY + h * 0.03f)
        lineTo(w * 0.15f - lipProtrude * 0.5f, lowerLipY)
        cubicTo(w * 0.11f - lipProtrude, lowerLipY + h * 0.05f, w * 0.15f, lowerLipY + h * 0.15f, w * 0.26f, h * 0.80f + jawShift * 0.4f)
    }
    drawPath(
        path = lowerLipPath,
        color = outlineColor,
        style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 6. Upper & Lower Incisor Teeth
    val upperTooth = Path().apply {
        moveTo(w * 0.205f, h * 0.385f)
        lineTo(w * 0.20f, h * 0.448f)
        lineTo(w * 0.232f, h * 0.444f)
        lineTo(w * 0.235f, h * 0.382f)
        close()
    }
    drawPath(upperTooth, color = Color.White)
    drawPath(upperTooth, color = outlineColor, style = Stroke(width = 2.6f, join = StrokeJoin.Round))

    val lowerToothTop = h * 0.50f + jawShift
    val lowerTooth = Path().apply {
        moveTo(w * 0.215f, lowerToothTop)
        lineTo(w * 0.210f, lowerToothTop + h * 0.056f)
        lineTo(w * 0.242f, lowerToothTop + h * 0.056f)
        lineTo(w * 0.244f, lowerToothTop)
        close()
    }
    drawPath(lowerTooth, color = Color.White)
    drawPath(lowerTooth, color = outlineColor, style = Stroke(width = 2.6f, join = StrokeJoin.Round))

    // 7. Vocal Fold Vibration Waves at Larynx (bottom right)
    val larynxCenter = Offset(w * 0.74f, h * 0.81f)
    if (pose.voiced && intensity > 0.2f) {
        val pulse = 0.65f + 0.35f * sin(flowPhase * 2f * PI.toFloat())
        for (ring in 1..3) {
            val radius = (10f + ring * 9f) * (0.85f + 0.15f * pulse)
            drawCircle(
                color = primaryTeal.copy(alpha = (0.55f - ring * 0.14f) * intensity),
                radius = radius,
                center = larynxCenter,
                style = Stroke(width = 3f)
            )
        }
        drawCircle(color = primaryTeal, radius = 6f, center = larynxCenter)
    } else {
        drawCircle(
            color = outlineColor.copy(alpha = 0.35f),
            radius = 7f,
            center = larynxCenter,
            style = Stroke(width = 2.5f)
        )
    }

    // 8. Animated Airflow Streamlines
    if (intensity > 0.25f) {
        val dashOffset = -flowPhase * 42f
        val streamColor = when (pose.airflowType) {
            AirflowType.CONTINUOUS_ORAL -> primaryTeal
            AirflowType.NASAL_CHANNEL -> amber
            AirflowType.PLOSIVE_TAP -> vermilion
            AirflowType.LAMINAR_OPEN -> primaryTeal.copy(alpha = 0.75f)
        }
        val airflowPath = if (pose.airflowType == AirflowType.NASAL_CHANNEL) {
            Path().apply {
                moveTo(w * 0.76f, h * 0.78f)
                cubicTo(w * 0.78f, h * 0.45f, w * 0.74f, h * 0.19f, w * 0.50f, h * 0.18f)
                cubicTo(w * 0.34f, h * 0.18f, w * 0.22f, h * 0.21f, w * 0.08f, h * 0.24f)
            }
        } else {
            Path().apply {
                moveTo(w * 0.76f, h * 0.78f)
                cubicTo(
                    w * 0.76f, h * 0.42f,
                    w * (pose.tongueDorsumX + 0.02f), h * (pose.tongueDorsumY - 0.12f),
                    w * (pose.tongueBladeX), h * (pose.tongueBladeY - 0.09f)
                )
                cubicTo(
                    w * (pose.tongueTipX + 0.04f), h * (pose.tongueTipY - 0.04f),
                    w * 0.16f, h * 0.45f + jawShift * 0.35f,
                    w * 0.05f, h * 0.45f + jawShift * 0.35f
                )
            }
        }
        drawPath(
            path = airflowPath,
            color = streamColor.copy(alpha = 0.85f * intensity),
            style = Stroke(
                width = 4f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), dashOffset)
            )
        )
    }
}

private fun DrawScope.drawGhostContrastTongue(
    pose: ArticulationPose,
    ghostColor: Color
) {
    val path = buildTongueContourPath(pose)
    drawPath(
        path = path,
        color = ghostColor.copy(alpha = 0.65f),
        style = Stroke(
            width = 4.5f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        )
    )
}

private fun DrawScope.drawLiveTongue(
    pose: ArticulationPose,
    tongueFill: Color,
    tongueStroke: Color
) {
    val w = size.width
    val h = size.height
    val jawShift = pose.jawDrop * h * 0.10f

    val filledPath = buildTongueContourPath(pose).apply {
        lineTo(w * 0.66f, h * 0.86f)
        lineTo(w * 0.30f, h * 0.78f + jawShift)
        close()
    }
    drawPath(
        path = filledPath,
        brush = Brush.verticalGradient(
            colors = listOf(tongueFill.copy(alpha = 0.38f), tongueFill.copy(alpha = 0.12f))
        )
    )
    drawPath(
        path = buildTongueContourPath(pose),
        color = tongueStroke,
        style = Stroke(width = 6.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.buildTongueContourPath(pose: ArticulationPose): Path {
    val w = size.width
    val h = size.height
    return Path().apply {
        moveTo(w * pose.tongueTipX, h * pose.tongueTipY)
        cubicTo(
            w * ((pose.tongueTipX + pose.tongueBladeX) / 2f), h * pose.tongueTipY,
            w * (pose.tongueBladeX - 0.04f), h * pose.tongueBladeY,
            w * pose.tongueBladeX, h * pose.tongueBladeY
        )
        cubicTo(
            w * ((pose.tongueBladeX + pose.tongueDorsumX) / 2f), h * pose.tongueBladeY,
            w * (pose.tongueDorsumX - 0.03f), h * pose.tongueDorsumY,
            w * pose.tongueDorsumX, h * pose.tongueDorsumY
        )
        cubicTo(
            w * (pose.tongueDorsumX + 0.08f), h * (pose.tongueDorsumY + 0.05f),
            w * pose.tongueRootX, h * (pose.tongueRootY - 0.04f),
            w * pose.tongueRootX, h * pose.tongueRootY
        )
    }
}

private fun DrawScope.drawArticulationContactMarker(
    pose: ArticulationPose,
    accent: Color,
    intensity: Float
) {
    if (intensity < 0.45f) return
    val w = size.width
    val h = size.height
    val focalPoint = if (pose.airflowType == AirflowType.NASAL_CHANNEL) {
        Offset(w * pose.tongueDorsumX, h * pose.tongueDorsumY)
    } else {
        Offset(w * pose.tongueTipX, h * pose.tongueTipY)
    }
    drawCircle(
        color = accent.copy(alpha = 0.22f * intensity),
        radius = 18f,
        center = focalPoint
    )
    drawCircle(
        color = accent,
        radius = 6.5f,
        center = focalPoint
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
