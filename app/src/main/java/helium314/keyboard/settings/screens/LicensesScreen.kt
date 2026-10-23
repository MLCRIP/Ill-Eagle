// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.settings.SearchSettingsScreen

data class LegalDocument(
    val title: String,
    val fileName: String,
    val description: String
)

val legalDocuments = listOf(
    LegalDocument(
        title = "Notices & Credits",
        fileName = "THIRD_PARTY_NOTICES.md",
        description = "Third-party attributions, dictionary sources, and component licenses"
    ),
    LegalDocument(
        title = "Ill Eagle Changes",
        fileName = "MODIFICATIONS.md",
        description = "Summary of modifications made to HeliBoard"
    ),
    LegalDocument(
        title = "GNU GPL v3.0",
        fileName = "LICENSE",
        description = "Full GNU General Public License v3.0"
    ),
    LegalDocument(
        title = "Apache 2.0",
        fileName = "LICENSE-Apache-2.0",
        description = "Full Apache License Version 2.0"
    ),
    LegalDocument(
        title = "CC BY-SA 4.0",
        fileName = "LICENSE-CC-BY-SA-4.0",
        description = "Full Creative Commons Attribution-ShareAlike 4.0 License"
    )
)

@Composable
fun LicensesScreen(
    onClickBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentDoc = legalDocuments[selectedIndex.coerceIn(0, legalDocuments.lastIndex)]

    val documentText = remember(currentDoc.fileName) {
        try {
            context.assets.open("licenses/${currentDoc.fileName}").use { inputStream ->
                inputStream.reader().readText()
            }
        } catch (e: Exception) {
            Log.e("LicensesScreen", "Error reading asset ${currentDoc.fileName}", e)
            "Error loading document: ${e.message}"
        }
    }

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = "Licences & Credits",
        settings = emptyList()
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    legalDocuments.forEachIndexed { index, doc ->
                        FilterChip(
                            selected = index == selectedIndex,
                            onClick = { selectedIndex = index },
                            label = { Text(doc.title) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Text(
                    text = currentDoc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                text = documentText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
