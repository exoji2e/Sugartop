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
            val readings = dc.get8h()
            val start = Time.now() - Time.HOUR*8
            val end = start + Time.HOUR*8 + Time.MINUTE*5
            val manual = ErbilDataBase.getInstance(this).manualEntryDao().getAll().filter{entry -> entry.utcTimeStamp > start && entry.utcTimeStamp < end}

            val toPlot = if(predict == null) readings.map { g -> g.toReading() } else readings.map { g -> g.toReading() } + predict
            val timeStamp = dc.lastTimeStamp()
            val (above, left, unit) = Time.timeLeft(timeStamp)

            var recentText = "-"
            if(readings.isNotEmpty() && predict != null) {
                val diff = predict.tommol() - readings.last().tommol()
                val trend = if (diff > .5) "↑↑" else if (diff > .25) "↑" else if (diff < -.5) "↓↓" else if (diff < -.25) "↓" else "→"
                if (Time.now() - readings.last().utcTimeStamp < Time.HOUR)
                    recentText = String.format("%.1f %s", predict.tommol(), trend)
            }
            if(recentText.equals("-") && manual.isNotEmpty()) {
                if(Time.now() - manual.last().utcTimeStamp < Time.HOUR)
                    recentText = String.format("%.1f", manual.last().value)
            }

            val avg = Compute.avg(readings)

            if(graph!=null && ingData != null && avgData != null && recentData != null && TimeLeftText != null && TimeLeftData != null && TimeLeftUnit != null) {
                graph.post { Push2Plot._setPlot(toPlot, manual, graph, Time.now() - Time.HOUR * 8, Time.now() + 5 * Time.MINUTE) }
                ingData.text = Compute.inGoal(4.0, 8.0, readings)
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
