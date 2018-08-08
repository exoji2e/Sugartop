package io.exoji2e.erbil

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_maual.*
import kotlinx.android.synthetic.main.content_maual.*

class ManualActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maual)

        fab.setOnClickListener { _ ->
            val v = glucoseinput.text.toString().toDoubleOrNull()
            if(v!=null && v > 1.0 && v < 25.0){
                val entry = ManualGlucoseEntry(Time.now(), v)
                DataContainer.getInstance(this).insertIntoDb(entry)
                Toast.makeText(this, String.format("inserted %.1f into database", v), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Value not inside range [1, 25]", Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

}
