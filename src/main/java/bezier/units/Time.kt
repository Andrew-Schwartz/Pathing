package bezier.units

class Time(seconds: Double) : SIUnit<Time>(seconds) {
    companion object {
        const val baseFromHundredMillis = 0.01
        const val baseFromSeconds = 1
        const val baseFromMinutes = 60

        fun seconds(seconds: Double): Time = Time(seconds * baseFromSeconds)
        fun minutes(minutes: Double): Time = Time(minutes * baseFromMinutes)
        fun hundredMillis(hundredMillis: Double): Time = Time(hundredMillis * baseFromHundredMillis)
    }

    val asSeconds: Double get() = value / baseFromSeconds
    val asMinutes: Double get() = value / baseFromMinutes
    val asHundredMillis: Double get() = value / baseFromHundredMillis
}