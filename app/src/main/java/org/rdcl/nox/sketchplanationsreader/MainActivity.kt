package org.rdcl.nox.sketchplanationsreader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.state.Effect
import org.rdcl.nox.sketchplanationsreader.ui.App
import org.rdcl.nox.sketchplanationsreader.ui.theme.SkTheme
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk

class MainActivity : ComponentActivity() {
    private val model: AppModel by viewModels()
    /** Where a notification asked to open, applied once the model has loaded. */
    private var pendingOpen by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingOpen = intent.getStringExtra(EXTRA_OPEN)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) pendingOpen = intent.getStringExtra(EXTRA_OPEN)
        enableEdgeToEdge()
        setContent {
            val m = model
            val dark = m.settings.dark ?: isSystemInDarkTheme()
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(style, style)
            }
            SkTheme(dark = dark, pack = m.settings.font, reduceMotion = m.settings.reduceMotion) {
                val chrome = Sk.colors.surfaceChrome
                LaunchedEffect(Unit) {
                    m.effects.collect { e -> handle(e, chrome.toArgb()) }
                }
                LaunchedEffect(m.loaded, pendingOpen) {
                    if (m.loaded && pendingOpen == OPEN_STORAGE) m.openStorage()
                    if (m.loaded) pendingOpen = null
                }
                if (m.loaded) App(m)
                else Box(Modifier.fillMaxSize().background(Sk.colors.surfaceApp))
            }
        }
    }

    private fun handle(e: Effect, toolbar: Int) {
        try {
            when (e) {
                is Effect.OpenUrl -> {
                    val uri = Uri.parse(e.url)
                    if (e.inApp) {
                        CustomTabsIntent.Builder()
                            .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(toolbar).build())
                            .setShowTitle(true)
                            .build()
                            .launchUrl(this, uri)
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                }
                is Effect.Share -> startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, e.text), null,
                ))
                Effect.RequestStorage -> requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                Effect.RequestNotifications -> if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2)
                is Effect.Email -> startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + e.to))
                    .putExtra(Intent.EXTRA_SUBJECT, e.subject).putExtra(Intent.EXTRA_TEXT, e.body))
            }
        } catch (_: ActivityNotFoundException) {
            model.flash("No app on this device can open that")
        }
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_STORAGE = "storage"
    }
}
