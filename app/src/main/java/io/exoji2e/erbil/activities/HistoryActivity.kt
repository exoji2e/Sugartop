package io.exoji2e.erbil.activities

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.os.Bundle
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.exoji2e.erbil.*
import io.exoji2e.erbil.database.DbWorkerThread
import io.exoji2e.erbil.database.ErbilDataBase
import io.exoji2e.erbil.settings.UserData
import kotlinx.android.synthetic.main.dashboard_layout.*
import kotlinx.android.synthetic.main.fragment_dashboard.*

class HistoryActivity : ErbilActivity() {
    var NUM_ITEMS = Time.date_as_int()
    override val TAG = "HISTORY"

    private lateinit var mSectionsPagerAdapter: SectionsPagerAdapter

    override fun getContentViewId(): Int = R.layout.dashboard_layout
    override fun getNavigationMenuItemId() = R.id.action_history


    override fun onResume() {
        super.onResume()
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        NUM_ITEMS = Time.date_as_int()
        container.adapter = mSectionsPagerAdapter
        container.setCurrentItem(NUM_ITEMS - 1, false)
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return PlaceholderFragment.newInstance(position)
        }

        override fun getCount(): Int {
            return NUM_ITEMS
        }
        override fun getPageTitle(position: Int) : CharSequence {
            val day = Time.date_as_int() - 1 - position
            return Time.date_as_string(day)
        }

    }

    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_dashboard, container, false)
            val day = Time.date_as_int() - 1 - (if(arguments != null) arguments!!.getInt(ARG_SECTION_NUMBER) else Time.date_as_int() - 1)

            val (start, end) = Time.limits(day)
            val task = Runnable {
                val dc = DataContainer.getInstance(context!!)
                val readings = dc.get(start, end)
                val sd = SensorData.instance(context!!)
                val avg = Compute.avg(readings, sd)
                val stddev = Compute.stddev(readings, sd)

                val thresholds = Pair(UserData.get_low_threshold(context!!), UserData.get_hi_threshold(context!!))
                val manual = ErbilDataBase.getInstance(context!!).manualEntryDao().getAll().filter{ entry -> entry.utcTimeStamp > start && entry.utcTimeStamp < end}
                val readdata = ErbilDataBase.getInstance(context!!).sensorContactDao().getAll().filter { s -> s.utcTimeStamp in start..end }.size.toString()
                val lowdata = String.format("%d", Compute.occurrencesBelow(thresholds.first, thresholds.first+1f, readings, sd))
                Push2Plot.setPlot(readings, manual, graph, start, end, sd, Push2Plot.PlotType.DAY, thresholds)
                Push2Plot.place_data_in_view(avg_elem, "avg:", String.format("%.1f", avg), "mmol/L")
                Push2Plot.place_data_in_view(std_dev_elem, "stddev:", String.format("%.1f", stddev), "mmol/L")
                Push2Plot.place_data_in_view(in_elem, "in target:", Compute.inGoal(thresholds.first, thresholds.second, readings, sd), "")
                Push2Plot.place_data_in_view(read_elem, "#readings:", readdata, "")
                Push2Plot.place_data_in_view(low_elem, "#lows:", lowdata, "")
            }

            DbWorkerThread.getInstance().postTask(task)
            return rootView
        }


        companion object {
            private val ARG_SECTION_NUMBER = "section_number"

            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }
}
