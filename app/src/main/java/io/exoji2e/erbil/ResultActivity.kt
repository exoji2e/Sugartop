package io.exoji2e.erbil

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_result.*
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import java.text.DateFormat
import java.util.*
import kotlin.math.roundToInt


class ResultActivity : AppCompatActivity() {
    private var dc : DataContainer? = null
    val hotPink = Color.rgb(255, 64, 129)

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

        historyTextV.text = fmt(getResources().
                getString(R.string.history), RawParser.history(raw_data)[31].value)
        recentTextV.text = fmt(getResources().
                getString(R.string.recent), last)
        guessTextV.text = fmt(getResources().
                getString(R.string.guess), predict)

        val readings = dc!!.get8h()
        val avg = Compute.avg(readings).roundToInt()
        avgTextV.text = fmt(getResources().getString(R.string.avg), avg)
        val percentInside = Compute.inGoal(4.0, 8.0, readings)*100
        ingoalTextV.text = String.format("%s %.1f%s", getResources().getString(R.string.ingoal), percentInside, "%")
        val values = ArrayList<Entry>()
        val first = readings[0].utcTimeStamp
        values.addAll(readings.map{r -> Entry((r.utcTimeStamp - first).toFloat(), r.tommol().toFloat())})
        values.add(Entry((Time.now() + 5*Time.MINUTE - first).toFloat(), RawParser.sensor2mmol(predict).toFloat()))
        val dataSet = LineDataSet(values,"")
        dataSet.axisDependency = AxisDependency.LEFT
        dataSet.color = ColorTemplate.getHoloBlue()
        dataSet.valueTextColor = ColorTemplate.getHoloBlue()
        dataSet.lineWidth = 3f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.fillAlpha = 65
        dataSet.fillColor = ColorTemplate.getHoloBlue()
        dataSet.highLightColor = Color.rgb(244, 117, 117)
        dataSet.setDrawCircleHole(false)

        GraphBelowRes.setData(LineData(dataSet))
        GraphBelowRes.getLegend().setEnabled(false)
        GraphBelowRes.getDescription().setText("");
        GraphBelowRes.invalidate();
        val xAxis = GraphBelowRes.getXAxis()
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM)
        xAxis.setTextSize(12f)
        xAxis.setDrawAxisLine(false)
        xAxis.setDrawGridLines(true)
        xAxis.setTextColor(hotPink)
        xAxis.setLabelCount(5, true)
        xAxis.setValueFormatter(object : IAxisValueFormatter {
            private val mCalendar = Calendar.getInstance()

            override fun getFormattedValue(value: Float, axis: AxisBase): String {
                mCalendar.setTimeInMillis(value.toLong() + first);
                val hhmm = DateFormat.getTimeInstance(DateFormat.SHORT).format(mCalendar.getTimeInMillis());
                return hhmm
            }
        })

        val leftAxis = GraphBelowRes.getAxisLeft()
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        leftAxis.setDrawGridLines(true)
        leftAxis.setGranularity(4f)
        leftAxis.setAxisMinimum(2f)
        leftAxis.setAxisMaximum(17f)
        leftAxis.setTextSize(12f)
        leftAxis.setTextColor(hotPink)

        val rightAxis = GraphBelowRes.getAxisRight()
        rightAxis.setEnabled(false)
    }
}
