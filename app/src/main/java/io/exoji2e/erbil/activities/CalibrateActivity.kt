package io.exoji2e.erbil.activities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.exoji2e.erbil.DataContainer
import io.exoji2e.erbil.R
import io.exoji2e.erbil.SensorData
import io.exoji2e.erbil.Time
import io.exoji2e.erbil.database.DbWorkerThread
import io.exoji2e.erbil.database.GlucoseEntry
import io.exoji2e.erbil.database.ManualGlucoseEntry
import kotlinx.android.synthetic.main.activity_calibrate.*
import kotlinx.android.synthetic.main.manual_checkbox_entry.view.*
import kotlinx.android.synthetic.main.save_calibration.view.*


class CalibrateActivity : SimpleActivity() {
    override val TAG = "CalibrateActivity"
    private fun put(p : Pair<Double, Double>, t: TextView) {
        t.text = String.format("%.3f; %.5f", p.first, p.second)
    }
    val s : MutableSet<Pair<ManualGlucoseEntry, GlucoseEntry>> = mutableSetOf()
    var id = 0L
    var P = Pair(0.0, 0.0)
    private fun recalib() {
        P = SensorData.recalibrate(s.toList())
        if(calibrated_save != null) {
            put(P, calibrated_save.data)
            calibrated_save.fab.setOnClickListener { _ ->
                val task = Runnable {
                    val inst = SensorData.instance(this)
                    inst.save(id, P)
                }
                DbWorkerThread.getInstance().postTask(task)
                finish()
            }
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibrate)
        val task = Runnable {
            val guess = DataContainer.getInstance(this).guess()
            try {
                id = guess!!.first.sensorId
            }catch(e:NullPointerException){
                Toast.makeText(this, "No sensor found to calibrate!", Toast.LENGTH_SHORT).show()
                finish()
                return@Runnable
            }
            val inst = SensorData.instance(this)
            val pairs = inst.get_calibration_pts(id, this).sortedBy{p -> -p.first.utcTimeStamp}.toMutableList()
            s.addAll(pairs)
            val adapter = ManualEntryAdapter(pairs)
            checkbox_list.post{checkbox_list.adapter = adapter}
            P = inst.recalibrate(id, this)

            current_save.post{
                current_save.text.text = "Current"
                put(inst.get(id), current_save.data)
                current_save.fab.setOnClickListener { _ ->
                    finish()
                }
            }
            default_save.post{
                default_save.text.text = "Default"
                put(SensorData.default, default_save.data)
                default_save.fab.setOnClickListener { _ ->
                    val task = Runnable {
                        inst.save(id, SensorData.default)
                    }
                    DbWorkerThread.getInstance().postTask(task)
                    finish()
                }
            }
            calibrated_save.post {
                calibrated_save.text.text = "Recalibrated"
                put(P, calibrated_save.data)
                calibrated_save.fab.setOnClickListener { _ ->
                    val task = Runnable {
                        inst.save(id, P)
                    }
                    DbWorkerThread.getInstance().postTask(task)
                    finish()
                }
            }
        }
        DbWorkerThread.getInstance().postTask(task)

        // List all sensors (in a scrolled view?
        // make all sensors buttons
        // if button pressed launch a new activity with sensor, where it shows current, default and calibrated glucose curves.
    }
    inner class ManualEntryAdapter(L : MutableList<Pair<ManualGlucoseEntry, GlucoseEntry>>):
            ArrayAdapter<Pair<ManualGlucoseEntry, GlucoseEntry>>(this, R.layout.manual_checkbox_entry, R.id.value, L) {
        val L = L.toList()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = if(convertView == null)
                    LayoutInflater.from(this@CalibrateActivity).inflate(R.layout.manual_checkbox_entry, parent, false);
                else
                    convertView

            val p = getItem(position)
            if(p==null)
                return view

            view.check.setOnClickListener { v  ->
                val c = v as CheckBox
                //isChecked has already changed
                if(c.isChecked){
                    s.add(p)
                } else{
                    s.remove(p)
                }
                recalib()
                refresh()
            }

            view.value.text = String.format("%.1f", p.first.value)
            view.calib_value.text = String.format("%.1f", SensorData.sensor2mmol(p.second.value, P))
            view.default_value.text = String.format("%.1f", SensorData.sensor2mmol(p.second.value))
            view.time.text = Time.datetime(p.first.utcTimeStamp)
            return view
        }
        fun refresh() {
            // Should find other way of reloading layout.
            clear()
            notifyDataSetChanged()
            addAll(L)
            notifyDataSetChanged()
        }
    }

}

