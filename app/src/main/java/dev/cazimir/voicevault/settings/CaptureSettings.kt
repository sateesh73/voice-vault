package dev.cazimir.voicevault.settings

import android.content.Context

enum class CaptureMode {
    QUICK_NOTE,
    QUICK_ADD
}

object CaptureSettings {
    private const val PREFS = "capture_settings"
    private const val KEY_CAPTURE_MODE = "capture_mode"
    private const val KEY_VAULT_NAME = "vault_name"
    private const val KEY_MACRO_NAME = "macro_name"
    private const val KEY_VARIABLE_NAME = "variable_name"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getCaptureMode(context: Context): CaptureMode {
        val modeStr = prefs(context).getString(KEY_CAPTURE_MODE, CaptureMode.QUICK_NOTE.name)
        return try {
            CaptureMode.valueOf(modeStr ?: CaptureMode.QUICK_NOTE.name)
        } catch (e: IllegalArgumentException) {
            CaptureMode.QUICK_NOTE
        }
    }

    fun setCaptureMode(context: Context, mode: CaptureMode) {
        prefs(context).edit().putString(KEY_CAPTURE_MODE, mode.name).apply()
    }

    fun getVaultName(context: Context): String {
        return prefs(context).getString(KEY_VAULT_NAME, "") ?: ""
    }

    fun setVaultName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_VAULT_NAME, name).apply()
    }

    fun getMacroName(context: Context): String {
        return prefs(context).getString(KEY_MACRO_NAME, "") ?: ""
    }

    fun setMacroName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_MACRO_NAME, name).apply()
    }

    fun getVariableName(context: Context): String {
        return prefs(context).getString(KEY_VARIABLE_NAME, "") ?: ""
    }

    fun setVariableName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_VARIABLE_NAME, name).apply()
    }
}
