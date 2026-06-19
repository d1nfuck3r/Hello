package hello.notify

import android.graphics.Typeface
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import hello.notify.R

data class AppThemeColors(
    val bgPrimary              : Color,
    val bgSurface              : Color,
    val bgSurfaceAlt           : Color,
    val bgSurfaceHigh          : Color,
    val textPrimary            : Color,
    val textSecondary          : Color,
    val accent                 : Color,
    val accentAlt              : Color,
    val accentContainer        : Color,
    val onAccentContainer      : Color,
    val accentTertiary         : Color,
    val accentTertiaryContainer: Color,
    val border                 : Color,
    val borderVariant          : Color,
    val dockBg                 : Color,
    val dockForeground         : Color,
    val isDark                 : Boolean = false
)

// Parse "#RRGGBB" or "RRGGBB" to a fully-opaque Compose Color
fun hexToColor(hex: String, fallback: Color = Color.Black): Color = try {
    val h = hex.trimStart('#').let {
        if (it.length == 3) it.map { c -> "$c$c" }.joinToString("") else it
    }.padStart(6, '0').take(6)
    Color(android.graphics.Color.parseColor("#$h"))
} catch (_: Exception) { fallback }

// Convert a Color to its "#RRGGBB" hex string (alpha ignored — always FF when reloaded)
fun Color.toHex6(): String = "#%06X".format(((value shr 32).toInt()) and 0x00FFFFFF)

// ── M3 Expressive – Light (seed #6750A4 · Purple) ────────────────────────────
// Exact M3 tonal-palette tokens from Material Theme Builder.
// surface = N98, surfaceContainer = N93, primary = P40, onSurfaceVariant = NV30.
val SunsetTheme = AppThemeColors(
    bgPrimary               = Color(0xFFFEF7FF),   // background / surface  (N 98)
    bgSurface               = Color(0xFFF3EDF7),   // surfaceContainer      (N ~93)
    bgSurfaceAlt            = Color(0xFFECE6F0),   // surfaceContainerHigh  (N ~90)
    bgSurfaceHigh           = Color(0xFFE6E0E9),   // surfaceContainerHighest (N ~87)
    textPrimary             = Color(0xFF1C1B1F),   // onSurface / onBackground (N 10)
    textSecondary           = Color(0xFF49454F),   // onSurfaceVariant      (NV 30)
    accent                  = Color(0xFF6750A4),   // primary               (P 40)
    accentAlt               = Color(0xFF625B71),   // secondary             (S 40)
    accentContainer         = Color(0xFFEADDFF),   // primaryContainer      (P 90)
    onAccentContainer       = Color(0xFF21005D),   // onPrimaryContainer    (P 10)
    accentTertiary          = Color(0xFF7D5260),   // tertiary              (T 40)
    accentTertiaryContainer = Color(0xFFFFD8E4),   // tertiaryContainer     (T 90)
    border                  = Color(0xFF79747E),   // outline               (NV 50)
    borderVariant           = Color(0xFFCAC4D0),   // outlineVariant        (NV 80)
    dockBg                  = Color(0xF5F3EDF7),   // surfaceContainer @ 96 %
    dockForeground          = Color(0xFF6750A4),
    isDark                  = false
)

// ── M3 Expressive – Dark (seed #6750A4 · Purple) ─────────────────────────────
// surface = N 6, surfaceContainer = N ~12, primary = P 80, outline = NV 60.
val MinimalTheme = AppThemeColors(
    bgPrimary               = Color(0xFF141218),   // background / surface      (N 6)
    bgSurface               = Color(0xFF211F26),   // surfaceContainer          (N ~12)
    bgSurfaceAlt            = Color(0xFF2B2930),   // surfaceContainerHigh      (N ~17)
    bgSurfaceHigh           = Color(0xFF36343B),   // surfaceContainerHighest   (N ~22)
    textPrimary             = Color(0xFFE6E0E9),   // onSurface / onBackground  (N 90)
    textSecondary           = Color(0xFFCAC4D0),   // onSurfaceVariant          (NV 80)
    accent                  = Color(0xFFD0BCFF),   // primary                   (P 80)
    accentAlt               = Color(0xFFCCC2DC),   // secondary                 (S 80)
    accentContainer         = Color(0xFF4F378B),   // primaryContainer          (P 30)
    onAccentContainer       = Color(0xFFEADDFF),   // onPrimaryContainer        (P 90)
    accentTertiary          = Color(0xFFEFB8C8),   // tertiary                  (T 80)
    accentTertiaryContainer = Color(0xFF633B48),   // tertiaryContainer         (T 30)
    border                  = Color(0xFF938F99),   // outline                   (NV 60)
    borderVariant           = Color(0xFF49454F),   // outlineVariant            (NV 30)
    dockBg                  = Color(0xF5211F26),   // surfaceContainer @ 96 %
    dockForeground          = Color(0xFFD0BCFF),
    isDark                  = true
)

