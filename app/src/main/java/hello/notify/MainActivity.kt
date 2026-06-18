package hello.notify

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs    = newBase.getSharedPreferences("hello_prefs", Context.MODE_PRIVATE)
        val langCode = prefs.getString("user_language_code", "th") ?: "th"
        val locale   = Locale.forLanguageTag(langCode)
        Locale.setDefault(locale)
        val config   = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark   = isSystemInDarkTheme()
            val context  = LocalContext.current

            // Monet Dynamic Color — Android 12+ only
            val theme: AppThemeColors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val scheme = if (isDark) dynamicDarkColorScheme(context)
                             else dynamicLightColorScheme(context)
                val base   = if (isDark) DarkTheme else LightTheme
                base.copy(
                    accent                  = scheme.primary,
                    accentAlt               = scheme.secondary,
                    accentContainer         = scheme.primaryContainer,
                    onAccentContainer       = scheme.onPrimaryContainer,
                    accentTertiary          = scheme.tertiary,
                    accentTertiaryContainer = scheme.tertiaryContainer,
                    bgPrimary               = scheme.background,
                    bgSurface               = scheme.surface,
                    bgSurfaceAlt            = scheme.surfaceVariant,
                    textPrimary             = scheme.onSurface,
                    textSecondary           = scheme.onSurfaceVariant,
                    border                  = scheme.outline,
                    borderVariant           = scheme.outlineVariant,
                    dockBg                  = scheme.surfaceContainer,
                    dockForeground          = scheme.primary
                )
            } else {
                if (isDark) DarkTheme else LightTheme
            }

            CompositionLocalProvider(LocalTheme provides theme) {
                Surface(color = theme.bgPrimary) {
                    NotifyScreen()
                }
            }
        }
    }
}
