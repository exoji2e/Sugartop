package io.exoji2e.sugartop.activities

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import android.os.*
import android.support.design.widget.Snackbar
import android.support.v4.app.ShareCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import io.exoji2e.sugartop.*
import io.exoji2e.sugartop.database.DbWorkerThread
import io.exoji2e.sugartop.database.GlucoseDataBase
import io.exoji2e.sugartop.database.GlucoseEntry
import io.exoji2e.sugartop.database.ManualGlucoseEntry
import java.io.*
import kotlinx.android.synthetic.main.element_bottom_navigation.*
import java.util.*
import java.nio.file.Files.size
import android.R.attr.src
import android.net.Uri
import android.provider.ContactsContract
import io.exoji2e.sugartop.settings.UserData
import java.nio.channels.Channels


abstract class BaseActivity  : AppCompatActivity(), NfcAdapter.ReaderCallback  {
    abstract val TAG : String
    private var nfcAdapter: NfcAdapter? = null
    private val REQUEST_MANUAL = 0
    internal abstract fun getNavigationMenuItemId(): Int
    internal abstract fun getContentViewId(): Int

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContentView(getContentViewId());
        navigation.setOnNavigationItemSelectedListener {item ->
                when(item.itemId) {
                    R.id.action_dashboard -> {
                        putOnTop(MainActivity::class.java)
                        true
                    }
                    R.id.action_recent -> {
                        putOnTop(RecentActivity::class.java)
                        true
                    }
                    R.id.action_history ->{
                        putOnTop(HistoryActivity::class.java)
                        true
                    }
                    else -> {
                        true
                    }
                }
        }
        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val action = intent.action
        if(action == Intent.ACTION_SEND) {

            val mime = get_mime()
            Log.d("MIME-TYPE", mime)
            if(mime == "application/octet-stream") {
                val task = Runnable {
                    DataContainer.destroyInstance()
                    val intentReader = ShareCompat.IntentReader.from(this)
                    val uri = intentReader.stream
                    val db_name = Time.datetime() + "-Sugartop.db"
                    val path = getDatabasePath(db_name).absolutePath
                    val inStream = contentResolver.openInputStream(uri)
                    val outStream = FileOutputStream(path)
                    val inChannel = Channels.newChannel(inStream)
                    val outChannel = outStream.channel
                    try {
                        outChannel.transferFrom(inChannel, 0, Long.MAX_VALUE)
                    } catch(e: Exception) {
                    }
                    inStream?.close()
                    outStream.close()
                    val old_db_name = UserData.get_db_path(this)
                    UserData.set_db_path(this, db_name)
                    try {
                        val dc = DataContainer.getInstance(this)
                        val x = dc.get(0, 0)
                        Log.d(TAG, x.toString())
                        Toast.makeText(this, "Import successful!", Toast.LENGTH_LONG).show()
                    } catch(e: Exception) {
                        DataContainer.destroyInstance()
                        UserData.set_db_path(this, old_db_name)
                        Toast.makeText(this, "Import failed!", Toast.LENGTH_LONG).show()
                    }

                    putOnTop(MainActivity::class.java)

                }
                DbWorkerThread.postTask(task)

            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null)    }
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this);
    }

    override fun onTagDiscovered(tag: Tag) {
        vibrate(100L, false)
        val data = read_nfc_data(tag)
        if(data!=null) {
            vibrate(100L, true)
            val tagId = RawParser.bin2long(tag.id)
            val now = Time.now()
            val task = Runnable {
                val dc = DataContainer.getInstance(this@BaseActivity)
                dc.append(data, now, tagId)
            }
            DbWorkerThread.postTask(task)
            putOnTop(RecentActivity::class.java)
        } else {
            vibrate(300L, false)
        }
    }
    fun putOnTop(cls: Class<*>) {
        val reopen = Intent(this, cls)
        reopen.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivityIfNeeded(reopen, 0)
    }
    private fun startActivity(cls: Class<*>) {
        val intent = Intent(this, cls)
        startActivity(intent)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.constant_menu, menu)
        return true
    }

    override fun onStart() {
        super.onStart()
        updateNavigationBarState()
    }

    private fun updateNavigationBarState() {
        val actionId = getNavigationMenuItemId()
        selectBottomNavigationBarItem(actionId)
    }

    private fun selectBottomNavigationBarItem(itemId: Int) {
        val item = navigation.menu.findItem(itemId)
        item.isChecked = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share_db -> {
                shareDB()
                return true
            }
            R.id.manual -> {
                val intent = Intent(this, ManualActivity::class.java)
                startActivityForResult(intent, REQUEST_MANUAL)
                return true
            }
            R.id.share_csv -> {
                shareAsCSV()
                return true
            }
            R.id.calibrate -> {
                startActivity(CalibrateActivity::class.java)
                return true
            }
            R.id.settings -> {
                startActivity(SettingsActivity::class.java)
                return true
            }
            R.id.raw_reading -> {
                startActivity(LastReadingActivity::class.java)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun shareDB() {
        val path = getDatabasePath(UserData.get_db_path(this)).getAbsolutePath()
        share_file(path)
    }

    fun shareAsCSV() {
        val task = Runnable {
            val v : List<GlucoseEntry> = GlucoseDataBase.getInstance(this).glucoseEntryDao().getAll()
            val sb = StringBuilder(v.size*100)
            sb.append(GlucoseEntry.headerString()).append('\n')
            for(entry in v) {
                sb.append(entry.toString()).append('\n')
            }
            val filename = Time.datetime() + "-Sugartop.csv"
            Log.d(TAG, filename)
            val file = File(filesDir, filename)
            file.writeText(sb.toString())
            share_file(file.absolutePath)
            file.deleteOnExit()
        }
        DbWorkerThread.postTask(task)
    }

    private fun share_file(path: String) {
        val file = File(path)
        try {
            val uri = FileProvider.getUriForFile(
                    this@BaseActivity,
                    "io.exoji2e.sugartop.fileprovider",
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MANUAL && resultCode == Activity.RESULT_OK) {
            val value = data!!.getDoubleExtra("value", 0.0)
            val time = data.getLongExtra("time", 0)
            val str = data.getStringExtra("text")
            val sb = Snackbar.make(window.decorView, str, Snackbar.LENGTH_LONG)
            val entry = ManualGlucoseEntry(time, value)
            sb.setAction(R.string.undo, View.OnClickListener {
                DbWorkerThread.postTask(Runnable {
                    GlucoseDataBase.getInstance(this).manualEntryDao().deleteRecord(entry)
                })
                val sb2 = Snackbar.make(window.decorView, "Removed last inserted manual entry", Snackbar.LENGTH_SHORT)
                sb2.show()

            })
            sb.show()
        }
    }
    private fun get_mime() : String? {
        val intentReader = ShareCompat.IntentReader.from(this)
        val s = intentReader.stream
        val mime = contentResolver.getType(s)
        return mime
    }
    private fun read_nfc_data(tag: Tag) : ByteArray? {
        val LOGTAG = "READINGNFC"
        val nfcvTag = NfcV.get(tag)
        val data = ByteArray(360)
        try {
            nfcvTag.connect()
            val uid : ByteArray = tag.id
            Log.d(LOGTAG, "TAGid sz: %d".format(uid.size))
            val sb = StringBuilder()
            for(b in uid){
                sb.append((b+256)%256).append(' ')
            }
            sb.append('\n')
            Log.d(LOGTAG, "TAGid: %s".format(sb.toString()))
            val tagId = RawParser.bin2long(uid)

            Log.d(LOGTAG, "TAGid: %d".format(tagId))
            // Get bytes [i*8:(i+1)*8] from sensor memory and stores in data
            for (i in 0..40) {
                val cmd = byteArrayOf(0x60, 0x20, 0, 0, 0, 0, 0, 0, 0, 0, i.toByte(), 0)
                System.arraycopy(uid, 0, cmd, 2, 8)
                var resp: ByteArray
                val time = Time.now()
                while (true) {
                    try {
                        resp = nfcvTag.transceive(cmd)
                        resp = Arrays.copyOfRange(resp, 2, resp.size)
                        System.arraycopy(resp, 0, data, i * 8, resp.size)
                        break
                    } catch (e: Exception) {
                        if (Time.now() > time + Time.SECOND*5) {
                            Log.e(LOGTAG, "Timeout: took more than 5 seconds to read nfctag")
                            val out = String.format("Failed to read sensor data. %d/40", i)
                            Toast.makeText(this@BaseActivity, out, Toast.LENGTH_LONG).show()
                            return null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val out = String.format("Failed to read sensor data")
            Toast.makeText(this@BaseActivity, out, Toast.LENGTH_LONG).show()
            return null
        } finally {
            try {
                nfcvTag.close()
            } catch (e: Exception) {
                Log.e(LOGTAG, "Error closing tag!")
            }
        }
        return data
    }
    private fun vibrate(t : Long, double : Boolean) {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(double) {
                val wave = LongArray(4, { _ -> t })
                v.vibrate(VibrationEffect.createWaveform(wave, -1))
            } else {
                v.vibrate(VibrationEffect.createOneShot(t, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } else {
            //deprecated in API 26
            v.vibrate(t)
        }
    }

}
