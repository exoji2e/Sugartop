package io.exoji2e.erbil

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.DateFormat
import java.util.*

class Push2Plot {
    companion object {
        private val hotPink = Color.rgb(255, 64, 129)
        private val gray = Color.rgb(100, 100, 100)
        fun standardLineDataSet(data : List<Entry>, dash : Boolean, color : Int) : LineDataSet {
            val dataSet = LineDataSet(data,"")
            dataSet.axisDependency = YAxis.AxisDependency.LEFT
            dataSet.valueTextColor = ColorTemplate.getHoloBlue()
            dataSet.color = color
            if(dash) {
                dataSet.lineWidth = 1f
                //dataSet.color = Color.rgb(100, 100, 100)
            }else {
                dataSet.lineWidth = 3f
                //dataSet.color = ColorTemplate.getHoloBlue()
            }
            dataSet.setDrawCircles(false)
            dataSet.setDrawValues(false)
            dataSet.fillAlpha = 65
            dataSet.fillColor = ColorTemplate.getHoloBlue()
            dataSet.highLightColor = Color.rgb(244, 117, 117)
            dataSet.setDrawCircleHole(false)
            return dataSet
        }
        fun setPlot(entries : List<GlucoseEntry>, graph : LineChart, start: Long, end : Long) {
            _setPlot(entries.map{g -> g.toReading()}, graph, start, end)
        }

        fun _setPlot(entries : List<GlucoseReading>, graph : LineChart, start: Long, end : Long) : Unit {
            val values = entries
                    .groupBy { g -> g.sensorId }
                    .mapValues{(_, glucs) ->
                        glucs.map{r -> Entry((r.utcTimeStamp).toFloat(), r.tommol().toFloat()) }
                                .toMutableList()}
            val sets = mutableListOf<LineDataSet>()
            for ((_, li) in values) {
                sets.add(standardLineDataSet(li, false, ColorTemplate.getHoloBlue()))
            }
            // TODO: Move constants to user.
            val lo = mutableListOf<Entry>()
            lo.add(Entry(start.toFloat(), 4f))
            lo.add(Entry(end.toFloat(), 4f))
            val hi = mutableListOf<Entry>()
            hi.add(Entry(start.toFloat(), 8f))
            hi.add(Entry(end.toFloat(), 8f))
            sets.add(standardLineDataSet(lo, true, gray))
            sets.add(standardLineDataSet(hi, true, gray))
            graph.data = LineData(sets as List<ILineDataSet>?)
            graph.legend.isEnabled = false
            graph.description.text = ""
            val xAxis = graph.getXAxis()
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 12f
            xAxis.setDrawAxisLine(false)
            xAxis.setDrawGridLines(true)
            xAxis.textColor = hotPink
            xAxis.setLabelCount(6, false)
            xAxis.valueFormatter = object : IAxisValueFormatter {
                private val mCalendar = Calendar.getInstance()
                override fun getFormattedValue(value: Float, axis: AxisBase): String {
                    mCalendar.setTimeInMillis(value.toLong())
                    val hhmm = DateFormat.getTimeInstance(DateFormat.SHORT).format(mCalendar.getTimeInMillis())
                    return hhmm
                }
            }

            val leftAxis = graph.getAxisLeft()
            leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
            leftAxis.setDrawGridLines(true)
            leftAxis.granularity = 4f
            leftAxis.axisMinimum = 2f
            leftAxis.axisMaximum = 17f
            leftAxis.textSize = 12f
            leftAxis.textColor = hotPink

            val rightAxis = graph.getAxisRight()
            rightAxis.setDrawGridLines(false)
            rightAxis.setLabelCount(0, true)

            rightAxis.isEnabled = false
            graph.invalidate()

        }
    }
}