package io.exoji2e.erbil

import android.content.Intent
import android.support.v4.app.ShareCompat
import android.widget.Toast
import kotlinx.android.synthetic.main.dashboard_layout.*
import java.io.BufferedReader
import java.io.InputStreamReader


class MainActivity : ErbilActivity() {
    override val TAG = "MAIN-Activity"
    override fun getNavigationMenuItemId() = R.id.action_dashboard
    override fun getContentViewId() = R.layout.dashboard_layout

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if(intent.action == Intent.ACTION_SEND) {
            val task = Runnable {
                val dc = DataContainer.getInstance(this)
                // Should maybe support merging later on?
                if(dc.size() != 0){
                    Toast.makeText(this, "Database not empty, aborting import.", Toast.LENGTH_LONG).show()
                    return@Runnable
                }
                val intentReader = ShareCompat.IntentReader.from(this)
                dc.insert(read(intentReader))
            }
            DbWorkerThread.getInstance().postTask(task)
        }
    }
    override fun onResume() {
        super.onResume()
        val task = Runnable {
            val dc = DataContainer.getInstance(this)
            val readings = dc.get24h()
            val avg = Compute.avg(readings)
            // TODO: Move constants to user.
            val recent: Double = if (readings.isEmpty()) 0.0 else readings.last().tommol()
            if (readings.size > 1) Push2Plot.setPlot(readings, graph, Time.now() - Time.DAY, Time.now())
            ingData.text = Compute.inGoal(4.0, 8.0, readings)
            avgData.text = String.format("%.1f", avg)
            recentData.text = String.format("%.1f", recent)
        }
        DbWorkerThread.getInstance().postTask(task)
    }
    private fun read(intentReader: ShareCompat.IntentReader) : List<GlucoseEntry> {
        val s = intentReader.stream
        val br = BufferedReader(InputStreamReader(contentResolver.openInputStream(s)))
        var lines = br.readLines()
        val entries = mutableListOf<GlucoseEntry>()
        for (line in lines) {
            val entry = GlucoseEntry.fromString(line)
            if(entry != null)
                entries.add(entry)
        }
        return entries
    }
}
