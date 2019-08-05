package io.exoji2e.sugartop.activities

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.os.Bundle
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.exoji2e.sugartop.*
import io.exoji2e.sugartop.database.DbWorkerThread
import kotlinx.android.synthetic.main.dashboard_layout.*
import android.app.DatePickerDialog
import java.util.Calendar


class HistoryActivity : BaseActivity() {
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
        val myCalendar = Calendar.getInstance()
        val date = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            container.setCurrentItem(NUM_ITEMS - 1 - Time.days_since(year, monthOfYear, dayOfMonth) , false)
        }
        pager_title_strip.setOnClickListener{
                DatePickerDialog(this@HistoryActivity, date, myCalendar
                    .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }


    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return HistoryFragment.newInstance(position)
        }

        override fun getCount(): Int {
            return NUM_ITEMS
        }
        override fun getPageTitle(position: Int) : CharSequence {
            val day = Time.date_as_int() - 1 - position
            return Time.date_as_string(day)
        }

    }
    class HistoryFragment : DashboardFragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_dashboard, container, false)
            val day = Time.date_as_int() - 1 - (if(arguments != null) arguments!!.getInt(HistoryFragment.ARG_SECTION_NUMBER) else Time.date_as_int() - 1)

            val (start, end) = Time.limits(day)
            if(context!=null) {
                val task = generate_task(start, end, context!!, Push2Plot.PlotType.DAY)
                DbWorkerThread.postTask(task)
            }
            return rootView
        }

        companion object {
            private val ARG_SECTION_NUMBER = "section_number"

            fun newInstance(sectionNumber: Int): DashboardFragment {
                val fragment = HistoryFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }


}
