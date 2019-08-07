package io.exoji2e.sugartop.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_manual.*
import android.view.inputmethod.InputMethodManager
import io.exoji2e.sugartop.DataContainer
import io.exoji2e.sugartop.R
import io.exoji2e.sugartop.Time
import io.exoji2e.sugartop.database.DbWorkerThread
import io.exoji2e.sugartop.database.ManualGlucoseEntry
import io.exoji2e.sugartop.settings.UserData

class ManualActivity : SimpleActivity() {
    override val TAG = "ManualActivity"
    lateinit var imm : InputMethodManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        glucoseinput.requestFocus()
        unit.text = UserData.get_unit(this)

        fab.setOnClickListener { _ ->
            imm.hideSoftInputFromWindow(glucoseinput.windowToken, 0)
            val v = glucoseinput.text.toString().toDoubleOrNull()
            if(v!=null){
                val v_mmol = v/UserData.get_multiplier(this)
                if(v_mmol > 1.0 && v_mmol < 25.0){
                    val entry = ManualGlucoseEntry(Time.now(), v_mmol)
                    DbWorkerThread.postTask(Runnable{
                        DataContainer.getInstance(this).insertManual(entry)
                    })
                    val str = String.format("Saved measurement: %.1f %s", v, UserData.get_unit(this))
                    setResult(Activity.RESULT_OK,
                            Intent().putExtra("text", str).putExtra("value", entry.value).putExtra("time", entry.utcTimeStamp))
                    finish()
                } else {
                    val mul = UserData.get_multiplier(this).toInt()
                    Toast.makeText(this, "Value not inside range [%d, %d]".format(1*mul, 25*mul), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        imm.hideSoftInputFromWindow(glucoseinput.windowToken, 0)
    }
}
