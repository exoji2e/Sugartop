package io.exoji2e.erbil

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_result.*
import com.github.mikephil.charting.data.Entry
import java.util.*

class ResultActivity : AppCompatActivity() {
    private var dc : DataContainer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        dc = DataContainer.getInstance(this)
        showLayout()
    }
    private fun fmt(str: String, readVal: Int) =
            String.format("%s %d %.2f", str, readVal, RawParser.sensor2mmol(readVal))

    private fun showLayout() {
        val raw_data = dc!!.dump()
        val predict = RawParser.guess(raw_data)
        val last = RawParser.last(raw_data)
        timestampTextV.text = String.format("%s %d", getResources().
                getString(R.string.timestamp), RawParser.timestamp(raw_data))

        historyTextV.text = fmt(resources.
                getString(R.string.history), RawParser.history(raw_data)[31].value)
        recentTextV.text = fmt(resources.
                getString(R.string.recent), last)
        guessTextV.text = fmt(resources.
                getString(R.string.guess), predict)

        val readings = dc!!.get8h()
        val avg = Compute.avg(readings)
        avgTextV.text = String.format("%s %.1f", resources.getString(R.string.avg), avg)
        val percentInside = Compute.inGoal(4.0, 8.0, readings)*100
        ingoalTextV.text = String.format("%s %.1f%s", resources.getString(R.string.ingoal), percentInside, "%")
        val values = ArrayList<Entry>()
        val first = readings[0].utcTimeStamp
        values.addAll(readings.map{r -> Entry((r.utcTimeStamp - first).toFloat(), r.tommol().toFloat())})
        values.add(Entry((Time.now() + 5*Time.MINUTE - first).toFloat(), RawParser.sensor2mmol(predict).toFloat()))
        Push2Plot.setPlot(values, GraphBelowRes, first)
    }
}
