package com.wpinrui.harmoni.home

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Home presses, which the launcher only hears about as a fresh intent to an activity that is
 * already on screen.
 *
 * Swiping home is how you leave anything, so it has to close whatever the launcher has put over
 * the wallpaper, even though nothing about the activity's lifecycle changes.
 */
object HomePresses {

    private val _presses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val presses: SharedFlow<Unit> = _presses

    fun record() {
        _presses.tryEmit(Unit)
    }
}
