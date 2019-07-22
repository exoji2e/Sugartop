package io.exoji2e.erbil.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_manual.*
import android.view.inputmethod.InputMethodManager
import io.exoji2e.erbil.DataContainer
import io.exoji2e.erbil.R
import io.exoji2e.erbil.Time
import io.exoji2e.erbil.database.DbWorkerThread
import io.exoji2e.erbil.database.ManualGlucoseEntry

class ManualActivity : SimpleActivity() {
    override val TAG = "ManualActivity"
    lateinit var imm : InputMethodManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        glucoseinput.requestFocus()

        fab.setOnClickListener { _ ->
            imm.hideSoftInputFromWindow(glucoseinput.windowToken, 0)
            val v = glucoseinput.text.toString().toDoubleOrNull()
            if(v!=null && v > 1.0 && v < 25.0){
                val entry = ManualGlucoseEntry(Time.now(), v)
                DbWorkerThread.getInstance().postTask(Runnable{
                    DataContainer.getInstance(this).insertManual(entry)
                })
                val str = String.format("Saved measurement: %.1f mmol/L", v)
                setResult(Activity.RESULT_OK,
                        Intent().putExtra("text", str).putExtra("value", entry.value).putExtra("time", entry.utcTimeStamp))
                finish()
            } else {
                Toast.makeText(this, "Value not inside range [1, 25]", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        imm.hideSoftInputFromWindow(glucoseinput.windowToken, 0)
    }
}
