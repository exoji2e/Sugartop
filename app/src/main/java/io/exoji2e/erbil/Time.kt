package io.exoji2e.erbil

import java.text.SimpleDateFormat
import java.util.*

class Time {
    companion object {
        val SECOND = 1000L
        val MINUTE = 60L*SECOND
        val HOUR = 60L*MINUTE
        fun now() : Long = System.currentTimeMillis()
        fun timeLeft(timeStamp: Int) : String {
            val left = 21328 - timeStamp
            val days = left/(24*60)
            val hours = (left/60)%24
            val minutes = left%60
            return if(left < 0)
                "Finished"
            else if(left < 60)
                String.format("%d%s", minutes, "m")
            else if(left < 60*24)
                String.format("%d%s:%d%s", hours, "h", minutes, "m")
            else
                String.format("%d%s:%d%s", days, "d", hours, "h")
        }
        fun datetime(): String {
            val t = now()
            val mSegmentStartTimeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
            val mCalendar = Calendar.getInstance()
            mCalendar.timeInMillis = t
            return mSegmentStartTimeFormatter.format(mCalendar.getTimeInMillis())
        }
    }
}