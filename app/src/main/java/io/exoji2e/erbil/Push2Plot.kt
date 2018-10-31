package io.exoji2e.erbil

import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import io.exoji2e.erbil.database.GlucoseEntry
import io.exoji2e.erbil.database.GlucoseReading
import io.exoji2e.erbil.database.ManualGlucoseEntry
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
        fun setPlot(entries : List<GlucoseEntry>, manual : List<ManualGlucoseEntry>, graph : CombinedChart, start: Long, end : Long, sd : SensorData) {
            _setPlot(entries.map{g -> g.toReading()}, manual, graph, start, end, sd)
        }

        fun scatter(data : List<Entry>) : ScatterDataSet {
            val dataSet = ScatterDataSet(data, "")
            dataSet.color = Color.hotPink
            dataSet.axisDependency = YAxis.AxisDependency.LEFT
            return dataSet
        }

        fun _setPlot(entries : List<GlucoseReading>, manual : List<ManualGlucoseEntry>, graph : CombinedChart, start: Long, end : Long, sd : SensorData) : Unit {
            val values = entries
                    .groupBy { g -> g.sensorId }
                    .mapValues{(_, glucs) ->
                        glucs.map{r -> Entry((r.utcTimeStamp).toFloat(), r.tommol(sd).toFloat()) }
                                .toMutableList()}
            val sets = mutableListOf<LineDataSet>()
            var max = 17f
            var min = 2f
            for ((i, li) in values.toList().withIndex()) {
                val vmax = li.second.maxBy{v -> v.y}?.y
                val vmin = li.second.minBy{v -> v.y}?.y
                if(vmax != null) max = Math.max(max, vmax)
                if(vmin != null) min = Math.min(min, vmin)
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
            if(manual.isNotEmpty()){
                combData.setData(scatter)
                val mmin = manual.minBy { m -> m.value }?.value?.toFloat()
                val mmax = manual.maxBy { m -> m.value }?.value?.toFloat()
                if(mmin != null) min = Math.min(min, mmin)
                if(mmax != null) max = Math.max(max, mmax)
            }
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
            xAxis.axisMinimum = start.toFloat()
            xAxis.axisMaximum = end.toFloat()
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
            leftAxis.axisMinimum = Math.max(min, 0f)
            leftAxis.axisMaximum = Math.min(max, 25f)
            leftAxis.textSize = 12f
            leftAxis.textColor = Color.black

            val rightAxis = graph.getAxisRight()
            rightAxis.setDrawGridLines(false)
            rightAxis.setLabelCount(0, true)

            rightAxis.isEnabled = false
            graph.invalidate()

        }
    }
}