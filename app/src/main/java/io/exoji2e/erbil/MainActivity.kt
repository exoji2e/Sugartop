package io.exoji2e.erbil

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.github.mikephil.charting.data.Entry
import java.util.ArrayList
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    var dc : DataContainer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startReadActivity()
    }

    override fun onResume() {
        super.onResume()
        dc = DataContainer.getInstance(this)
        val readings = dc!!.get24h()
        val avg = Compute.avg(readings)
        // TODO: Move constants to user.
        val percentInside = Compute.inGoal(4.0, 8.0, readings)*100
        val values = ArrayList<Entry>()
        val first : Long = if (readings.isEmpty()) 0L else readings[0].utcTimeStamp
        val recent : Double = if (readings.isEmpty()) 0.0 else readings.last().tommol()
        values.addAll(readings.map{r -> Entry((r.utcTimeStamp - first).toFloat(), r.tommol().toFloat()) })
        if (readings.size > 1) Push2Plot.setPlot(values, graph, first)
        ingData.text = String.format("%.1f %s", percentInside, "%")
        avgData.text = String.format("%.1f", avg)
        recentData.text = String.format("%.1f", recent)
    }

    fun startReadActivity() {
        val intent = Intent(this, ReadActivity::class.java)
        startActivity(intent)
    }
}
