package io.exoji2e.erbil.activities

import io.exoji2e.erbil.*
import io.exoji2e.erbil.database.DbWorkerThread
import io.exoji2e.erbil.database.ErbilDataBase
import kotlinx.android.synthetic.main.recent_layout.*

class RecentActivity : ErbilActivity() {
    override val TAG = "RecentActivity"
    override fun getNavigationMenuItemId() = R.id.action_recent
    override fun getContentViewId() = R.layout.recent_layout

    override fun onResume() {
        super.onResume()
        showLayout()
    }

    private fun showLayout() {
        val task = Runnable {
            val dc = DataContainer.getInstance(this)
            val sd = SensorData.instance(this)

            val guess = dc.guess()
            val now = Time.now()
            val guess_time = now + Time.MINUTE * 5
            val end = Time.floor_hour(guess_time + Time.HOUR)
            val start = end - Time.HOUR * 8

            val readings = dc.get(start, end)
            val manual = ErbilDataBase.getInstance(this).manualEntryDao().getAll().filter{ entry -> entry.utcTimeStamp > start && entry.utcTimeStamp < end}

            val toPlot = if(guess == null) readings.map { g -> g.toReading() } else readings.map { g -> g.toReading() } + guess.second
            val timeStamp = dc.lastTimeStamp()
            val (above, left, unit) = Time.timeLeft(timeStamp)

            var recentText = "-"
            if(readings.isNotEmpty() && guess != null) {

                val diff = guess.second.tommol(sd) - guess.first.tommol(sd)
                val trend = if (diff > .5) "↑↑" else if (diff > .25) "↑" else if (diff < -.5) "↓↓" else if (diff < -.25) "↓" else "→"
                if (Time.now() - readings.last().utcTimeStamp < Time.HOUR)
                    recentText = String.format("%.1f %s", guess.second.tommol(sd), trend)
            }
            if(recentText.equals("-") && manual.isNotEmpty()) {
                if(Time.now() - manual.last().utcTimeStamp < Time.HOUR)
                    recentText = String.format("%.1f", manual.last().value)
            }

            val avg = Compute.avg(readings, sd)

            if(graph!=null && ingData != null && avgData != null && recentData != null && TimeLeftText != null && TimeLeftData != null && TimeLeftUnit != null) {
                graph.post { Push2Plot._setPlot(toPlot, manual, graph, start, end, sd, Push2Plot.PlotType.RECENT) }

                ingData.text = Compute.inGoal(4.0, 8.0, readings, sd)
                avgData.text = String.format("%.1f", avg)
                recentData.text = recentText
                TimeLeftText.text = above
                TimeLeftData.text = left
                TimeLeftUnit.text = unit
            }
        }
        DbWorkerThread.getInstance().postTask(task)
    }
}
