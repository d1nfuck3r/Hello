package hello.notify

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
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
            val isSystemDark = isSystemInDarkTheme()
            val theme = if (isSystemDark) DarkTheme else LightTheme

            CompositionLocalProvider(LocalTheme provides theme) {
                Surface(color = theme.bgPrimary) {
                    NotifyScreen()
                }
            }
        }
    }
}
