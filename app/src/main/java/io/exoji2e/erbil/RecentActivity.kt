package io.exoji2e.erbil

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
            val predict = dc.guess()
            if(predict == null) return@Runnable
            val readings = dc.get8h()

            val last = readings.last().value
            val timeStamp = dc.lastTimeStamp()
            val (left, unit) = Time.timeLeft(timeStamp)
            TimeLeftData.text = left
            TimeLeftUnit.text = unit

            val diff = RawParser.sensor2mmol(predict.value) - RawParser.sensor2mmol(last)
            val trend = if (diff > .5) "↑↑" else if (diff > .25) "↑" else if (diff < -.5) "↓↓" else if (diff < -.25) "↓" else "→"

            val avg = Compute.avg(readings)
            graph.post{Push2Plot._setPlot(readings.map{g -> g.toReading()} + predict, graph, Time.now() - Time.HOUR*8, Time.now() + 5*Time.MINUTE)}
            ingData.text = Compute.inGoal(4.0, 8.0, readings)
            avgData.text = String.format("%.1f", avg)
            recentData.text = String.format("%.1f %s", RawParser.sensor2mmol(predict.value), trend)
        }
        DbWorkerThread.getInstance().postTask(task)
    }
}
