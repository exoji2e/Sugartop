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
        fun setPlot(values : Map<Long, List<Entry>>, graph : LineChart, first: Long) {
            // Maybe do this gracefully. Toast "can't show plot with one data point"
            //if(values.size < 2) return
            val sets = mutableListOf<LineDataSet>()
            var (minx, maxx) = Pair(1e18f, -1e18f)
            for ((_, li) in values) {
                sets.add(standardLineDataSet(li, false, ColorTemplate.getHoloBlue()))
                minx = Math.min(minx, li.first().x)
                maxx = Math.max(maxx, li.last().x)
            }
            // TODO: Move constants to user.
            val lo = mutableListOf<Entry>()
            lo.add(Entry(minx, 4f))
            lo.add(Entry(maxx, 4f))
            val hi = mutableListOf<Entry>()
            hi.add(Entry(minx, 8f))
            hi.add(Entry(maxx, 8f))
            sets.add(standardLineDataSet(lo, true, gray))
            sets.add(standardLineDataSet(hi, true, gray))

            graph.data = LineData(sets as List<ILineDataSet>?)
            graph.legend.isEnabled = false
            graph.description.text = ""
            graph.invalidate()

            val xAxis = graph.getXAxis()
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 12f
            xAxis.setDrawAxisLine(false)
            xAxis.setDrawGridLines(true)
            xAxis.textColor = hotPink
            xAxis.setLabelCount(5, true)
            xAxis.valueFormatter = object : IAxisValueFormatter {
                private val mCalendar = Calendar.getInstance()
                override fun getFormattedValue(value: Float, axis: AxisBase): String {
                    mCalendar.setTimeInMillis(value.toLong() + first)
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
            rightAxis.isEnabled = false
        }
    }
}