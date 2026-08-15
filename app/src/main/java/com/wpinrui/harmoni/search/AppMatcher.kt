package com.wpinrui.harmoni.search

import com.wpinrui.harmoni.apps.AppEntry
import kotlin.math.min

/**
 * Filters and ranks installed apps against a query, per Section 4.
 *
 * A match is a case-insensitive subsequence: the query's characters appear in order but need not
 * be adjacent, so "cl" finds Clock and Calculator alike. Ranking then prefers matches that landed
 * on word boundaries, then runs of adjacent characters, then shorter names, which is what makes
 * "cl" put Clock above Calculator rather than the other way round.
 *
 * Edit distance is a second tier, used only when nothing matches as a subsequence, so a typo
 * still finds something rather than emptying the grid.
 *
 * The same query always produces the same order: every comparison falls through to the app's
 * package name, which is unique.
 */
object AppMatcher {

    fun match(entries: List<AppEntry>, query: String): List<AppEntry> {
        if (query.isBlank()) return entries

        val needle = query.lowercase().filterNot { it.isWhitespace() }
        if (needle.isEmpty()) return entries

        val subsequence = entries.mapNotNull { entry ->
            subsequenceScore(entry.label.lowercase(), needle)?.let { entry to it }
        }

        if (subsequence.isNotEmpty()) {
            return subsequence
                .sortedWith(
                    compareByDescending<Pair<AppEntry, Int>> { it.second }
                        .thenBy { it.first.label.length }
                        .thenBy { it.first.component.flattenToShortString() },
                )
                .map { it.first }
        }

        // Nothing matched in order, so the query is probably mistyped rather than sparse.
        return entries.mapNotNull { entry ->
            val distance = editDistance(entry.label.lowercase(), needle)
            if (distance <= tolerance(needle)) entry to distance else null
        }
            .sortedWith(
                compareBy<Pair<AppEntry, Int>> { it.second }
                    .thenBy { it.first.label.length }
                    .thenBy { it.first.component.flattenToShortString() },
            )
            .map { it.first }
    }

    /**
     * Null when [needle] is not a subsequence of [haystack].
     *
     * Greedy and leftmost: each character is taken at its earliest remaining position, which is
     * cheap and gives the same answer every time.
     */
    private fun subsequenceScore(haystack: String, needle: String): Int? {
        var from = 0
        var previous = -2
        var boundaries = 0
        var runs = 0

        for (character in needle) {
            val at = haystack.indexOf(character, from)
            if (at < 0) return null
            if (at == previous + 1) runs++
            if (at == 0 || !haystack[at - 1].isLetterOrDigit()) boundaries++
            previous = at
            from = at + 1
        }

        // Boundaries dominate runs, which dominate everything else, so the tiers never blur.
        return boundaries * 100 + runs * 10
    }

    /** Room for one mistake in a short query, and a little more as it gets longer. */
    private fun tolerance(needle: String) = when {
        needle.length <= 3 -> 1
        needle.length <= 6 -> 2
        else -> 3
    }

    private fun editDistance(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = min(min(current[j - 1] + 1, previous[j] + 1), substitution)
            }
            val swap = previous
            previous = current
            current = swap
        }

        return previous[b.length]
    }
}
