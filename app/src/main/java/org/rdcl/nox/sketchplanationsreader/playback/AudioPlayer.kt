package org.rdcl.nox.sketchplanationsreader.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import kotlinx.coroutines.suspendCancellableCoroutine
import org.rdcl.nox.sketchplanations.explode.Episode
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The app's handle on podcast audio. With [withService] the audio lives in [PlaybackService] and
 * keeps going with the screen locked, with lock-screen and shade controls. Without it, a plain
 * in-app player that never posts a notification. Android requires one for background playback,
 * so in that mode audio stops when the app leaves the screen.
 */
class AudioPlayer(private val context: Context) {
    private var controller: MediaController? = null
    private var local: ExoPlayer? = null
    private var listener: Player.Listener? = null
    private var pauseForCalls = true

    val player: Player? get() = controller ?: local

    fun setListener(l: Player.Listener) {
        listener?.let { player?.removeListener(it) }
        listener = l
        player?.addListener(l)
    }

    /** The player for the chosen mode, switching (and stopping the other) if the mode changed. */
    suspend fun connect(withService: Boolean): Player {
        if (withService) {
            controller?.let { return it }
            local?.let { old -> listener?.let(old::removeListener); old.release() }
            local = null
            val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            val c = suspendCancellableCoroutine { cont ->
                future.addListener({
                    runCatching { future.get() }.onSuccess { cont.resume(it) }.onFailure { cont.resumeWithException(it) }
                }, ContextCompat.getMainExecutor(context))
                cont.invokeOnCancellation { future.cancel(true) }
            }
            controller = c
            listener?.let(c::addListener)
            sendFocus(c)
            return c
        }
        local?.let { return it }
        controller?.let { old -> listener?.let(old::removeListener); old.stop(); old.release() }
        controller = null
        val p = PlaybackService.buildPlayer(context, pauseForCalls)
        local = p
        listener?.let(p::addListener)
        return p
    }

    fun setPauseForCalls(on: Boolean) {
        pauseForCalls = on
        local?.let { PlaybackService.applyFocus(it, on) }
        controller?.let(::sendFocus)
    }

    private fun sendFocus(c: MediaController) {
        c.sendCustomCommand(
            SessionCommand(PlaybackService.PAUSE_FOR_CALLS, Bundle.EMPTY),
            Bundle().apply { putBoolean(PlaybackService.PAUSE_FOR_CALLS, pauseForCalls) },
        )
    }

    /** Drops the app's connection. Service-mode audio keeps playing; in-app audio stops. */
    fun release() {
        controller?.release(); controller = null
        local?.release(); local = null
    }

    companion object {
        /**
         * [uri] is the downloaded file when there is one, otherwise the feed's audio URL: a
         * tracking redirect that is resolved now, at play time, and never stored.
         */
        fun item(e: Episode, uri: Uri, chapter: String?): MediaItem = MediaItem.Builder()
            .setMediaId(e.guid)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(e.title)
                    .setArtist("Sketchplanations - The Podcast")
                    .setSubtitle(chapter)
                    .setDisplayTitle(e.title)
                    .setArtworkUri(e.image?.let(Uri::parse))
                    .build(),
            )
            .build()
    }
}
