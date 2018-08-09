package io.exoji2e.erbil

import android.content.Intent
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import java.io.File

abstract class ErbilActivity : AppCompatActivity() {
    abstract val TAG : String
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.constant_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.getItemId()) {
            R.id.share_db -> {
                shareDB()
                return true
            }
            R.id.manual -> {
                launchManual()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
    fun shareDB() {
        val path = getDatabasePath("Erbil.db").getAbsolutePath()
        Log.d(TAG, path)
        val filesdir: String = this@ErbilActivity.filesDir.absolutePath
        Log.d(TAG, filesdir)

        val file = File(path)
        try {
            val uri = FileProvider.getUriForFile(
                    this@ErbilActivity,
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
    private fun launchManual() {
        val intent = Intent(this, ManualActivity::class.java)
        startActivity(intent)
    }
}
