package io.exoji2e.erbil.activities

import android.os.Bundle
import android.widget.TextView
import io.exoji2e.erbil.DataContainer
import io.exoji2e.erbil.R
import io.exoji2e.erbil.SensorData
import io.exoji2e.erbil.database.DbWorkerThread
import kotlinx.android.synthetic.main.activity_calibrate.*



class CalibrateActivity : SimpleActivity() {
    override val TAG = "CalibrateActivity"
    private fun put(p : Pair<Double, Double>, t: TextView) {
        t.text = String.format("%.3f; %.5f", p.first, p.second)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibrate)
        val task = Runnable {
            val guess = DataContainer.getInstance(this).guess()
            if (guess == null) finish()
            val inst = SensorData.instance(this)
            val id = guess!!.first.sensorId
            val p = inst.recalibrate(id, this)
            put(inst.default, DefaultData)
            put(p, RecalibratedData)
            put(inst.get(id), CurrData)

            CurrFAB.setOnClickListener { _ ->
                finish()
            }
            DefaultFAB.setOnClickListener { _ ->
                val task = Runnable {
                    inst.save(id, inst.default)
                }
                DbWorkerThread.getInstance().postTask(task)
                finish()
            }
            RecalibratedFAB.setOnClickListener { _ ->
                val task = Runnable {
                    inst.save(id, p)
                }
                DbWorkerThread.getInstance().postTask(task)
                finish()
            }
        }
        DbWorkerThread.getInstance().postTask(task)
        // List all sensors (in a scrolled view?
        // make all sensors buttons
        // if button pressed launch a new activity with sensor, where it shows current, default and calibrated glucose curves.
    }

}
