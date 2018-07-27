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
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.DateFormat
import java.util.*

class Push2Plot {
    companion object {
        private val hotPink = Color.rgb(255, 64, 129)
        fun standardLineDataSet(data : ArrayList<Entry>, dash : Boolean) : LineDataSet {
            val dataSet = LineDataSet(data,"")
            dataSet.axisDependency = YAxis.AxisDependency.LEFT
            dataSet.valueTextColor = ColorTemplate.getHoloBlue()
            if(dash) {
                dataSet.lineWidth = 1f
                dataSet.color = Color.rgb(100, 100, 100)
            }else {
                dataSet.lineWidth = 3f
                dataSet.color = ColorTemplate.getHoloBlue()
            }
            dataSet.setDrawCircles(false)
            dataSet.setDrawValues(false)
            dataSet.fillAlpha = 65
            dataSet.fillColor = ColorTemplate.getHoloBlue()
            dataSet.highLightColor = Color.rgb(244, 117, 117)
            dataSet.setDrawCircleHole(false)
            return dataSet
        }
        fun setPlot(values : ArrayList<Entry>, graph : LineChart, first: Long) {
            // Maybe do this gracefully. Toast "can't show plot with one data point"
            if(values.size < 2) return
            val dataSet = standardLineDataSet(values, false)
            // TODO: Move constants to user.
            val lo = ArrayList<Entry>(2)
            lo.add(Entry(values.first().x, 4f))
            lo.add(Entry(values.last().x, 4f))
            val hi = ArrayList<Entry>(2)
            hi.add(Entry(values.first().x, 8f))
            hi.add(Entry(values.last().x, 8f))

            graph.data = LineData(standardLineDataSet(lo, true), standardLineDataSet(hi, true), dataSet)
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