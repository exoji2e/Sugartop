package io.exoji2e.sugartop.activities

import io.exoji2e.sugartop.*
import io.exoji2e.sugartop.database.*
import io.exoji2e.sugartop.settings.UserData
import kotlinx.android.synthetic.main.element_recent_info.view.*
import kotlinx.android.synthetic.main.recent_layout.*

class RecentActivity : BaseActivity() {
    override val TAG = "RecentActivity"
    override fun getNavigationMenuItemId() = R.id.action_recent
    override fun getContentViewId() = R.layout.recent_layout

    override fun onResume() {
        super.onResume()
        showLayout()
    }

    private fun get_recent_text(readings: List<GlucoseEntry>,
                                guess : Pair<GlucoseReading, GlucoseReading>?,
                                sd: SensorData,
                                manual : List<ManualGlucoseEntry>) : String{
        if(readings.isNotEmpty() && guess != null) {
            val diff = guess.second.tommol(sd) - guess.first.tommol(sd)
            val trend = if (diff > .5) "↑↑" else if (diff > .25) "↑" else if (diff < -.5) "↓↓" else if (diff < -.25) "↓" else "→"
            if (Time.now() - readings.last().utcTimeStamp < Time.HOUR)
                return String.format("%.1f %s", guess.second.tommol(sd), trend)
        }
        if(manual.isNotEmpty()) {
            if(Time.now() - manual.last().utcTimeStamp < Time.HOUR)
                return String.format("%.1f", manual.last().value)
        }
        return "-"

    }

    private fun showLayout() {
        val task = Runnable {
            val dc = DataContainer.getInstance(this)
            val sd = SensorData.instance(this)

            val guess : Pair<GlucoseReading, GlucoseReading>? = dc.guess()
            val now = Time.now()
            val guess_time = now + Time.MINUTE * 5
            val end = Time.floor_hour(guess_time + Time.HOUR)
            val start = end - Time.HOUR * 8

            val readings = dc.get(start, end)
            val manual = GlucoseDataBase.getInstance(this).manualEntryDao().getAll().filter{ entry -> entry.utcTimeStamp > start && entry.utcTimeStamp < end}

            val toPlot = if(guess == null) readings.map { g -> g.toReading() } else readings.map { g -> g.toReading() } + guess.second
            val timeStamp = dc.lastTimeStamp()
            val (above, left, unit) = Time.timeLeft(timeStamp)

            val recentText = get_recent_text(readings, guess, sd, manual)
            val avg = Compute.avg(readings, sd)
            val thresholds = Pair(UserData.get_low_threshold(this), UserData.get_hi_threshold(this))


            if(graph!=null) {
                recentData.text = recentText
                Push2Plot._setPlot(toPlot, manual, graph, start, end, sd, Push2Plot.PlotType.RECENT, thresholds)
                val goal = Compute.inGoal(thresholds.first, thresholds.second, readings, sd)
                insideBox.post {
                    insideBox.recent_info_text.text = "Inside target:"
                    insideBox.recent_info_data.text = goal
                }
                avgBox.post {
                    avgBox.recent_info_text.text = "Average:"
                    avgBox.recent_info_data.text = String.format("%.1f", avg)
                    avgBox.recent_info_unit.text = "mmol/L"
                }
                timeBox.post {
                    timeBox.recent_info_text.text = above
                    timeBox.recent_info_data.text = left
                    timeBox.recent_info_unit.text = unit
                }
            }
        }
        DbWorkerThread.getInstance().postTask(task)
    }
}
