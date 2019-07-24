package io.exoji2e.erbil.activities

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.exoji2e.erbil.*
import io.exoji2e.erbil.database.ErbilDataBase
import io.exoji2e.erbil.settings.UserData
import kotlinx.android.synthetic.main.fragment_dashboard.*

abstract class DashboardFragment : Fragment() {

    fun generate_task(start: Long, end: Long, ctx: Context, ptype : Push2Plot.PlotType) : Runnable {
        return Runnable {
            try {
                val dc = DataContainer.getInstance(ctx)
                val readings = dc.get(start, end)
                val sd = SensorData.instance(ctx)
                val avg = Compute.avg(readings, sd)
                val stddev = Compute.stddev(readings, sd)

                val thresholds = Pair(UserData.get_low_threshold(ctx), UserData.get_hi_threshold(ctx))
                val manual = ErbilDataBase.getInstance(ctx).manualEntryDao().getAll().filter { entry -> entry.utcTimeStamp > start && entry.utcTimeStamp < end }
                val readdata = ErbilDataBase.getInstance(ctx).sensorContactDao().getAll().filter { s -> s.utcTimeStamp in start..end }.size.toString()
                val lowdata = String.format("%d", Compute.occurrencesBelow(thresholds.first, readings, sd))
                Push2Plot.setPlot(readings, manual, graph, start, end, sd, ptype, thresholds)
                Push2Plot.place_data_in_view(avg_elem, "avg:", String.format("%.1f", avg), "mmol/L")
                Push2Plot.place_data_in_view(std_dev_elem, "stddev:", String.format("%.1f", stddev), "mmol/L")
                Push2Plot.place_data_in_view(in_elem, "in target:", Compute.inGoal(thresholds.first, thresholds.second, readings, sd), "")
                Push2Plot.place_data_in_view(read_elem, "#readings:", readdata, "")
                Push2Plot.place_data_in_view(low_elem, "#lows:", lowdata, "")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_dashboard, container, false)
        return rootView
    }
}