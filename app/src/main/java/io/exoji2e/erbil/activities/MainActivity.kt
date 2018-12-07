package io.exoji2e.erbil.activities

import android.os.Bundle
import android.support.v4.app.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.exoji2e.erbil.*
import io.exoji2e.erbil.settings.UserData
import io.exoji2e.erbil.database.DbWorkerThread
import io.exoji2e.erbil.database.ErbilDataBase
import kotlinx.android.synthetic.main.dashboard_layout.*
import kotlinx.android.synthetic.main.fragment_dashboard.*


class MainActivity : ErbilActivity() {
    override val TAG = "MAIN-Activity"
    override fun getNavigationMenuItemId() = R.id.action_dashboard
    override fun getContentViewId() = R.layout.dashboard_layout
    val titles = arrayOf("Day", "Week", "Month")
    private lateinit var mSectionsPagerAdapter: SectionsPagerAdapter

    override fun onResume() {
        super.onResume()
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        container.adapter = mSectionsPagerAdapter
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return PlaceholderFragment.newInstance(position)
        }

        override fun getCount(): Int {
            return titles.size
        }
        override fun getPageTitle(position: Int) : CharSequence {
            return titles[position]
        }
    }

    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_dashboard, container, false)
            val section = (if(arguments != null) arguments!!.getInt(ARG_SECTION_NUMBER) else 0)

            val now = Time.now()
            val end = if(section == 0) Time.floor_hour(now) + Time.HOUR else Time.floor_day(now) + Time.DAY
            val start = end - durations[section]

            val task = Runnable {
                val dc = DataContainer.getInstance(context!!)
                val readings = dc.get(start, end)
                val sd = SensorData.instance(context!!)
                val avg = Compute.avg(readings, sd)
                val thresholds = Pair(UserData.get_low_threshold(context!!), UserData.get_hi_threshold(context!!))
                val manual = ErbilDataBase.getInstance(context!!).manualEntryDao().getAll().filter{ entry -> entry.utcTimeStamp > start && entry.utcTimeStamp < end}
                val readdata = ErbilDataBase.getInstance(context!!).sensorContactDao().getAll().filter { s -> s.utcTimeStamp in start..end }.size.toString()
                val lowdata = String.format("%d", Compute.occurrencesBelow(thresholds.first, thresholds.first+1f, readings, sd))
                Push2Plot.setPlot(readings, manual, graph, start, end, sd, plotTypes[section], thresholds)
                Push2Plot.place_data_in_view(avg_elem, "Average:", String.format("%.1f", avg), "mmol/L")
                Push2Plot.place_data_in_view(in_elem, "Inside Target:", Compute.inGoal(thresholds.first, thresholds.second, readings, sd), "")
                Push2Plot.place_data_in_view(read_elem, "Readings:", readdata, "")
                Push2Plot.place_data_in_view(low_elem, "Low Occurrences:", lowdata, "")

            }
            DbWorkerThread.getInstance().postTask(task)
            return rootView
        }


        companion object {
            private val ARG_SECTION_NUMBER = "section_number"
            val durations = longArrayOf(Time.DAY, Time.DAY * 7, Time.DAY *30)
            val plotTypes = arrayOf(Push2Plot.PlotType.DAY, Push2Plot.PlotType.WEEK, Push2Plot.PlotType.MONTH)

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
