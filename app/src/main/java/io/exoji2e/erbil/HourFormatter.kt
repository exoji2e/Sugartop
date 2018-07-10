package io.exoji2e.erbil

import android.content.Context
import com.jjoe64.graphview.DefaultLabelFormatter
import java.text.DateFormat
import java.util.*

class HourFormatter : DefaultLabelFormatter() {
    val mCalendar = Calendar.getInstance();
    override fun formatLabel(value : Double, isValueX: Boolean) : String {
        if (isValueX) {
            // format as date
            mCalendar.setTimeInMillis(value.toLong());
            val hhmm = DateFormat.getTimeInstance(DateFormat.SHORT).format(mCalendar.getTimeInMillis());
            return hhmm.substring(0, 2)
        } else {
            return super.formatLabel(value, isValueX);
        }
    }
}