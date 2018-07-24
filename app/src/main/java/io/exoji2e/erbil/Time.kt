package io.exoji2e.erbil

class Time {
    companion object {
        val SECOND = 1000L
        val MINUTE = 60L*SECOND
        val HOUR = 60L*MINUTE
        fun now() : Long = System.currentTimeMillis()
    }
}