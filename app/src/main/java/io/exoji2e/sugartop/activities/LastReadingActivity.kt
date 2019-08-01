package io.exoji2e.sugartop.activities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import io.exoji2e.sugartop.DataContainer

import io.exoji2e.sugartop.R
import io.exoji2e.sugartop.database.DbWorkerThread
import kotlinx.android.synthetic.main.activity_lastreading.*
import kotlinx.android.synthetic.main.reading_entry.view.*


class LastReadingActivity : SimpleActivity() {
    override val TAG = "LastReadingActivity"
    var sz = 0
    private fun updateReadingList(i: Int) {
        val task = Runnable {
            status_text.post{status_text.text="%d/%d".format(i+1, sz)}
            Log.d(TAG, "Updating values")
            val raw_data = DataContainer.getInstance(this).get_raw_data(i)
            Log.d(TAG, raw_data.size.toString())
            val adapter = ManualEntryAdapter(raw_data.toMutableList())
            reading_list.post{
                reading_list.adapter = adapter
                adapter.refresh()
            }
            if(i < sz - 1)
                forward_button.setOnClickListener{
                    updateReadingList(i + 1)
                }
            if(i > 0)
                back_button.setOnClickListener{
                    updateReadingList(i-1)
                }
        }
        DbWorkerThread.getInstance().postTask(task)

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lastreading)
        listhead.a.text="id"
        listhead.b.text="value"
        listhead.c.text="comment"

        DbWorkerThread.getInstance().postTask(Runnable{
            sz = DataContainer.getInstance(this).get_sz_raw_data()
            updateReadingList(sz - 1)
        })

    }
    inner class ManualEntryAdapter(L : MutableList<Byte>):
            ArrayAdapter<Byte>(this, R.layout.manual_checkbox_entry, R.id.value, L) {
        val L = L.toList()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = if (convertView == null)
                LayoutInflater.from(this@LastReadingActivity).inflate(R.layout.reading_entry, parent, false);
            else
                convertView

            val p = getItem(position)
            if (p == null)
                return view

            view.a.text = position.toString()
            view.b.text = ((p + 256) % 256).toString()
            val comment =
                if(24 <= position && position <= 25 )
                    "Dynamic"
                else if(5 <= position && position <= 23)
                    "Zero"
                else if(position == 318 || position == 319)
                    "Zero"
                else if(328 <= position)
                    "Zero"
                else if(position == 26)
                    "Recent start"
                else if(position == 27)
                    "History start"
                else if(position >= 28 && position < 28 + 6*16) {
                    val dt = (position - 28) / 6
                    String.format("Recent %d", dt)
                }else if(position >= 124 && position < 124 + 6*32) {
                    val dt = (position - 124) / 6
                    String.format("History %d", dt)
                }
                else if (position <= 317 && position >= 316)
                    "Time stamp"
                else ""

            view.c.text = comment
            return view
        }
        fun refresh() {
            clear()
            notifyDataSetChanged()
            addAll(L)
            notifyDataSetChanged()
        }
    }

}

