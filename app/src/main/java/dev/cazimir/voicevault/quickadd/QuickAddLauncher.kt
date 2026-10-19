package dev.cazimir.voicevault.quickadd

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dev.cazimir.voicevault.settings.CaptureSettings

object QuickAddLauncher {

    /**
     * Copies the transcribed text to the clipboard (for macros that paste) and
     * generates the Intent to launch Obsidian with the QuickAdd URI.
     */
    fun buildIntent(context: Context, transcribedText: String): Intent {
        // 1. Copy to clipboard so {{CLIPBOARD}} macros work out of the box
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Voice Vault Transcription", transcribedText)
        clipboard.setPrimaryClip(clip)

        // 2. Build the QuickAdd URI
        val vaultName = CaptureSettings.getVaultName(context)
        val macroName = CaptureSettings.getMacroName(context)
        val customVarName = CaptureSettings.getVariableName(context)

        val uriBuilder = "obsidian://quickadd".toUri().buildUpon()
        if (vaultName.isNotBlank()) {
            uriBuilder.appendQueryParameter("vault", vaultName)
        }
        if (macroName.isNotBlank()) {
            uriBuilder.appendQueryParameter("choice", macroName)
        }

        // Pass custom user variable name first if specified (e.g. "What happened?")
        if (customVarName.isNotBlank()) {
            uriBuilder.appendQueryParameter(customVarName, transcribedText)
        }

        // Pass common fallback variable parameter keys used in QuickAdd macros
        val fallbackKeys = listOf("text", "value", "input", "value-text", "VALUE")
        for (key in fallbackKeys) {
            if (key != customVarName) {
                uriBuilder.appendQueryParameter(key, transcribedText)
            }
        }

        val intent = Intent(Intent.ACTION_VIEW, uriBuilder.build())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    fun launch(context: Context, transcribedText: String) {
        val intent = buildIntent(context, transcribedText)
        context.startActivity(intent)
    }
}
