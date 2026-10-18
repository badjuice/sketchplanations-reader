package org.rdcl.nox.sketchplanationsreader

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import org.rdcl.nox.sketchplanationsreader.state.AppModel
import org.rdcl.nox.sketchplanationsreader.state.Effect
import org.rdcl.nox.sketchplanationsreader.ui.App
import org.rdcl.nox.sketchplanationsreader.ui.theme.SkTheme
import org.rdcl.nox.sketchplanationsreader.ui.theme.Sk

class MainActivity : ComponentActivity() {
    private val model: AppModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                if (m.loaded) App(m, dark) { m.updateSettings { copy(dark = !dark) } }
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
                is Effect.Email -> startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + e.to))
                    .putExtra(Intent.EXTRA_SUBJECT, e.subject).putExtra(Intent.EXTRA_TEXT, e.body))
            }
        } catch (_: ActivityNotFoundException) {
            model.flash("No app on this device can open that")
        }
    }
}
