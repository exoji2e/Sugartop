package io.exoji2e.erbil

import android.os.Bundle
import kotlinx.android.synthetic.main.result_layout.*
import com.github.mikephil.charting.data.Entry
import java.util.*

class ResultActivity : ErbilActivity() {
    override val TAG = "ResultActivity"
    override fun getNavigationMenuItemId() = R.id.action_recent
    override fun getContentViewId() = R.layout.result_layout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onResume()
    }

    override fun onResume() {
        super.onResume()
        showLayout()
    }

    private fun showLayout() {
        val task = Runnable {
            val dc = DataContainer.getInstance(this)
            val predict = dc.guess()
            if(predict == null) return@Runnable
            val readings = dc.get8h()

            val last = readings.last().value
            val timeStamp = dc.lastTimeStamp()
            TimeLeftTV.text = String.format("Time left %s %d", Time.timeLeft(timeStamp), timeStamp)

            val diff = RawParser.sensor2mmol(predict.value) - RawParser.sensor2mmol(last)
            val trend = if (diff > .5) "↑↑" else if (diff > .25) "↑" else if (diff < -.5) "↓↓" else if (diff < -.25) "↓" else "→"

            val avg = Compute.avg(readings)
            val percentInside = Compute.inGoal(4.0, 8.0, readings) * 100
            val values = ArrayList<Entry>()
            val now = Time.now()
            val first: Long = if (readings.isEmpty()) now else readings[0].utcTimeStamp
            values.addAll(readings.map { r -> Entry((r.utcTimeStamp - first).toFloat(), r.tommol().toFloat()) })
            values.add(Entry((predict.utcTimeStamp - first).toFloat(), predict.tommol().toFloat()))
            Push2Plot.setPlot(values, graph, first)
            ingData.text = String.format("%.1f %s", percentInside, "%")
            avgData.text = String.format("%.1f", avg)
            recentData.text = String.format("%.1f %s", RawParser.sensor2mmol(predict.value), trend)
        }
        DbWorkerThread.getInstance().postTask(task)
    }
}
