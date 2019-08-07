package io.exoji2e.sugartop

import android.view.View
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import io.exoji2e.sugartop.database.GlucoseEntry
import io.exoji2e.sugartop.database.GlucoseReading
import io.exoji2e.sugartop.database.ManualGlucoseEntry
import kotlinx.android.synthetic.main.element_data_row.view.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class Push2Plot {
    enum class PlotType{
        RECENT,
        DAY,
        WEEK,
        MONTH
    }
    companion object {
        fun place_data_in_view(v: View?, description: String, data: String, unit: String) {
            if(v != null)
                v.post {
                    v.description.text = description
                    v.data.text = data
                    v.unit.text = unit
                }
        }

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
        fun setPlot(entries : List<GlucoseEntry>, manual : List<ManualGlucoseEntry>, graph : CombinedChart?, start: Long, end : Long, sd : SensorData, type : PlotType, thresholds: Pair<Float, Float>) {
            _setPlot(entries.map{g -> g.toReading()}, manual, graph, start, end, sd, type, thresholds)
        }

        fun scatter(data : List<Entry>) : ScatterDataSet {
            val dataSet = ScatterDataSet(data, "")
            dataSet.color = Color.hotPink
            dataSet.axisDependency = YAxis.AxisDependency.LEFT
            return dataSet
        }
        fun get_value_fomatter(t : PlotType, start: Long): IAxisValueFormatter? {
            return if(t == PlotType.RECENT || t == PlotType.DAY) {
                object : IAxisValueFormatter {
                    private val mCalendar = Calendar.getInstance()
                    val TimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                    override fun getFormattedValue(value: Float, axis: AxisBase): String {
                        mCalendar.timeInMillis = value.toLong() + start + Time.SECOND*10
                        return TimeFormatter.format(mCalendar.timeInMillis)
                    }
                }
            } else if(t == PlotType.WEEK) {
                object : IAxisValueFormatter {
                    private val mCalendar = Calendar.getInstance()
                    val WeekDayTimeFormatter = SimpleDateFormat("EEE", Locale.getDefault())
                    override fun getFormattedValue(value: Float, axis: AxisBase): String {
                        mCalendar.timeInMillis = value.toLong() + start + Time.HOUR
                        return WeekDayTimeFormatter.format(mCalendar.timeInMillis)
                    }
                }
            }
            else {
                object : IAxisValueFormatter {
                    private val mCalendar = Calendar.getInstance()
                    val DateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
                    override fun getFormattedValue(value: Float, axis: AxisBase): String {
                        mCalendar.timeInMillis = value.toLong() + start + Time.HOUR
                        return DateFormatter.format(mCalendar.timeInMillis)
                    }
                }
            }
        }
        private fun fixXaxis(xAxis: XAxis, t : PlotType, start: Long, end: Long){
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 12f
            xAxis.setDrawAxisLine(false)
            xAxis.setDrawGridLines(true)
            xAxis.textColor = Color.black
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = (end-start).toFloat()
            xAxis.valueFormatter = get_value_fomatter(t, start)
            if(t == PlotType.RECENT){
                xAxis.setLabelCount(9, true)
                val now = (Time.now() - start).toFloat()
                val L = LimitLine(now, xAxis.valueFormatter.getFormattedValue(now, xAxis))
                L.labelPosition = LimitLine.LimitLabelPosition.LEFT_TOP
                xAxis.removeAllLimitLines()
                xAxis.addLimitLine(L)
            } else if(t == PlotType.DAY){
                xAxis.setLabelCount(7, true)
            } else if(t == PlotType.WEEK){
                xAxis.setLabelCount(8, true)
            } else if(t == PlotType.MONTH){
                xAxis.setLabelCount(6, true)
            }
        }

        fun _setPlot(entries : List<GlucoseReading>, manual : List<ManualGlucoseEntry>, graph : CombinedChart?, start: Long, end : Long, sd : SensorData, type: PlotType, thresholds: Pair<Float, Float>) {
            if(graph == null) return
            val task = {
                val values = entries
                        .groupBy { g -> g.sensorId }
                val sets = mutableListOf<LineDataSet>()
                val multiplier = sd.get_multiplier()
                var max = 17f * multiplier
                var min = 2f * multiplier
                for ((c, li) in values.toList().withIndex()) {
                    val gr = li.second
                    val out = arrayListOf<Entry>()
                    for (i in 0 until gr.size) {
                        val th = gr[i]
                        if (i > 0 && gr[i - 1].utcTimeStamp + Time.MINUTE > th.utcTimeStamp) continue
                        if (th.status == 200 || th.value < 5000) //5000 corresponds to 28 mmol/L with default calibration.
                            out.add(Entry((th.utcTimeStamp - start).toFloat(), th.tounit(sd, multiplier).toFloat()))
                    }
                    val vmax = out.maxBy { v -> v.y }?.y
                    val vmin = out.minBy { v -> v.y }?.y
                    if (vmax != null) max = Math.max(max, vmax)
                    if (vmin != null) min = Math.min(min, vmin)
                    sets.add(standardLineDataSet(out, false, Color.lineColor(c)))
                }

                val scatter = ScatterData(scatter(manual.map { m -> Entry((m.utcTimeStamp - start).toFloat(), m.value.toFloat() * multiplier) }))
                val lo = arrayListOf(Entry(0f, thresholds.first), Entry((end - start).toFloat(), thresholds.first))
                val hi = arrayListOf(Entry(0f, thresholds.second), Entry((end - start).toFloat(), thresholds.second))
                sets.add(standardLineDataSet(lo, true, Color.gray))
                sets.add(standardLineDataSet(hi, true, Color.gray))
                val combData = CombinedData()
                combData.setData(LineData(sets as List<ILineDataSet>?))
                if (manual.isNotEmpty()) {
                    combData.setData(scatter)
                    val mmin = manual.minBy { m -> m.value }?.value?.toFloat()
                    val mmax = manual.maxBy { m -> m.value }?.value?.toFloat()
                    if (mmin != null) min = Math.min(min, mmin * multiplier)
                    if (mmax != null) max = Math.max(max, mmax * multiplier)
                }
                graph.data = combData
                graph.legend.isEnabled = false
                graph.description.text = ""
                fixXaxis(graph.xAxis, type, start, end)

                val leftAxis = graph.axisLeft
                leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
                leftAxis.setDrawGridLines(true)
                leftAxis.granularity = 4f * multiplier
                leftAxis.axisMinimum = Math.max(min, 0f)
                leftAxis.axisMaximum = Math.min(max, 25f * multiplier)
                leftAxis.textSize = 12f
                leftAxis.textColor = Color.black

                graph.axisRight.isEnabled = false
                graph.invalidate()

            }
            graph.post(task)
        }
    }
}