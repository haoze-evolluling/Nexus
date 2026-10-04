package com.haoze.nexus.ui.gamepad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun EditableComponentWrapper(
    isEditMode: Boolean,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    onOffsetChange: (Float, Float) -> Unit,
    onScaleChange: (Float) -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current.density
    
    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)
    val currentScale by rememberUpdatedState(scale)
    val currentOnOffsetChange by rememberUpdatedState(onOffsetChange)
    val currentOnScaleChange by rememberUpdatedState(onScaleChange)
    
    var layoutTopInWindowPx by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                layoutTopInWindowPx = coordinates.positionInWindow().y
            }
            .offset {
                val layoutTopInWindow = layoutTopInWindowPx / density
                val constrainedOffsetY = if (layoutTopInWindow > 0) {
                    val minY = 38f + 4f - layoutTopInWindow
                    offsetY.coerceAtLeast(minY)
                } else {
                    offsetY
                }
                IntOffset((offsetX * density).roundToInt(), (constrainedOffsetY * density).roundToInt())
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        content()
        
        if (isEditMode) {
            // 1. Overlay container with border that intercepts gestures for moving and pinch zoom
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = 1.08f
                        scaleY = 1.08f
                    }
                    .border(
                        width = 1.2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            // 1. Update scale via pinch zoom
                            val newScale = (currentScale * zoom).coerceIn(0.6f, 1.8f)
                            currentOnScaleChange(newScale)
                            
                            // 2. Update offset with scale factor correction and topbar constraint
                            val minY = 38f + 4f - layoutTopInWindowPx / density
                            val newX = currentOffsetX + (pan.x * currentScale) / density
                            val newY = (currentOffsetY + (pan.y * currentScale) / density).coerceAtLeast(minY)
                            currentOnOffsetChange(newX, newY)
                        }
                    }
            )
            
            // 2. Drag resize handle in bottom-right corner (Alternative scaling option)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaScale = (dragAmount.x + dragAmount.y) / 150f
                            currentOnScaleChange((currentScale + deltaScale).coerceIn(0.6f, 1.8f))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Resize",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
