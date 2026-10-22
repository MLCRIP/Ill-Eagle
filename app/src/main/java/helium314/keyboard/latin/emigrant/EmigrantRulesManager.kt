package helium314.keyboard.latin.emigrant

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.prefs
import androidx.core.content.edit
import java.io.File

/**
 * Manages active ruleset state, persistent custom JSON imports, feature category toggles, and restoration of bundled defaults.
 */
object EmigrantRulesManager {

    private const val TAG = "EmigrantRulesManager"
    private const val CUSTOM_RULES_FILENAME = "emigrant_rules_custom.json"
    private const val CUSTOM_RULES_TMP_FILENAME = "emigrant_rules_custom.tmp"
    private const val PREF_IS_CUSTOM_RULESET = "emigrant_is_custom_ruleset"
    private const val PREF_ACTIVE_RULESET_NAME = "emigrant_active_ruleset_name"
    private const val PREF_EMIGRANT_MODE_ENABLED = "emigrant_mode_enabled"

    // Feature Category Preference Keys
    private const val PREF_WORD_RULES_ENABLED = "emigrant_word_rules_enabled"
    private const val PREF_AUTO_SUFFIXES_ENABLED = "emigrant_auto_suffixes_enabled"
    private const val PREF_PHRASE_RULES_ENABLED = "emigrant_phrase_rules_enabled"
    private const val PREF_CONTEXT_RULES_ENABLED = "emigrant_context_rules_enabled"
    private const val PREF_GRAMMAR_RULES_ENABLED = "emigrant_grammar_rules_enabled"

    const val DEFAULT_RULESET_NAME = "Bundled Defaults"

    /**
     * Initializes the EmigrantEngine with saved ON/OFF toggle state and active ruleset.
     */
    fun init(context: Context) {
        val prefs = context.prefs()
        val modeEnabled = prefs.getBoolean(PREF_EMIGRANT_MODE_ENABLED, true)
        val wordEnabled = prefs.getBoolean(PREF_WORD_RULES_ENABLED, true)
        val suffixesEnabled = prefs.getBoolean(PREF_AUTO_SUFFIXES_ENABLED, true)
        val phraseEnabled = prefs.getBoolean(PREF_PHRASE_RULES_ENABLED, true)
        val contextEnabled = prefs.getBoolean(PREF_CONTEXT_RULES_ENABLED, true)
        val grammarEnabled = prefs.getBoolean(PREF_GRAMMAR_RULES_ENABLED, true)

        EmigrantEngine.instance.setEmigrantModeEnabled(modeEnabled)
        EmigrantEngine.instance.setRuleCategoryToggles(
            wordRules = wordEnabled,
            autoSuffixes = suffixesEnabled,
            phraseRules = phraseEnabled,
            contextRules = contextEnabled,
            grammarRules = grammarEnabled
        )

        val isCustom = prefs.getBoolean(PREF_IS_CUSTOM_RULESET, false)
        val customFile = File(context.filesDir, CUSTOM_RULES_FILENAME)

        if (isCustom && customFile.exists()) {
            try {
                val jsonString = customFile.readText()
                EmigrantEngine.instance.parseRulesJson(jsonString)
                Log.i(TAG, "Loaded custom ruleset from ${customFile.absolutePath}")
                return
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load custom ruleset file. Falling back to bundled defaults.", e)
            }
        }

        loadBundledDefaults(context)
    }

    /**
     * Checks if Emigrant mode is enabled (default true).
     */
    fun isEmigrantModeEnabled(context: Context): Boolean {
        return context.prefs().getBoolean(PREF_EMIGRANT_MODE_ENABLED, true)
    }

    /**
     * Updates Emigrant mode ON/OFF toggle state in SharedPreferences and notifies EmigrantEngine.
     */
    fun setEmigrantModeEnabled(context: Context, enabled: Boolean) {
        context.prefs().edit { putBoolean(PREF_EMIGRANT_MODE_ENABLED, enabled) }
        EmigrantEngine.instance.setEmigrantModeEnabled(enabled)
        Log.i(TAG, "Emigrant mode set to: $enabled")
    }

    // Category Toggles Getters & Setters
    fun isWordRulesEnabled(context: Context): Boolean = context.prefs().getBoolean(PREF_WORD_RULES_ENABLED, true)
    fun setWordRulesEnabled(context: Context, enabled: Boolean) {
        context.prefs().edit { putBoolean(PREF_WORD_RULES_ENABLED, enabled) }
        EmigrantEngine.instance.setWordRulesEnabled(enabled)
    }

    fun isAutoSuffixesEnabled(context: Context): Boolean = context.prefs().getBoolean(PREF_AUTO_SUFFIXES_ENABLED, true)
    fun setAutoSuffixesEnabled(context: Context, enabled: Boolean) {
        context.prefs().edit { putBoolean(PREF_AUTO_SUFFIXES_ENABLED, enabled) }
        EmigrantEngine.instance.setAutoSuffixesEnabled(enabled)
    }

