package com.oxygen.financemanager

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.oxygen.financemanager.data.db.CategoryDao
import com.oxygen.financemanager.data.db.localizeSystemCategories
import com.oxygen.financemanager.data.prefs.ThemeMode
import com.oxygen.financemanager.feature.settings.ThemeViewModel
import com.oxygen.financemanager.navigation.AppNavigation
import com.oxygen.financemanager.ui.theme.OxygenTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var categories: CategoryDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // дати й суми форматуються за Locale.getDefault — тож він має збігатися з мовою застосунку
        Locale.setDefault(resources.configuration.locales[0])
        // після зміни мови екран перестворюється — і стандартні назви категорій переходять на неї
        lifecycleScope.launch { localizeSystemCategories(this@MainActivity, categories) }
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val mode by themeViewModel.mode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // системні панелі беруть bg застосунку; іконки на них — за темою застосунку,
            // а не системи, інакше при примусовій темній темі вони зливаються з фоном
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            OxygenTheme(darkTheme = dark) { AppNavigation() }
        }
    }
}
