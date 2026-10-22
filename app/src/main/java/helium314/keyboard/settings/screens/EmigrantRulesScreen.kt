// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import helium314.keyboard.latin.emigrant.EmigrantRulesManager
import helium314.keyboard.latin.utils.NextScreenIcon
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.dialogs.ConfirmationDialog
import helium314.keyboard.settings.dialogs.InfoDialog
import helium314.keyboard.settings.preferences.Preference

private val jsonImportIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
    .addCategory(Intent.CATEGORY_OPENABLE)
    .setType("*/*")
    .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/*"))

@Composable
fun EmigrantRulesScreen(
    onClickBack: () -> Unit
) {
    val context = LocalContext.current
    var modeEnabled by remember { mutableStateOf(EmigrantRulesManager.isEmigrantModeEnabled(context)) }
    var wordRulesEnabled by remember { mutableStateOf(EmigrantRulesManager.isWordRulesEnabled(context)) }
    var autoSuffixesEnabled by remember { mutableStateOf(EmigrantRulesManager.isAutoSuffixesEnabled(context)) }
    var phraseRulesEnabled by remember { mutableStateOf(EmigrantRulesManager.isPhraseRulesEnabled(context)) }
    var contextRulesEnabled by remember { mutableStateOf(EmigrantRulesManager.isContextRulesEnabled(context)) }
    var grammarRulesEnabled by remember { mutableStateOf(EmigrantRulesManager.isGrammarRulesEnabled(context)) }

    var activeRulesetName by remember { mutableStateOf(EmigrantRulesManager.getActiveRulesetName(context)) }
    var isCustomActive by remember { mutableStateOf(EmigrantRulesManager.isCustomRulesetActive(context)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult

        val importResult = EmigrantRulesManager.importRulesetFromUri(context, uri)
        importResult.onSuccess { name ->
            activeRulesetName = name
            isCustomActive = true
            Toast.makeText(context, "Successfully imported ruleset: $name", Toast.LENGTH_SHORT).show()
        }.onFailure { error ->
            errorMessage = error.message ?: "Failed to validate JSON ruleset."
        }
    }

    if (errorMessage != null) {
        InfoDialog(errorMessage!!) {
            errorMessage = null
        }
    }

    if (showRestoreDialog) {
        ConfirmationDialog(
            onDismissRequest = { showRestoreDialog = false },
            onConfirmed = {
                showRestoreDialog = false
                val restoredName = EmigrantRulesManager.restoreBundledDefaults(context)
                activeRulesetName = restoredName
                isCustomActive = false
                Toast.makeText(context, "Restored bundled defaults.", Toast.LENGTH_SHORT).show()
            },
            content = { Text("Restore bundled default ruleset? Any custom imported JSON ruleset will be replaced.") }
        )
    }

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = "Ill Eagle Rules",
        settings = emptyList()
    ) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) { innerPadding ->
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .then(Modifier.padding(innerPadding))
            ) {
                Preference(
                    name = "Ill Eagle Mode",
                    description = if (modeEnabled) "ON — Transformations active" else "OFF — Standard keyboard behavior",
                    onClick = {
                        val newMode = !modeEnabled
                        modeEnabled = newMode
                        EmigrantRulesManager.setEmigrantModeEnabled(context, newMode)
                    }
                ) {
                    Switch(
                        checked = modeEnabled,
                        onCheckedChange = { newMode ->
                            modeEnabled = newMode
                            EmigrantRulesManager.setEmigrantModeEnabled(context, newMode)
                        }
                    )
                }

                if (modeEnabled) {
                    Preference(
                        name = "Word Replacements",
                        description = "Single word rules (word_rules)",
                        onClick = {
                            val newVal = !wordRulesEnabled
                            wordRulesEnabled = newVal
                            EmigrantRulesManager.setWordRulesEnabled(context, newVal)
                        }
                    ) {
                        Switch(
                            checked = wordRulesEnabled,
                            onCheckedChange = { newVal ->
                                wordRulesEnabled = newVal
                                EmigrantRulesManager.setWordRulesEnabled(context, newVal)
                            }
                        )
                    }

                    Preference(
                        name = "Automatic Suffixes",
                        description = "Apply suffixes (-s, -ing, -ed, -ly) automatically to word rules",
                        onClick = {
                            val newVal = !autoSuffixesEnabled
                            autoSuffixesEnabled = newVal
                            EmigrantRulesManager.setAutoSuffixesEnabled(context, newVal)
                        }
                    ) {
                        Switch(
                            checked = autoSuffixesEnabled,
                            onCheckedChange = { newVal ->
                                autoSuffixesEnabled = newVal
                                EmigrantRulesManager.setAutoSuffixesEnabled(context, newVal)
                            }
                        )
                    }

                    Preference(
                        name = "Phrase Rules",
                        description = "Multiword phrase rules (phrase_rules)",
                        onClick = {
                            val newVal = !phraseRulesEnabled
                            phraseRulesEnabled = newVal
                            EmigrantRulesManager.setPhraseRulesEnabled(context, newVal)
                        }
                    ) {
                        Switch(
                            checked = phraseRulesEnabled,
                            onCheckedChange = { newVal ->
                                phraseRulesEnabled = newVal
                                EmigrantRulesManager.setPhraseRulesEnabled(context, newVal)
                            }
                        )
                    }

                    Preference(
                        name = "Context Rules",
                        description = "Sentence-dependent context rules (context_rules)",
                        onClick = {
                            val newVal = !contextRulesEnabled
                            contextRulesEnabled = newVal
                            EmigrantRulesManager.setContextRulesEnabled(context, newVal)
                        }
                    ) {
                        Switch(
                            checked = contextRulesEnabled,
                            onCheckedChange = { newVal ->
                                contextRulesEnabled = newVal
                                EmigrantRulesManager.setContextRulesEnabled(context, newVal)
                            }
                        )
                    }

                    Preference(
                        name = "Grammar Rules",
                        description = "Grammar & pattern rules (grammar_rules)",
                        onClick = {
                            val newVal = !grammarRulesEnabled
                            grammarRulesEnabled = newVal
                            EmigrantRulesManager.setGrammarRulesEnabled(context, newVal)
                        }
                    ) {
                        Switch(
                            checked = grammarRulesEnabled,
                            onCheckedChange = { newVal ->
                                grammarRulesEnabled = newVal
                                EmigrantRulesManager.setGrammarRulesEnabled(context, newVal)
                            }
                        )
                    }
                }

                Preference(
                    name = "Active Ruleset",
                    description = activeRulesetName,
                    onClick = {}
                )

                Preference(
                    name = "Import JSON Ruleset",
                    description = "Replace active rules with a custom JSON file",
                    onClick = {
                        importLauncher.launch(jsonImportIntent)
                    }
                ) { NextScreenIcon() }

                if (isCustomActive) {
                    Preference(
                        name = "Restore Bundled Defaults",
                        description = "Reset active rules back to original bundled defaults",
                        onClick = {
                            showRestoreDialog = true
                        }
                    )
                }
            }
        }
    }
}