    fun isPhraseRulesEnabled(context: Context): Boolean = context.prefs().getBoolean(PREF_PHRASE_RULES_ENABLED, true)
    fun setPhraseRulesEnabled(context: Context, enabled: Boolean) {
        context.prefs().edit { putBoolean(PREF_PHRASE_RULES_ENABLED, enabled) }
        EmigrantEngine.instance.setPhraseRulesEnabled(enabled)
    }

    fun isContextRulesEnabled(context: Context): Boolean = context.prefs().getBoolean(PREF_CONTEXT_RULES_ENABLED, true)
    fun setContextRulesEnabled(context: Context, enabled: Boolean) {
        context.prefs().edit { putBoolean(PREF_CONTEXT_RULES_ENABLED, enabled) }
        EmigrantEngine.instance.setContextRulesEnabled(enabled)
    }

    fun isGrammarRulesEnabled(context: Context): Boolean = context.prefs().getBoolean(PREF_GRAMMAR_RULES_ENABLED, true)
    fun setGrammarRulesEnabled(context: Context, enabled: Boolean) {
        context.prefs().edit { putBoolean(PREF_GRAMMAR_RULES_ENABLED, enabled) }
        EmigrantEngine.instance.setGrammarRulesEnabled(enabled)
    }

    /**
     * Loads the bundled asset ruleset (emigrant_rules.json) directly into EmigrantEngine.
     */
    fun loadBundledDefaults(context: Context) {
        try {
            context.assets.open(EmigrantEngine.RULES_FILE_NAME).use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                EmigrantEngine.instance.parseRulesJson(jsonString)
            }
            context.prefs().edit {
                putBoolean(PREF_IS_CUSTOM_RULESET, false)
                putString(PREF_ACTIVE_RULESET_NAME, DEFAULT_RULESET_NAME)
            }
            Log.i(TAG, "Loaded bundled default ruleset.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bundled default ruleset from assets", e)
        }
    }

    /**
     * Returns description of the currently active ruleset.
     */
    fun getActiveRulesetName(context: Context): String {
        return context.prefs().getString(PREF_ACTIVE_RULESET_NAME, DEFAULT_RULESET_NAME) ?: DEFAULT_RULESET_NAME
    }

    /**
     * Checks if a custom ruleset is currently active.
     */
    fun isCustomRulesetActive(context: Context): Boolean {
        return context.prefs().getBoolean(PREF_IS_CUSTOM_RULESET, false)
    }

    /**
     * Imports a JSON ruleset from a user-selected URI using a fully transactional workflow.
     */
    fun importRulesetFromUri(context: Context, uri: Uri): Result<String> {
        val customTmpFile = File(context.filesDir, CUSTOM_RULES_TMP_FILENAME)
        return try {
            val contentResolver = context.contentResolver
            val displayName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            } ?: "Custom Ruleset.json"

            val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: throw IllegalArgumentException("Could not read content from selected file.")

            // 1. Validate structure & check duplicate conflicts into temporary ParsedRuleset without mutating active engine state
            val parsedRuleset = EmigrantEngine.instance.validateJson(jsonString)

            // 2. Write to temporary file first
            customTmpFile.writeText(jsonString)

            // 3. Atomically move/replace target custom rules file
            val customFile = File(context.filesDir, CUSTOM_RULES_FILENAME)
            if (customFile.exists()) {
                customFile.delete()
            }
            if (!customTmpFile.renameTo(customFile)) {
                // Fallback copy if renameTo fails across file systems
                customTmpFile.copyTo(customFile, overwrite = true)
                customTmpFile.delete()
            }

            // 4. Activate in memory only after file save has succeeded
            EmigrantEngine.instance.activateParsedRuleset(parsedRuleset)

            // 5. Update preferences
            context.prefs().edit {
                putBoolean(PREF_IS_CUSTOM_RULESET, true)
                putString(PREF_ACTIVE_RULESET_NAME, displayName)
            }

            Log.i(TAG, "Successfully imported custom ruleset transactionally: $displayName")
            Result.success(displayName)
        } catch (e: Exception) {
            if (customTmpFile.exists()) {
                customTmpFile.delete()
            }
            Log.e(TAG, "Transactional import failed", e)
            Result.failure(e)
        }
    }

    /**
     * Restores the active ruleset to bundled defaults and reloads the engine.
     */
    fun restoreBundledDefaults(context: Context): String {
        val customFile = File(context.filesDir, CUSTOM_RULES_FILENAME)
        if (customFile.exists()) {
            customFile.delete()
        }
        loadBundledDefaults(context)
        Log.i(TAG, "Restored bundled default ruleset.")
        return DEFAULT_RULESET_NAME
    }
}
