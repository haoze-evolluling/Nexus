package com.haoze.nexus.ui.audio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.haoze.nexus.R
import com.haoze.nexus.audio.CalibrationPhase
import com.haoze.nexus.audio.CalibrationState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
internal fun calibrationStepLabels(): List<String> = listOf(
    stringResource(R.string.calib_step_detect),
    stringResource(R.string.calib_step_calculate),
    stringResource(R.string.calib_step_sync),
    stringResource(R.string.calib_step_done),
)

@Composable
internal fun CalibrationPhase.label(): String = when (this) {
    CalibrationPhase.DETECT -> stringResource(R.string.calib_phase_detect)
    CalibrationPhase.CALCULATE -> stringResource(R.string.calib_phase_calculate)
    CalibrationPhase.SYNC -> stringResource(R.string.calib_phase_sync)
    CalibrationPhase.DONE -> stringResource(R.string.calib_phase_done)
}

@Composable
internal fun CalibrationPanel(state: CalibrationState, done: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CalibrationWave(Modifier.size(width = 64.dp, height = 28.dp))
                Spacer(Modifier.width(14.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (done) stringResource(R.string.calib_synced) else stringResource(R.string.calibrating),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                    val stats = if (done || state.phase == CalibrationPhase.SYNC) {
                        val offset = state.offsetMs
                        val rtt = state.rttMs
                        if (offset != null && rtt != null) stringResource(R.string.calib_stats, abs(offset), rtt) else null
                    } else null
                    Text(
                        stats ?: state.phase.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            CalibrationSteps(state.phase)
        }
    }
}

@Composable
internal fun CalibrationWave(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "calib-wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "calib-wave-phase",
    )
    val barColor = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val bars = 12
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (bars - 1)) / bars
        val mid = size.height / 2f
        for (i in 0 until bars) {
            val level = abs(sin(phase + i * 0.55f)) * 0.85f + 0.15f
            val h = size.height * level * 0.9f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(i * (barWidth + gap), mid - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

@Composable
internal fun CalibrationSteps(current: CalibrationPhase) {
    val activeIndex = when (current) {
        CalibrationPhase.DETECT -> 0
        CalibrationPhase.CALCULATE -> 1
        CalibrationPhase.SYNC -> 2
        CalibrationPhase.DONE -> 3
    }
    val pulse = rememberInfiniteTransition(label = "step-pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "step-pulse-scale",
    )
    val connectorColor = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(10.dp)) {
            val nodeSpacing = size.width / 4f
            val connectorY = size.height / 2f
            for (index in 1 until 4) {
                drawLine(
                    color = connectorColor.copy(alpha = if (index <= activeIndex) 0.7f else 0.25f),
                    start = Offset(nodeSpacing * (index - 0.5f), connectorY),
                    end = Offset(nodeSpacing * (index + 0.5f), connectorY),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            calibrationStepLabels().forEachIndexed { index, label ->
                val active = index == activeIndex
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (index <= activeIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(10.dp).then(if (active) Modifier.scale(scale) else Modifier),
                    ) {}
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index <= activeIndex) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}
