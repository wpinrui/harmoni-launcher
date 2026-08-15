package com.wpinrui.harmoni.system

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.wpinrui.harmoni.home.HomeBindings

/** What the music element shows when something is playing. */
data class NowPlaying(val title: String, val artist: String?) {
    /** "Says, Nils Frahm" in the mockup, or just the title when the artist is unknown. */
    val line: String get() = if (artist.isNullOrBlank()) title else "$title, $artist"
}

/**
 * The track playing on whichever media session is active, or null when nothing is.
 *
 * Reading sessions is gated on notification access, which is the same grant the badges need, so
 * the music element sits in its idle state until that is given.
 *
 * Only YouTube Music counts. Anything else playing leaves the element idle, which keeps what the
 * element shows and what tapping it opens the same app.
 */
@Composable
fun rememberNowPlaying(): State<NowPlaying?> {
    val context = LocalContext.current
    val playing = remember { mutableStateOf<NowPlaying?>(null) }
    val connected by NotificationAccess.connected.collectAsState()

    // Keyed on the bind, not just on the context: the listener is bound a moment after the
    // process starts, so a one-shot attempt at first composition always lost the race.
    DisposableEffect(context, connected) {
        if (!connected) {
            playing.value = null
            return@DisposableEffect onDispose { }
        }

        val listenerComponent = ComponentName(context, HarmoniNotificationListener::class.java)
        val sessions = context.getSystemService(MediaSessionManager::class.java)

        // Each active controller gets its own callback, and the set of controllers is itself
        // watched, so a newly started app is picked up without a poll.
        var controllers = emptyList<MediaController>()
        val callbacks = mutableMapOf<MediaController, MediaController.Callback>()

        fun publish() {
            val current = controllers.firstNotNullOfOrNull { it.nowPlayingOrNull() }
            Log.d(TAG, "Now playing: ${current?.line ?: "nothing"}")
            playing.value = current
        }

        fun rebind(active: List<MediaController>) {
            callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
            callbacks.clear()
            controllers = active.filter { it.packageName == HomeBindings.YOUTUBE_MUSIC }
            controllers.forEach { controller ->
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
                    override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
                }
                controller.registerCallback(callback)
                callbacks[controller] = callback
            }
            publish()
        }

        val onSessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { active ->
            rebind(active.orEmpty())
        }

        var listening = false
        try {
            sessions.addOnActiveSessionsChangedListener(onSessionsChanged, listenerComponent)
            listening = true
            rebind(sessions.getActiveSessions(listenerComponent))
        } catch (e: SecurityException) {
            // Worth seeing rather than swallowing: it means the bind is not usable yet, and the
            // music element will sit idle until the next connection event brings us back here.
            Log.w(TAG, "Cannot read media sessions despite a bound listener", e)
        }

        onDispose {
            if (listening) sessions.removeOnActiveSessionsChangedListener(onSessionsChanged)
            callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        }
    }

    return playing
}

private fun MediaController.nowPlayingOrNull(): NowPlaying? {
    if (playbackState?.state != PlaybackState.STATE_PLAYING) return null
    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }
        ?: return null
    return NowPlaying(title, metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST))
}

private const val TAG = "NowPlaying"
