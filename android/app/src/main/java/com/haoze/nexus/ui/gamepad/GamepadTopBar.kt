package com.haoze.nexus.ui.gamepad

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haoze.nexus.R
import com.haoze.nexus.ui.component.animation.ScreenEntranceState
import com.haoze.nexus.ui.component.animation.screenEntranceTopBar

@Composable
internal fun GamepadTopBar(
    entranceState: ScreenEntranceState,
    isConnected: Boolean,
    deviceName: String?,
    isEditMode: Boolean,
    isModified: Boolean,
    currentConsoleName: String,
    isVibrationEnabled: Boolean,
    onExit: () -> Unit,
    onOpenKeyboard: () -> Unit,
    onResetDefaults: () -> Unit,
    onToggleEditMode: () -> Unit,
    onCycleConsole: () -> Unit,
    onToggleVibration: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .screenEntranceTopBar(entranceState)
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Close
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onExit() }
                    .padding(horizontal = 8.dp)
                    .testTag("exit_gamepad_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Close, "Exit", tint = Color.White, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(4.dp))
                Text("Close", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Mode cycle
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onOpenKeyboard() }
                    .padding(horizontal = 8.dp)
                    .testTag("gamepad_mode_cycle_btn"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.SportsEsports, "Switch Mode", tint = Color.White, modifier = Modifier.size(11.dp))
                Text("Gamepad Mode", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Connection status
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (isConnected) Color(0xFF39FF14) else Color(0xFFFF9800)))
            Text(deviceName ?: "No Host", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
            Text(if (isConnected) "[connected]" else "[offline]", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = FontFamily.SansSerif)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Reset Defaults (only shown when edit mode is active and there are layout changes)
            if (isEditMode && isModified) {
                Row(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEF5350).copy(alpha = 0.35f))
                        .clickable { onResetDefaults() }
                        .padding(horizontal = 8.dp)
                        .testTag("reset_layout_defaults_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Refresh, "Reset Defaults", tint = Color.White, modifier = Modifier.size(11.dp))
                    Text("Reset Defaults", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Edit Layout toggle
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onToggleEditMode() }
                    .padding(horizontal = 8.dp)
                    .testTag("edit_layout_toggle_btn"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = "Edit Layout",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (isEditMode) "Done" else "Edit Layout",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Console layout selector
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onCycleConsole() }
                    .padding(horizontal = 8.dp)
                    .testTag("console_selector_pill"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SportsEsports, "Consoles", tint = Color.White, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(currentConsoleName, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            // Vibration toggle
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onToggleVibration() }
                    .testTag("vibration_gamepad_toggle"),
                contentAlignment = Alignment.Center
            ) {
                if (isVibrationEnabled) {
                    Icon(Icons.Default.Vibration, "Haptics", tint = Color.White, modifier = Modifier.size(12.dp))
                } else {
                    Icon(painterResource(R.drawable.ic_vibration_off), "Haptics", tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
