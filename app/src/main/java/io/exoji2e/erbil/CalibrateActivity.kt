package io.exoji2e.erbil

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_calibrate.*



class CalibrateActivity : AppCompatActivity() {

    private fun put(p : Pair<Double, Double>, t: TextView) {
        t.text = String.format("%.3f; %.5f", p.first, p.second)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibrate)
        val task = Runnable {
            val guess = DataContainer.getInstance(this).guess()
            if (guess == null) finish()
            val inst = SensorData.instance()
            val id = guess!!.sensorId
            val p = inst.recalibrate(id, this)
            put(inst.default, DefaultData)
            put(p, RecalibratedData)
            put(inst.get(id), CurrData)

            CurrFAB.setOnClickListener { _ ->
                finish()
            }
            DefaultFAB.setOnClickListener { _ ->
                inst.save(id, inst.default)
                finish()
            }
            RecalibratedFAB.setOnClickListener { _ ->
                inst.save(id, p)
                finish()
            }

        }
        DbWorkerThread.getInstance().postTask(task)
        // List all sensors (in a scrolled view?
        // make all sensors buttons
        // if button pressed launch a new activity with sensor, where it shows current, default and calibrated glucose curves.
    }

}
