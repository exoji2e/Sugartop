package io.exoji2e.erbil

class Time {
    companion object {
        val SECOND = 1000L
        val MINUTE = 60L*SECOND
        val HOUR = 60L*MINUTE
        fun now() : Long = System.currentTimeMillis()
        fun timeLeft(timeStamp: Int) : String {
            val left = 21328 - timeStamp
            return if(left < 0)
                "Finished"
            else if(left < 60)
                String.format("%d minutes", left)
            else if(left < 60*24)
                String.format("%d hours", left/60)
            else
                String.format("%d days", left / (24 * 60))
        }
    }
}