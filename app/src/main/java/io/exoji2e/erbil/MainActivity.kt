package io.exoji2e.erbil

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
            val end = Time.now()
            val start = end - durations[section]

            val task = Runnable {
                val dc = DataContainer.getInstance(context!!)
                val readings = dc.get(start, end)
                val avg = Compute.avg(readings)
                // TODO: Move constants to user.
                if (readings.size > 1) Push2Plot.setPlot(readings, graph, start, end)
                ingData.text = Compute.inGoal(4.0, 8.0, readings)
                avgData.text = String.format("%.1f", avg)
                readingsData.text = ErbilDataBase.getInstance(context!!)!!.sensorContactDao().getAll().filter { s -> s.utcTimeStamp in start..end }.size.toString()
            }
            DbWorkerThread.getInstance().postTask(task)
            return rootView
        }


        companion object {
            private val ARG_SECTION_NUMBER = "section_number"
            val durations = longArrayOf(Time.DAY, Time.DAY*7, Time.DAY*31)

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
