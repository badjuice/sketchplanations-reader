package org.rdcl.nox.sketchplanationsreader.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.rdcl.nox.sketchplanationsreader.MainActivity

/**
 * Background podcast playback. Media3 gives the session its notification, lock-screen controls and
 * Bluetooth/headset buttons; this class only builds the player and keeps it alive.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = buildPlayer(this, pauseForCalls = true)
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(open)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    val base = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                        .add(SessionCommand(PAUSE_FOR_CALLS, Bundle.EMPTY)).build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(base).build()
                }

                override fun onCustomCommand(
                    session: MediaSession, controller: MediaSession.ControllerInfo, command: SessionCommand, args: Bundle,
                ): ListenableFuture<SessionResult> {
                    if (command.customAction == PAUSE_FOR_CALLS) {
                        applyFocus(session.player as ExoPlayer, args.getBoolean(PAUSE_FOR_CALLS, true))
                        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    return super.onCustomCommand(session, controller, command, args)
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    /** Swiping the app away stops the service unless something is actually playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }

    companion object {
        const val PAUSE_FOR_CALLS = "pauseForCalls"

        /**
         * Speech-tuned player. Handling audio focus is what makes it pause for a call (and for other
         * apps' audio) and resume after; unplugging headphones always pauses.
         */
        @OptIn(UnstableApi::class)
        fun buildPlayer(context: Context, pauseForCalls: Boolean): ExoPlayer =
            ExoPlayer.Builder(context)
                .setHandleAudioBecomingNoisy(true)
                .setSeekBackIncrementMs(15_000)
                .setSeekForwardIncrementMs(30_000)
                .build()
                .also { applyFocus(it, pauseForCalls) }

        fun applyFocus(player: ExoPlayer, pauseForCalls: Boolean) {
            val attrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build()
            player.setAudioAttributes(attrs, pauseForCalls)
        }
    }
}
