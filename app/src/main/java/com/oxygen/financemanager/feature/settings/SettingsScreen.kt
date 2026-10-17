package com.oxygen.financemanager.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.oxygen.financemanager.BuildConfig
import com.oxygen.financemanager.R
import com.oxygen.financemanager.data.db.APP_LANGUAGES
import com.oxygen.financemanager.data.prefs.AppPreferences
import com.oxygen.financemanager.data.prefs.ThemeMode
import com.oxygen.financemanager.ui.ChipRow
import com.oxygen.financemanager.ui.OxListItem
import com.oxygen.financemanager.ui.OxSwitchRow
import com.oxygen.financemanager.ui.SectionHeader
import com.oxygen.financemanager.ui.theme.Oxygen
import com.oxygen.financemanager.ui.theme.OxygenType
import com.oxygen.financemanager.ui.theme.Space
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(private val prefs: AppPreferences) : ViewModel() {
    val mode: StateFlow<ThemeMode> =
        prefs.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val excludeSavings: StateFlow<Boolean> =
        prefs.excludeSavingsFromTotal.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun set(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setExcludeSavings(exclude: Boolean) {
        viewModelScope.launch { prefs.setExcludeSavingsFromTotal(exclude) }
    }
}

@Composable
fun SettingsScreen(
    onOpenFontLicense: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThemeViewModel = hiltViewModel(),
) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val excludeSavings by viewModel.excludeSavings.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.s4),
    ) {
        SectionHeader(stringResource(R.string.settings_appearance))
        Column(Modifier.padding(vertical = Space.s2), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.settings_theme), style = OxygenType.label, color = Oxygen.colors.ink)
            // зміна одразу набирає чинності — тому чіпи, а не форма з кнопкою
            ChipRow(
                options = ThemeMode.entries,
                selected = mode,
                optionLabel = { stringResource(themeLabel(it)) },
                onSelect = { picked -> picked?.let(viewModel::set) },
                required = true,
            )
        }

        LanguagePicker()

        SectionHeader(stringResource(R.string.settings_balance))
        OxSwitchRow(
            label = stringResource(R.string.settings_exclude_savings),
            supporting = stringResource(R.string.settings_exclude_savings_hint),
            checked = excludeSavings,
            onChange = viewModel::setExcludeSavings,
        )

        SectionHeader(stringResource(R.string.settings_about))
        OxListItem(
            title = stringResource(R.string.app_name_full),
            supporting = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
        )
        OxListItem(
            title = "Manrope",
            supporting = stringResource(R.string.settings_font_license_name),
            trailing = {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Oxygen.colors.inkMuted)
            },
            onClick = onOpenFontLicense,
        )
    }
}

/** OFL вимагає, щоб текст ліцензії їхав разом зі шрифтом — він лежить у res/raw і показується тут. */
@Composable
fun FontLicenseScreen(modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    val text = remember { resources.openRawResource(R.raw.manrope_ofl).bufferedReader().use { it.readText() } }
    Text(
        text = text,
        style = OxygenType.body,
        color = Oxygen.colors.inkMuted,
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.s4, vertical = Space.s2),
    )
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

/**
 * Мова застосунку. Назви мов — кожна своєю мовою: так людина знайде свою,
 * навіть якщо зараз інтерфейс незнайомою. Вибір зберігає AppCompat і сам
 * перестворює екран; поточна — та, якою застосунок говорить зараз.
 */
@Composable
private fun LanguagePicker() {
    val current = LocalConfiguration.current.locales[0].language.takeIf { it in APP_LANGUAGES } ?: APP_LANGUAGES.first()
    Column(Modifier.padding(vertical = Space.s2), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.settings_language), style = OxygenType.label, color = Oxygen.colors.ink)
        ChipRow(
            options = APP_LANGUAGES,
            selected = current,
            optionLabel = { tag ->
                Locale.forLanguageTag(tag).let { it.getDisplayLanguage(it) }.replaceFirstChar { it.titlecase(Locale.forLanguageTag(tag)) }
            },
            onSelect = { tag ->
                if (tag != null && tag != current) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            },
            required = true,
        )
    }
}
