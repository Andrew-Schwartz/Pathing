package bezier

infix fun ClosedRange<Double>.step(step: Double): Iterable<Double> {
    val sequence = generateSequence(start) { previous ->
        val next = previous + step
        if (next > endInclusive) null else next
    }
    return sequence.asIterable()
}

fun <T : Comparable<T>> T.maxVs(other: T): T = if (this > other) this else other

fun <T : Comparable<T>> T.minVs(other: T): T = if (this < other) this else other