package io.exoji2e.erbil

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.os.Bundle
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.fragment_history.*

class HistoryActivity : ErbilActivity() {
    var NUM_ITEMS = Time.date_as_int()
    override val TAG = "HISTORY"

    private lateinit var mSectionsPagerAdapter: SectionsPagerAdapter

    override fun getContentViewId(): Int = R.layout.activity_history
    override fun getNavigationMenuItemId() = R.id.action_history


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)


        // Set up the ViewPager with the sections adapter.
        container.adapter = mSectionsPagerAdapter

        container.setCurrentItem(NUM_ITEMS - 1, false)

    }



    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
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
            val rootView = inflater.inflate(R.layout.fragment_history, container, false)
            val day = Time.date_as_int() - 1 - (if(arguments != null) arguments!!.getInt(ARG_SECTION_NUMBER) else Time.date_as_int() - 1)

            val (start, end) = Time.limits(day)
            val task = Runnable {
                try {
                    val dc = DataContainer.getInstance(context!!)
                    val readings = dc.get(start, end)
                    Push2Plot.setPlot(readings, graph, start, end)
                } catch(e: Exception) {}

            }
            DbWorkerThread.getInstance().postTask(task)
            return rootView
        }


        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
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
