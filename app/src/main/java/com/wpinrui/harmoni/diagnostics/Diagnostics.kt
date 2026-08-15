package com.wpinrui.harmoni.diagnostics

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * What the launcher got wrong, counted.
 *
 * All three are the same shape of evidence: an input the launcher accepted and the user then undid.
 * A letter immediately erased was the wrong letter. A ring dismissed without a pick was the wrong
 * eight, or the wrong gesture. A tap refused near an edge was aimed somewhere the ring cannot go.
 * None of them can be judged from the event alone, only from what happened next, which is why they
 * are counted rather than logged and forgotten.
 */
data class DiagnosticCounts(
    /** Letters recognised and then backspaced straight away, keyed by what was recognised. */
    val misreads: Map<Char, Int> = emptyMap(),
    val ringDismissals: Int = 0,
    val edgeRejects: Int = 0,
) {
    val total: Int get() = misreads.values.sum() + ringDismissals + edgeRejects
}

/**
 * Counts kept across restarts, since the launcher process rarely dies and a session's worth of
 * evidence would otherwise vanish with it.
 *
 * Preferences rather than a file: these are a handful of integers written one at a time, at human
 * speed, which is exactly what preferences are for.
 */
object Diagnostics {

    private const val FILE = "diagnostics"
    private const val MISREAD_PREFIX = "misread_"
    private const val RING_DISMISSALS = "ring_dismissals"
    private const val EDGE_REJECTS = "edge_rejects"

    private val _counts = MutableStateFlow(DiagnosticCounts())
    val counts: StateFlow<DiagnosticCounts> = _counts.asStateFlow()

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(context: Context) {
        val preferences = preferences(context)

        _counts.value = DiagnosticCounts(
            misreads = preferences.all
                .filterKeys { it.startsWith(MISREAD_PREFIX) }
                .mapNotNull { (key, value) ->
                    val letter = key.removePrefix(MISREAD_PREFIX).firstOrNull() ?: return@mapNotNull null
                    val count = value as? Int ?: return@mapNotNull null
                    letter to count
                }
                .toMap(),
            ringDismissals = preferences.getInt(RING_DISMISSALS, 0),
            edgeRejects = preferences.getInt(EDGE_REJECTS, 0),
        )
    }

    /** A letter was recognised, inserted, and erased again before anything else happened. */
    fun recordMisread(context: Context, letter: Char) {
        val counts = _counts.value
        val next = (counts.misreads[letter] ?: 0) + 1
        preferences(context).edit { putInt("$MISREAD_PREFIX$letter", next) }
        _counts.value = counts.copy(misreads = counts.misreads + (letter to next))
    }

    fun recordRingDismissal(context: Context) {
        val next = _counts.value.ringDismissals + 1
        preferences(context).edit { putInt(RING_DISMISSALS, next) }
        _counts.value = _counts.value.copy(ringDismissals = next)
    }

    fun recordEdgeReject(context: Context) {
        val next = _counts.value.edgeRejects + 1
        preferences(context).edit { putInt(EDGE_REJECTS, next) }
        _counts.value = _counts.value.copy(edgeRejects = next)
    }

    /** Offered on the diagnostics view, since counts are only worth reading against a period. */
    fun clear(context: Context) {
        preferences(context).edit { clear() }
        _counts.value = DiagnosticCounts()
    }
}
