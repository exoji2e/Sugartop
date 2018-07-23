package io.exoji2e.erbil

import android.util.Log

class Compute {
    companion object {
        fun avg(data : List<Reading>) : Double {
            var sum = 0.0
            for (i in 1..data.size - 1) {
                val tdiff = data[i].utcTimeStamp - data[i-1].utcTimeStamp
                sum += (data[i-1].readingValue + data[i].readingValue)/2.0*tdiff
            }
            return sum/(data.last().utcTimeStamp - data[0].utcTimeStamp)
        }
        fun inGoal(lo: Double, hi : Double, data: List<Reading>) : Double {
            var sum = 0.0
            for (i in 1..data.size - 1) {
                val tdiff = data[i].utcTimeStamp - data[i-1].utcTimeStamp
                val (v0, v1) = Pair(data[i-1].readingValue, data[i].readingValue)
                val (min, max) = Pair(Math.min(v0, v1), Math.max(v0, v1))
                //Log.w("COMPUTE", "" + min + " " + max)
                if (max <= lo) continue
                if (min >= hi) continue
                var (ts, tf) = Pair(0.0, tdiff.toDouble())
                if (min < lo) {
                    ts = (lo - min)*tdiff / (max - min)
                }
                if (max > hi) {
                    tf = tdiff*(1 - (max - hi)/(max - min))
                }
                sum += tf - ts
            }
            return sum/(data.last().utcTimeStamp - data[0].utcTimeStamp)
        }
    }
}