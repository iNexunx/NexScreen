package nx.screen.ds.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import nx.screen.ds.R
import nx.screen.ds.ui.component.SplicedColumnGroup
import nx.screen.ds.ui.theme.AppFilterChip

@Composable
fun LanguagesScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsState()
    KeyboardScrollColumn(
        modifier = Modifier
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Color.Transparent),
    ) {
        ScreenHeader(title = stringResource(R.string.language), onBack = onBack)
        Text(
            stringResource(R.string.language_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SplicedColumnGroup {
            item(key = "languages") {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    languages().forEach { (code, label) ->
                        AppFilterChip(
                            selected = settings.language == code,
                            onClick = { viewModel.setLanguage(code) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            label = {
                                Text(
                                    label,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun languages(): List<Pair<String?, String>> = listOf(
    null to stringResource(R.string.lang_system),
    "es" to stringResource(R.string.lang_spanish),
    "en" to stringResource(R.string.lang_english),
    "pt" to stringResource(R.string.lang_portuguese),
    "fr" to stringResource(R.string.lang_french),
    "de" to stringResource(R.string.lang_german),
    "it" to stringResource(R.string.lang_italian),
    "ru" to stringResource(R.string.lang_russian),
    "ar" to stringResource(R.string.lang_arabic),
    "hi" to stringResource(R.string.lang_hindi),
    "zh" to stringResource(R.string.lang_chinese),
    "ja" to stringResource(R.string.lang_japanese),
    "ko" to stringResource(R.string.lang_korean),
    "tr" to stringResource(R.string.lang_turkish),
    "pl" to stringResource(R.string.lang_polish),
    "nl" to stringResource(R.string.lang_dutch),
    "id" to stringResource(R.string.lang_indonesian),
)
