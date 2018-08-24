package io.exoji2e.erbil

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.text.DateFormat
import java.util.*

class Push2Plot {
    companion object {
        fun standardLineDataSet(data : List<Entry>, dash : Boolean, color : Int) : LineDataSet {
            val dataSet = LineDataSet(data,"")
            dataSet.axisDependency = YAxis.AxisDependency.LEFT
            dataSet.color = color
            if(dash) {
                dataSet.lineWidth = 1f
            }else {
                dataSet.lineWidth = 3f
            }
            dataSet.setDrawCircles(false)
            dataSet.setDrawValues(false)
            dataSet.fillAlpha = 65
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
            for ((i, li) in values.toList().withIndex()) {
                sets.add(standardLineDataSet(li.second, false, Color.lineColor(i)))
            }
            // TODO: Move constants to user.
            val lo = arrayListOf(Entry(start.toFloat(), 4f), Entry(end.toFloat(), 4f))
            val hi = arrayListOf(Entry(start.toFloat(), 8f), Entry(end.toFloat(), 8f))
            sets.add(standardLineDataSet(lo, true, Color.gray))
            sets.add(standardLineDataSet(hi, true, Color.gray))
            graph.data = LineData(sets as List<ILineDataSet>?)
            graph.legend.isEnabled = false
            graph.description.text = ""
            val xAxis = graph.xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 12f
            xAxis.setDrawAxisLine(false)
            xAxis.setDrawGridLines(true)
            xAxis.textColor = Color.black
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
            leftAxis.textSize = 16f
            leftAxis.textColor = Color.black

            val rightAxis = graph.getAxisRight()
            rightAxis.setDrawGridLines(false)
            rightAxis.setLabelCount(0, true)

            rightAxis.isEnabled = false
            graph.invalidate()

        }
    }
}