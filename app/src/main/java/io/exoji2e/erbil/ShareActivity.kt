package io.exoji2e.erbil

import android.os.Bundle
import android.app.Activity

import kotlinx.android.synthetic.main.activity_share.*
import android.content.Intent
import android.view.View
import java.io.File
import android.support.v4.content.FileProvider
import android.util.Log


class ShareActivity : Activity() {
    private val TAG = "SHAREACTIVITY"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)
        shareButton.setOnClickListener{ v : View ->
            val path = getDatabasePath("glucose.db").getAbsolutePath()
            Log.d(TAG, path)
            val filesdir : String = this@ShareActivity.filesDir.absolutePath
            Log.d(TAG, filesdir)

            val file = File(path)
            try {
                val uri = FileProvider.getUriForFile(
                        this@ShareActivity,
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

}
