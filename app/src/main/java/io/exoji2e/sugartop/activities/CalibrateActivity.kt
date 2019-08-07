package io.exoji2e.sugartop.activities

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import io.exoji2e.sugartop.DataContainer
import io.exoji2e.sugartop.R
import io.exoji2e.sugartop.SensorData
import io.exoji2e.sugartop.Time
import io.exoji2e.sugartop.database.DbWorkerThread
import io.exoji2e.sugartop.database.GlucoseEntry
import io.exoji2e.sugartop.database.ManualGlucoseEntry
import io.exoji2e.sugartop.settings.UserData
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
    var DT = 5L

    private fun recalib() {
        P = SensorData.recalibrate(s.toList())
        if(calibrated_save != null) {
            put(P, calibrated_save.data)
            calibrated_save.fab.setOnClickListener { _ ->
                val task = Runnable {
                    val inst = SensorData.instance(this)
                    inst.save(id, P)
                }
                DbWorkerThread.postTask(task)
                finish()
            }
        }
    }
    private fun updateCheckboxList() {
        val task = Runnable {
            Log.d(TAG, "Updating checkboxes")
            Log.d(TAG, DT.toString())
            val guess = DataContainer.getInstance(this).guess()
            try {
                id = guess!!.first.sensorId
            }catch(e:NullPointerException){
                Toast.makeText(this, "No sensor found to calibrate!", Toast.LENGTH_SHORT).show()
                finish()
                return@Runnable
            }
            val inst = SensorData.instance(this)
            P = inst.recalibrate(id, this)
            val pairs = inst.get_calibration_pts(id, this, Time.MINUTE*DT).sortedBy{p -> -p.first.utcTimeStamp}.toMutableList()
            s.clear()
            s.addAll(pairs)
            val adapter = ManualEntryAdapter(pairs)
            checkbox_list.post{
                checkbox_list.adapter = adapter
                adapter.refresh()
            }


            calibrated_save.post {
                calibrated_save.text.text = "Recalibrated"
                put(P, calibrated_save.data)
                calibrated_save.fab.setOnClickListener { _ ->
                    val task = Runnable {
                        inst.save(id, P)
                    }
                    DbWorkerThread.postTask(task)
                    finish()
                }
            }
        }
        DbWorkerThread.postTask(task)

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibrate)
        listhead.value.text="Real"
        listhead.calib_value.text="Re"
        listhead.default_value.text="Def"
        listhead.time.text = "Time"
        minutes.setOnFocusChangeListener { _, b ->
            if (!b) {
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(minutes.windowToken, 0)
                val v = minutes.text.toString().toDoubleOrNull()
                if (v != null){
                    val vL = v.toLong()
                    if (vL > 0 && vL < 60) {
                        DT = vL
                        updateCheckboxList()
                    }
                    minutes.text.replace(0, minutes.text.length, DT.toString())
                }
            }
        }
        current_save.post{
            current_save.text.text = "Current"
            val task = Runnable {
                val inst = SensorData.instance(this)
                put(inst.get(id), current_save.data)
                current_save.fab.setOnClickListener {
                    finish()
                }
            }
            DbWorkerThread.postTask(task)
        }
        default_save.post{
            default_save.text.text = "Default"
            put(SensorData.default, default_save.data)
            default_save.fab.setOnClickListener {
                val task = Runnable {
                    val inst = SensorData.instance(this)
                    inst.save(id, SensorData.default)
                }
                DbWorkerThread.postTask(task)
                finish()
            }
        }
        updateCheckboxList()
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
            val sd = SensorData.instance(this@CalibrateActivity)
            view.value.text = String.format("%.1f", p.first.value * UserData.get_multiplier(this@CalibrateActivity))
            view.calib_value.text = String.format("%.1f", sd.sensor2unit(p.second.value, P))
            view.default_value.text = String.format("%.1f", sd.sensor2unit(p.second.value))
            view.time.text = Time.datetime(p.first.utcTimeStamp).replace('T', ' ')
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

