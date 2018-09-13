package io.exoji2e.erbil

import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
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
            dataSet.setDrawCircleHole(false)
            return dataSet
        }
        fun setPlot(entries : List<GlucoseEntry>, manual : List<ManualGlucoseEntry>, graph : CombinedChart, start: Long, end : Long) {
            _setPlot(entries.map{g -> g.toReading()}, manual, graph, start, end)
        }

        fun scatter(data : List<Entry>) : ScatterDataSet {
            val dataSet = ScatterDataSet(data, "")
            dataSet.color = Color.hotPink
            dataSet.axisDependency = YAxis.AxisDependency.LEFT
            return dataSet
        }
        fun _setPlot(entries : List<GlucoseReading>, graph : CombinedChart, start: Long, end : Long) : Unit {
            _setPlot(entries, mutableListOf<ManualGlucoseEntry>(), graph, start, end)
        }
        fun _setPlot(entries : List<GlucoseReading>, manual : List<ManualGlucoseEntry>, graph : CombinedChart, start: Long, end : Long) : Unit {
            val values = entries
                    .groupBy { g -> g.sensorId }
                    .mapValues{(_, glucs) ->
                        glucs.map{r -> Entry((r.utcTimeStamp).toFloat(), r.tommol().toFloat()) }
                                .toMutableList()}
            val sets = mutableListOf<LineDataSet>()
            for ((i, li) in values.toList().withIndex()) {
                sets.add(standardLineDataSet(li.second, false, Color.lineColor(i)))
            }
            val scatter = ScatterData(scatter(manual.map{ m -> Entry(m.utcTimeStamp.toFloat(), m.value.toFloat())}))
            // TODO: Move constants to user.
            val lo = arrayListOf(Entry(start.toFloat(), 4f), Entry(end.toFloat(), 4f))
            val hi = arrayListOf(Entry(start.toFloat(), 8f), Entry(end.toFloat(), 8f))
            sets.add(standardLineDataSet(lo, true, Color.gray))
            sets.add(standardLineDataSet(hi, true, Color.gray))
            val combData = CombinedData()
            combData.setData(LineData(sets as List<ILineDataSet>?))
            if(manual.isNotEmpty()) combData.setData(scatter)
            graph.data = combData
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