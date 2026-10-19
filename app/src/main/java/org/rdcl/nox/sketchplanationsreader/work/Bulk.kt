package org.rdcl.nox.sketchplanationsreader.work

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * "Keep every sketch / episode offline", run by [BulkDownloadService] so it carries on with the app
 * closed. The screens watch [progress]; the service reports each stored item through [saved].
 */
object Bulk {
    enum class Kind { Sketches, Episodes }

    data class Progress(
        val kind: Kind,
        val done: Int,
        val total: Int,
        val failed: Int = 0,
        val bytesPerSec: Long = 0,
        val paused: Boolean = false,
        /** Wi-Fi only is on and there is no Wi-Fi: the run waits instead of failing. */
        val waiting: Boolean = false,
    ) {
        val fraction get() = if (total == 0) 0f else done.toFloat() / total
    }

    internal val state = MutableStateFlow<Progress?>(null)
    val progress: StateFlow<Progress?> = state

    internal val savedFlow = MutableSharedFlow<Pair<Kind, String>>(extraBufferCapacity = 256)
    val saved: SharedFlow<Pair<Kind, String>> = savedFlow

    internal val finishedFlow = MutableSharedFlow<Progress>(extraBufferCapacity = 4)
    val finished: SharedFlow<Progress> = finishedFlow

    internal const val ACTION_RUN = "run"
    internal const val ACTION_PAUSE = "pause"
    internal const val ACTION_CANCEL = "cancel"

    /** Stored in preferences, so Resume works even after Android has stopped the app. */
    fun start(ctx: Context, kind: Kind, hd: Boolean, wifiOnly: Boolean) {
        ctx.getSharedPreferences("bulk", Context.MODE_PRIVATE).edit()
            .putString("kind", kind.name).putBoolean("hd", hd).putBoolean("wifiOnly", wifiOnly).apply()
        resume(ctx)
    }

    fun resume(ctx: Context) = ContextCompat.startForegroundService(ctx, intent(ctx, ACTION_RUN))
    fun pause(ctx: Context) { ctx.startService(intent(ctx, ACTION_PAUSE)) }
    fun cancel(ctx: Context) { ctx.startService(intent(ctx, ACTION_CANCEL)) }

    internal fun intent(ctx: Context, action: String) = Intent(ctx, BulkDownloadService::class.java).setAction(action)
}
