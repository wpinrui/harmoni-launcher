package com.wpinrui.harmoni.system

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

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
 * The session is whichever app is playing, not YouTube Music specifically. Tapping the element
 * still opens YouTube Music, per the design document.
 */
@Composable
fun rememberNowPlaying(): State<NowPlaying?> {
    val context = LocalContext.current
    val playing = remember { mutableStateOf<NowPlaying?>(null) }

    DisposableEffect(context) {
        val listenerComponent = ComponentName(context, HarmoniNotificationListener::class.java)
        val sessions = context.getSystemService(MediaSessionManager::class.java)

        // Each active controller gets its own callback, and the set of controllers is itself
        // watched, so a newly started app is picked up without a poll.
        var controllers = emptyList<MediaController>()
        val callbacks = mutableMapOf<MediaController, MediaController.Callback>()

        fun publish() {
            playing.value = controllers.firstNotNullOfOrNull { it.nowPlayingOrNull() }
        }

        fun rebind(active: List<MediaController>) {
            callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
            callbacks.clear()
            controllers = active
            active.forEach { controller ->
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

        val granted = runCatching {
            sessions.addOnActiveSessionsChangedListener(onSessionsChanged, listenerComponent)
            rebind(sessions.getActiveSessions(listenerComponent))
        }.isSuccess

        onDispose {
            if (granted) sessions.removeOnActiveSessionsChangedListener(onSessionsChanged)
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
