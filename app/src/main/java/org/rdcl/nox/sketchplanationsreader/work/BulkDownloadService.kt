package org.rdcl.nox.sketchplanationsreader.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.rdcl.nox.sketchplanations.explode.Rendition
import org.rdcl.nox.sketchplanationsreader.MainActivity
import org.rdcl.nox.sketchplanationsreader.R
import org.rdcl.nox.sketchplanationsreader.ReaderApp
import org.rdcl.nox.sketchplanationsreader.data.Conn
import org.rdcl.nox.sketchplanationsreader.data.currentConn
import org.rdcl.nox.sketchplanationsreader.ui.image
import java.util.Locale

/** Runs a [Bulk] download one item at a time, with progress, speed, Pause and Cancel in the shade. */
class BulkDownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Bulk.ACTION_PAUSE -> pause()
            Bulk.ACTION_CANCEL -> cancel()
            else -> run()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Android 15 caps data-sync services at 6 hours a day; pausing keeps what's done. */
    @Suppress("unused")
    fun onTimeout(startId: Int, fgsType: Int) = pause()

    private fun run() {
        val prefs = getSharedPreferences("bulk", Context.MODE_PRIVATE)
        val kind = runCatching { Bulk.Kind.valueOf(prefs.getString("kind", null)!!) }.getOrNull() ?: return stopSelf()
        val renditions = if (prefs.getBoolean("hd", false)) listOf(Rendition.Read, Rendition.Hd) else listOf(Rendition.Read)
        val wifiOnly = prefs.getBoolean("wifiOnly", true)
        val start = Bulk.Progress(kind, Bulk.state.value?.takeIf { it.kind == kind }?.done ?: 0, Bulk.state.value?.total ?: 0)
        Bulk.state.value = start.copy(paused = false)
        ServiceCompat.startForeground(
            this, ID, notification(start), if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
        if (job?.isActive == true) return
        val app = application as ReaderApp
        val d = app.downloads
        job = scope.launch {
            // Each item is an id plus a way to store it; anything already on disk counts as done.
            val items: List<Triple<String, () -> Boolean, suspend () -> Unit>> = when (kind) {
                Bulk.Kind.Sketches -> app.repo.summaries().map { s ->
                    Triple(s.uid, { renditions.all { d.hasImage(s.image, it) } }, suspend { d.saveImages(s.image, renditions) })
                }
                Bulk.Kind.Episodes -> app.repo.podcast()?.episodes.orEmpty().filter { it.audioUrl != null }.map { e ->
                    Triple(e.guid, { d.hasEpisode(e.guid) }, suspend { d.saveEpisode(e.guid, e.audioUrl!!) {} })
                }
            }
            var done = 0
            var failed = 0
            fun publish(waiting: Boolean = false, speed: Long = Bulk.state.value?.bytesPerSec ?: 0) {
                Bulk.state.value = Bulk.Progress(kind, done, items.size, failed, speed, waiting = waiting)
            }
            publish()
            // Speed and the notification update once a second; the shade rate-limits faster updates.
            val ticker = launch {
                var lastBytes = d.bytesRead.get()
                var lastAt = SystemClock.elapsedRealtime()
                while (isActive) {
                    delay(1000)
                    val now = SystemClock.elapsedRealtime()
                    val bytes = d.bytesRead.get()
                    val p = Bulk.state.value ?: break
                    Bulk.state.value = p.copy(bytesPerSec = (bytes - lastBytes) * 1000 / (now - lastAt).coerceAtLeast(1))
                    lastBytes = bytes; lastAt = now
                    notify(notification(Bulk.state.value!!))
                }
            }
            for ((id, stored, save) in items) {
                while (!allowed(wifiOnly)) { publish(waiting = true, speed = 0); delay(3000) }
                if (!stored()) {
                    try {
                        save()
                        Bulk.savedFlow.tryEmit(kind to id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failed++
                    }
                }
                done++
                publish()
            }
            ticker.cancel()
            finish(Bulk.state.value ?: Bulk.Progress(kind, done, items.size, failed))
        }
    }

    private fun allowed(wifiOnly: Boolean) = when (currentConn(this)) {
        Conn.Wifi -> true
        Conn.Mobile -> !wifiOnly
        Conn.Off -> false
    }

    private fun pause() {
        job?.cancel()
        val p = Bulk.state.value?.copy(paused = true, waiting = false, bytesPerSec = 0) ?: return cancel()
        Bulk.state.value = p
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        notify(notification(p))
        stopSelf()
    }

    private fun cancel() {
        job?.cancel()
        Bulk.state.value = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(ID)
        stopSelf()
    }

    private fun finish(p: Bulk.Progress) {
        Bulk.state.value = null
        Bulk.finishedFlow.tryEmit(p)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        val noun = noun(p.kind)
        notify(base().setContentTitle(if (p.failed == 0) "All $noun kept offline" else "${p.failed} $noun failed")
            .setContentText("${p.done - p.failed} of ${p.total} $noun stored on this phone")
            .setAutoCancel(true).setOngoing(false).build())
        stopSelf()
    }

    private fun noun(k: Bulk.Kind) = if (k == Bulk.Kind.Sketches) "sketches" else "episodes"

    private fun notification(p: Bulk.Progress): Notification {
        val noun = noun(p.kind)
        val title = when {
            p.paused -> "Paused"
            p.waiting -> "Waiting for Wi-Fi"
            else -> "Keeping $noun offline"
        }
        val speed = if (!p.paused && !p.waiting && p.bytesPerSec > 0) " · ${speed(p.bytesPerSec)}" else ""
        val b = base().setContentTitle(title)
            .setContentText("${p.done} of ${p.total} $noun$speed")
            .setProgress(p.total.coerceAtLeast(1), p.done, p.total == 0)
            .setOngoing(!p.paused)
        if (p.paused) b.addAction(0, "Resume", PendingIntent.getForegroundService(this, 1, Bulk.intent(this, Bulk.ACTION_RUN), FLAGS))
        else b.addAction(0, "Pause", PendingIntent.getService(this, 2, Bulk.intent(this, Bulk.ACTION_PAUSE), FLAGS))
        b.addAction(0, "Cancel", PendingIntent.getService(this, 3, Bulk.intent(this, Bulk.ACTION_CANCEL), FLAGS))
        return b.build()
    }

    /** Tapping the notification opens Settings at Storage, where the same controls are. */
    private fun base(): NotificationCompat.Builder {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Offline downloads", NotificationManager.IMPORTANCE_LOW))
        }
        val open = Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_STORAGE)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, open, FLAGS))
    }

    private fun notify(n: Notification) {
        runCatching { getSystemService(NotificationManager::class.java).notify(ID, n) }
    }

    companion object {
        private const val ID = 41
        private const val CHANNEL = "bulk"
        private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        fun speed(bytesPerSec: Long): String = when {
            bytesPerSec >= 1_000_000 -> String.format(Locale.UK, "%.1f MB/s", bytesPerSec / 1e6)
            else -> "${bytesPerSec / 1000} KB/s"
        }
    }
}
