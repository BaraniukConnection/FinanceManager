package com.oxygen.financemanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.oxygen.financemanager.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Oxygen Theme — токени з OxygenTheme/tokens.json, змаплені рівно один раз.
 * Поза цим файлом hex-кольорів, розмірів шрифтів і радіусів у застосунку бути не повинно.
 */

// ---------- Колір ----------

@Immutable
data class OxygenColors(
    val bg: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val line: Color,
    val ink: Color,
    val inkMuted: Color,
    val accent: Color,
    val onAccent: Color,
    /** Акцент як передній план: посилання, активні іконки, фокус. Не `accent` — м'ята на білому 1.5:1. */
    val accentInk: Color,
    val accentSoft: Color,
    val attention: Color,
    val attentionInk: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerInk: Color,
    val scrim: Color,
    val isDark: Boolean,
)

private val Dark = OxygenColors(
    bg = Color(0xFF10221C),
    surface = Color(0xFF162E26),
    surfaceVariant = Color(0xFF233932),
    line = Color(0xFF23483C),
    ink = Color(0xFFE6F2EC),
    inkMuted = Color(0xFF92C9B7),
    accent = Color(0xFF13ECA4),
    onAccent = Color(0xFF0B1F18),
    accentInk = Color(0xFF13ECA4),
    accentSoft = Color(0xFF15543F),
    attention = Color(0xFFF59E0B),
    attentionInk = Color(0xFFF59E0B),
    danger = Color(0xFFDC2626),
    onDanger = Color(0xFFFFFFFF),
    dangerInk = Color(0xFFFCA5A5),
    scrim = Color(0x8C000000),
    isDark = true,
)

private val Light = OxygenColors(
    bg = Color(0xFFF3FAF7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDFF2EB),
    line = Color(0xFFD3E8DF),
    ink = Color(0xFF1A2F24),
    inkMuted = Color(0xFF4A6B5A),
    accent = Color(0xFF13ECA4),
    onAccent = Color(0xFF0B1F18),
    accentInk = Color(0xFF047857),
    accentSoft = Color(0xFFD0FBED),
    attention = Color(0xFFF59E0B),
    attentionInk = Color(0xFFB45309),
    danger = Color(0xFFDC2626),
    onDanger = Color(0xFFFFFFFF),
    dangerInk = Color(0xFFDC2626),
    scrim = Color(0x66000000),
    isDark = false,
)

/**
 * Вісім кольорів для вмісту користувача: рахунки, категорії, мітки. Ніколи для станів UI.
 * `Long`, а не `Color`, щоб шар даних і JVM-тести не залежали від Compose.
 */
object Mark {
    const val MINT = 0xFF13ECA4
    const val BLUE = 0xFF3B82F6
    const val AMBER = 0xFFF59E0B
    const val RED = 0xFFEF4444
    const val PURPLE = 0xFFA855F7
    const val EMERALD = 0xFF10B981
    const val PINK = 0xFFEC4899
    const val SLATE = 0xFF64748B

    val all: List<Int> = listOf(MINT, BLUE, AMBER, RED, PURPLE, EMERALD, PINK, SLATE).map { it.toInt() }
}

/** Слоти M3 за platforms.md. Решта заповнена токенами явно, щоб базова фіолетова палітра M3 ніде не просочилась. */
private fun OxygenColors.toScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = ink,
        inversePrimary = accentInk,
        secondary = accentInk,
        onSecondary = bg,
        secondaryContainer = accentSoft,
        onSecondaryContainer = ink,
        tertiary = attentionInk,
        onTertiary = bg,
        tertiaryContainer = surfaceVariant,
        onTertiaryContainer = ink,
        background = bg,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = inkMuted,
        // глибина — тінями, не тонуванням поверхні
        surfaceTint = Color.Transparent,
        inverseSurface = ink,
        inverseOnSurface = bg,
        error = danger,
        onError = onDanger,
        errorContainer = danger,
        onErrorContainer = onDanger,
        outline = inkMuted,
        outlineVariant = line,
        scrim = scrim,
        surfaceBright = surface,
        surfaceDim = bg,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceVariant,
    )
}

// ---------- Шрифт ----------

/**
 * Manrope — один змінний файл на всі накреслення: вага задається віссю `wght`, а не окремими TTF.
 * Ліцензія SIL OFL 1.1 лежить поруч у res/raw і показується в «Про застосунок».
 */
@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    resId = R.font.manrope,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val OxygenFont = FontFamily(listOf(400, 500, 600, 700, 800).map(::manrope))

private fun style(size: Int, line: Int, weight: Int, tracking: Float = 0f) = TextStyle(
    fontFamily = OxygenFont,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.sp,
)

/** Іменовані стилі Oxygen. Використовувати лише їх — жодних «довільних» розмірів. */
object OxygenType {
    val display = style(40, 44, 800, -0.5f)
    val screenTitle = style(22, 28, 700)
    val title = style(16, 22, 700)
    val bodyLg = style(16, 26, 400)
    val body = style(14, 20, 400)
    val label = style(13, 18, 600)
    val button = style(15, 20, 700)
    val overline = style(13, 18, 700, 1.6f)
    val caption = style(12, 16, 500)
    val micro = style(11, 14, 600)
}

private val OxygenTypography = Typography(
    titleLarge = OxygenType.screenTitle,
    titleMedium = OxygenType.title,
    bodyLarge = OxygenType.bodyLg,
    bodyMedium = OxygenType.body,
    labelLarge = OxygenType.button,
    labelMedium = OxygenType.label,
    labelSmall = OxygenType.micro,
)

// ---------- Форма, простір, глибина ----------

private val OxygenShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

object Space {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s8 = 32.dp

    /** Щоб вміст прокручувався під плаваючою кнопкою створення і не ховався за нею. */
    val bottomInset = 140.dp
}

object Size {
    val target = 44.dp
    val row = 56.dp
    val fab = 70.dp
}

object Elevation {
    /** elev-1: картки в спокої. */
    val card = 1.dp
    /** elev-2: FAB і те, що тягнуть. */
    val fab = 6.dp
    /** elev-3: усе, що плаває. */
    val floating = 10.dp
}

// ---------- Тема ----------

private val LocalOxygenColors = staticCompositionLocalOf { Dark }

/** Токени, для яких у M3 немає слота: `Oxygen.colors.accentInk`, `.accentSoft`, `.attention`… */
object Oxygen {
    val colors: OxygenColors
        @Composable get() = LocalOxygenColors.current
}

/** Динамічні кольори (Material You) вимкнені свідомо: Oxygen має виглядати як Oxygen. */
@Composable
fun OxygenTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) Dark else Light
    CompositionLocalProvider(LocalOxygenColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toScheme(),
            typography = OxygenTypography,
            shapes = OxygenShapes,
            content = content,
        )
    }
}