// ── M3 Expressive – AMOLED (true-black surfaces · teal seed #00796B) ─────────
// True-black OLED optimisation. M3 dark palette shifted to a teal hue for
// maximum contrast vibrancy against black; surfaces follow M3 N 4–12 tones.
val AmoledTheme = AppThemeColors(
    bgPrimary               = Color(0xFF000000),   // true black
    bgSurface               = Color(0xFF0F0D13),   // surfaceContainerLowest (N 4)
    bgSurfaceAlt            = Color(0xFF1D1B20),   // surfaceContainerLow    (N 10)
    bgSurfaceHigh           = Color(0xFF211F26),   // surfaceContainer       (N 12)
    textPrimary             = Color(0xFFE6E0E9),   // onSurface              (N 90)
    textSecondary           = Color(0xFFCAC4D0),   // onSurfaceVariant       (NV 80)
    accent                  = Color(0xFF5DD5BB),   // primary teal dark      (T 80)
    accentAlt               = Color(0xFF4FBFAB),   // secondary teal         (T2 80)
    accentContainer         = Color(0xFF004D40),   // primaryContainer       (T 30)
    onAccentContainer       = Color(0xFFB2F5EC),   // onPrimaryContainer     (T 90)
    accentTertiary          = Color(0xFFFFB74D),   // tertiary warm amber    (complementary to teal)
    accentTertiaryContainer = Color(0xFF4A2900),   // tertiaryContainer dark
    border                  = Color(0xFF3B3840),   // outline subtle on black
    borderVariant           = Color(0xFF1D1B20),   // outlineVariant
    dockBg                  = Color(0xF50F0D13),   // surfaceContainerLowest @ 96 %
    dockForeground          = Color(0xFF5DD5BB),
    isDark                  = true
)

// ── M3 Expressive – Minimal (neutral/zero-chroma seed) ───────────────────────
// Strictly achromatic M3 dark variant — surfaces and accent follow the N
// (neutral) tonal palette only; no hue saturation anywhere.
val DarkTheme = AppThemeColors(
    bgPrimary               = Color(0xFF0E0E0E),   // background neutral     (N ~5)
    bgSurface               = Color(0xFF1A1A1A),   // surfaceContainer       (N ~10)
    bgSurfaceAlt            = Color(0xFF242424),   // surfaceContainerHigh   (N ~14)
    bgSurfaceHigh           = Color(0xFF2E2E2E),   // surfaceContainerHighest (N ~18)
    textPrimary             = Color(0xFFE3E3E3),   // onSurface              (N 89)
    textSecondary           = Color(0xFFA3A3A3),   // onSurfaceVariant       (N 64)
    accent                  = Color(0xFFBEBDBD),   // primary neutral        (N 80)
    accentAlt               = Color(0xFF9E9E9E),   // secondary neutral      (N 62)
    accentContainer         = Color(0xFF3D3D3D),   // primaryContainer       (N ~24)
    onAccentContainer       = Color(0xFFE3E3E3),   // onPrimaryContainer     (N 89)
    accentTertiary          = Color(0xFF8A8A8A),   // tertiary neutral       (N 54)
    accentTertiaryContainer = Color(0xFF2A2A2A),   // tertiaryContainer      (N ~16)
    border                  = Color(0xFF5C5C5C),   // outline                (N 36)
    borderVariant           = Color(0xFF2E2E2E),   // outlineVariant         (N 18)
    dockBg                  = Color(0xF51A1A1A),   // surfaceContainer @ 96 %
    dockForeground          = Color(0xFFBEBDBD),
    isDark                  = true
)

// ── M3 Expressive – Sunset (warm orange seed #BF360C, dark) ──────────────────
// M3 tonal palette from a deep-orange seed. Rich warm surfaces follow the
// N (neutral) palette of the orange hue; accent = primary dark tone 80.
val LightTheme = AppThemeColors(
    bgPrimary               = Color(0xFF1C1107),   // background warm        (N ~6)
    bgSurface               = Color(0xFF28180A),   // surfaceContainer warm  (N ~12)
    bgSurfaceAlt            = Color(0xFF33210F),   // surfaceContainerHigh   (N ~17)
    bgSurfaceHigh           = Color(0xFF3E2C18),   // surfaceContainerHighest (N ~22)
    textPrimary             = Color(0xFFF0DFCF),   // onSurface warm         (N 90)
    textSecondary           = Color(0xFFD3C0B1),   // onSurfaceVariant warm  (NV 80)
    accent                  = Color(0xFFFFB77A),   // primary orange dark    (P 80)
    accentAlt               = Color(0xFFE5BE9B),   // secondary warm         (S 80)
    accentContainer         = Color(0xFF7B2000),   // primaryContainer       (P 30)
    onAccentContainer       = Color(0xFFFFDBC9),   // onPrimaryContainer     (P 90)
    accentTertiary          = Color(0xFF8ECAE6),   // tertiary cool sky blue (contrast with orange)
    accentTertiaryContainer = Color(0xFF0B3547),   // tertiaryContainer dark blue
    border                  = Color(0xFF9E8F83),   // outline warm           (NV 60)
    borderVariant           = Color(0xFF524540),   // outlineVariant warm    (NV 30)
    dockBg                  = Color(0xF528180A),   // surfaceContainer @ 96 %
    dockForeground          = Color(0xFFFFB77A),
    isDark                  = true
)

// ── Custom (user-defined) — live object rebuilt from CustomThemeData ──────────
val CustomTheme: AppThemeColors get() = DarkTheme.copy()

val LocalTheme = compositionLocalOf<AppThemeColors> { DarkTheme }

enum class ThemeName(val label: String, val emoji: String) {
    LIGHT("Light", "☀️"), DARK("Dark", "🌙"), MINIMAL("Minimal", "◾"),
    AMOLED("AMOLED", "🖤"), SUNSET("Sunset", "🌅"), CUSTOM("Custom", "🎨")
}

fun themeColors(name: ThemeName) = when (name) {
    ThemeName.LIGHT   -> LightTheme
    ThemeName.DARK    -> DarkTheme
    ThemeName.MINIMAL -> MinimalTheme
    ThemeName.AMOLED  -> AmoledTheme
    ThemeName.SUNSET  -> SunsetTheme
    ThemeName.CUSTOM  -> CustomTheme
}

val StarGold  = Color(0xFFF59E0B)
val GreenOk   = Color(0xFF1DB954)
val RedDanger = Color(0xFFEF4444)