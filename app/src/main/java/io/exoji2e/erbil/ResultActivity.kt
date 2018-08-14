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
            val first: Long = readings[0].utcTimeStamp
            val values = readings
                    .groupBy { g -> g.sensorId }
                    .mapValues{(id, glucs) ->
                        glucs.map{r -> Entry((r.utcTimeStamp - first).toFloat(), r.tommol().toFloat()) }
                                .toMutableList()}
            values[predict.sensorId]?.add(Entry((predict.utcTimeStamp - first).toFloat(), predict.tommol().toFloat()))

            Push2Plot.setPlot(values, graph, first)
            ingData.text = Compute.inGoal(4.0, 8.0, readings)
            avgData.text = String.format("%.1f", avg)
            recentData.text = String.format("%.1f %s", RawParser.sensor2mmol(predict.value), trend)
        }
        DbWorkerThread.getInstance().postTask(task)
    }
}
