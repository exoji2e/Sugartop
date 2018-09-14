package io.exoji2e.erbil

import java.text.SimpleDateFormat
import java.util.*

class Time {
    companion object {
        val SECOND = 1000L
        val MINUTE = 60L*SECOND
        val HOUR = 60L*MINUTE
        val DAY = 24L*HOUR
        fun now() : Long = System.currentTimeMillis()
        fun timeLeft(timeStamp: Int) : Triple<String,String,String> {
            val left = 14*24*60 - timeStamp
            val days = left/(24*60)
            val hours = (left/60)%24
            val minutes = left%60
            return if(left < 0)
                Triple("Finished", String.format("%d",-left), "minutes ago")
            else if(left < 60)
                Triple("Time Left", minutes.toString(), "minutes")
            else if(left < 60*24)
                Triple("Time Left", String.format("%d:%d", hours, minutes), "hours:minutes")
            else
                Triple("Time Left", String.format("%d:%d", days, hours), "days:hours")
        }
        fun datetime(): String {
            return datetime(now())
        }
        fun datetime(t : Long): String {
            val mSegmentStartTimeFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm")
            val mCalendar = Calendar.getInstance()
            mCalendar.timeInMillis = t
            return mSegmentStartTimeFormatter.format(mCalendar.getTimeInMillis())
        }
        fun date(t: Long) : String {
            val mSegmentStartTimeFormatter = SimpleDateFormat("yyyy-MM-dd")
            val mCalendar = Calendar.getInstance()
            mCalendar.timeInMillis = t
            return mSegmentStartTimeFormatter.format(mCalendar.getTimeInMillis())
        }
        fun date_as_int() : Int {
            val date_str = date(now())
            val (y, m, d) = date_str.split("-").map{i -> i.toInt()}
            return d + m*31 + (y-1970)*31*12
        }
        fun floor_day(t : Long) : Long {
            val mSegmentStartTimeFormatter = SimpleDateFormat("HH:mm:ss")
            val mCalendar = Calendar.getInstance()
            mCalendar.timeInMillis = t
            val time_str = mSegmentStartTimeFormatter.format(mCalendar.getTimeInMillis())
            val (h, m, s) = time_str.split(":").map{i -> i.toInt()}
            return t - h* HOUR - m* MINUTE - s * SECOND
        }
        fun date_as_string(daysSince: Int) : String {
            return date(now() - daysSince*DAY)
        }
        fun limits(v : Int) : Pair<Long, Long> {
            val start = floor_day(Time.now() - v*DAY)
            return Pair(start, start + DAY)
        }

    }
}