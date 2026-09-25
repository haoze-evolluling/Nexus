package com.haoze.nexus.macro

import android.content.Context
import com.haoze.nexus.R

/**
 * Preset macros for Claude Code CLI.
 */
object PresetMacros {

    /**
     * Get the list of preset macros.
     */
    fun getPresets(context: Context): List<Macro> {
        return listOf(
            Macro(
                id = "preset__clear",
                label = "/clear",
                command = "/clear",
                description = context.getString(R.string.preset_clear_desc),
                isPreset = true,
                sortOrder = 1,
                sendEnter = true
            ),
            Macro(
                id = "preset__compact",
                label = "/compact",
                command = "/compact",
                description = context.getString(R.string.preset_compact_desc),
                isPreset = true,
                sortOrder = 2,
                sendEnter = true
            ),
            Macro(
                id = "preset__model",
                label = "/model",
                command = "/model",
                description = context.getString(R.string.preset_model_desc),
                isPreset = true,
                sortOrder = 3,
                sendEnter = true
            ),
            Macro(
                id = "preset__btw",
                label = "/btw",
                command = "/btw",
                description = context.getString(R.string.preset_btw_desc),
                isPreset = true,
                sortOrder = 4,
                sendEnter = true
            )
        )
    }
}
