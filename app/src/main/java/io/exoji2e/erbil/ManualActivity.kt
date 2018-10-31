package io.exoji2e.erbil

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_manual.*
import android.view.inputmethod.InputMethodManager

class ManualActivity : SimpleActivity() {
    override val TAG = "ManualActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)

        fab.setOnClickListener { _ ->
            imm.hideSoftInputFromWindow(glucoseinput.windowToken, 0)
            val v = glucoseinput.text.toString().toDoubleOrNull()
            if(v!=null && v > 1.0 && v < 25.0){
                val entry = ManualGlucoseEntry(Time.now(), v)
                DbWorkerThread.getInstance().postTask(Runnable{
                    DataContainer.getInstance(this).insertIntoDb(entry)
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
}
