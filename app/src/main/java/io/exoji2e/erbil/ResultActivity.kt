package io.exoji2e.erbil

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

import kotlinx.android.synthetic.main.activity_result.*

class ResultActivity : AppCompatActivity() {
    private var dc : DataContainer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)
        dc = DataContainer.getInstance(this)
        showLayout()
    }
    private fun showLayout() {
        val raw_data = dc!!.dump()
        val predict = RawParser.guess(raw_data)
        timestamp.text = String.format("%s %d", getResources().
                getString(R.string.timestamp), RawParser.timestamp(raw_data))
        history.text = String.format("%s %d size: %d", getResources().
                getString(R.string.history), RawParser.history(raw_data)[31].value, dc!!.size())
        recent.text = String.format("%s %d", getResources().
                getString(R.string.recent), RawParser.last(raw_data))
        guess.text = String.format("%s %d %.2f", getResources().
                getString(R.string.guess), predict, RawParser.sensor2mmol(predict))

        val history = dc!!.get8h()
        //TODO: Change/Fix Graph lib.
        val out = history.map({v -> v.toDataPoint()}).toTypedArray()
        val series = LineGraphSeries<DataPoint>(out)
        GraphBelowRes.addSeries(series)

        GraphBelowRes.getGridLabelRenderer().setLabelFormatter(HourFormatter())

        GraphBelowRes.viewport.setXAxisBoundsManual(true)
        GraphBelowRes.viewport.setMinX(history[0].utcTimeStamp.toDouble());
        GraphBelowRes.viewport.setMaxX(history[history.size -1].utcTimeStamp.toDouble());
        GraphBelowRes.gridLabelRenderer.numHorizontalLabels = 6
        GraphBelowRes.gridLabelRenderer.verticalAxisTitle = "mmol/L"
    }
}
