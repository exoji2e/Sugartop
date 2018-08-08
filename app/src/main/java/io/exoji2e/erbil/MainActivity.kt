package io.exoji2e.erbil

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.util.Log
import com.github.mikephil.charting.data.Entry
import java.util.ArrayList
import kotlinx.android.synthetic.main.main_layout.*
import java.io.File


class MainActivity : AppCompatActivity() {
    val TAG = "MAIN"
    val dc : DataContainer = DataContainer.getInstance(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        startReadActivity()
        floatingButton.setOnClickListener { _ ->
            val path = getDatabasePath("Erbil.db").getAbsolutePath()
            Log.d(TAG, path)
            val filesdir: String = this@MainActivity.filesDir.absolutePath
            Log.d(TAG, filesdir)

            val file = File(path)
            try {
                val uri = FileProvider.getUriForFile(
                        this@MainActivity,
                        "io.exoji2e.erbil.fileprovider",
                        file)
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.setType("application/octet-stream");
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
                sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                startActivity(Intent.createChooser(sharingIntent, "Share using"))

            } catch (e: IllegalArgumentException) {
                Log.e(TAG,
                        "The selected file can't be shared: $path")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val readings = dc.get24h()
        val avg = Compute.avg(readings)
        // TODO: Move constants to user.
        val percentInside = Compute.inGoal(4.0, 8.0, readings)*100
        val values = ArrayList<Entry>()
        val first : Long = if (readings.isEmpty()) 0L else readings[0].utcTimeStamp
        val recent : Double = if (readings.isEmpty()) 0.0 else readings.last().tommol()
        values.addAll(readings.map{r -> Entry((r.utcTimeStamp - first).toFloat(), r.tommol().toFloat()) })
        if (readings.size > 1) Push2Plot.setPlot(values, graph, first)
        ingData.text = String.format("%.1f %s", percentInside, "%")
        avgData.text = String.format("%.1f", avg)
        recentData.text = String.format("%.1f", recent)
    }

    fun startReadActivity() {
        val intent = Intent(this, ReadActivity::class.java)
        startActivity(intent)
    }
}
