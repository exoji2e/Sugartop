package io.exoji2e.erbil.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import io.exoji2e.erbil.*
import io.exoji2e.erbil.database.DbWorkerThread
import io.exoji2e.erbil.database.ErbilDataBase
import io.exoji2e.erbil.database.GlucoseEntry
import io.exoji2e.erbil.database.ManualGlucoseEntry
import java.io.*
import kotlinx.android.synthetic.main.element_bottom_navigation.*
import java.util.*


abstract class ErbilActivity : AppCompatActivity() {
    abstract val TAG : String
    private lateinit var nfcAdapter: NfcAdapter
    private val REQUEST_MANUAL = 0;
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
        if (    action == NfcAdapter.ACTION_TECH_DISCOVERED ||
                action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
                action == NfcAdapter.ACTION_TAG_DISCOVERED) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            NfcVReaderTask().execute(tag)
        }
        if(action == Intent.ACTION_SEND) {
            val task = Runnable {
                Log.d(TAG, "received-send-intent")
                val dc = DataContainer.getInstance(this)
                // Should maybe support merging later on?
                if(dc.size() != 0){
                    Toast.makeText(this, "Database not empty, aborting import.", Toast.LENGTH_LONG).show()
                    return@Runnable
                }
                Toast.makeText(this, "Importing data...", Toast.LENGTH_LONG).show()
                val intentReader = ShareCompat.IntentReader.from(this)
                dc.insert(read(intentReader))
                Toast.makeText(this, "Import successful!", Toast.LENGTH_LONG).show()

            }
            DbWorkerThread.getInstance().postTask(task)
        }
    }

    override fun onResume() {
        super.onResume()
        setupForegroundDispatch(this, nfcAdapter)
    }
    override fun onPause() {
        super.onPause()
        stopForegroundDispatch(this, nfcAdapter)
    }
    fun putOnTop(cls: Class<*>) {
        val reopen = Intent(this, cls)
        reopen.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivityIfNeeded(reopen, 0)
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
        when (item.getItemId()) {
            R.id.share_db -> {
                shareDB()
                return true
            }
            R.id.manual -> {
                launchManual()
                return true
            }
            R.id.share_csv -> {
                shareAsCSV()
                return true
            }
            R.id.calibrate -> {
                launchCalibrate()
                return true
            }
            R.id.settings -> {
                launchSettings()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun shareDB() {
        val path = getDatabasePath(ErbilDataBase.NAME).getAbsolutePath()
        share_file(path)
    }
    fun shareAsCSV() {
        val task = Runnable {
            val v : List<GlucoseEntry> = ErbilDataBase.getInstance(this).glucoseEntryDao().getAll()
            val sb = StringBuilder(v.size*100)
            sb.append(GlucoseEntry.headerString()).append('\n')
            for(entry in v) {
                sb.append(entry.toString()).append('\n')
            }
            val filename = Time.datetime() + "-Erbil.csv"
            Log.d(TAG, filename)
            val file = File(filesDir, filename)
            file.writeText(sb.toString())
            share_file(file.absolutePath)
            file.deleteOnExit()
        }
        DbWorkerThread.getInstance().postTask(task)
    }

    private fun share_file(path: String) {
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
        startActivityForResult(intent, REQUEST_MANUAL)
    }
    private fun launchSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MANUAL && resultCode == Activity.RESULT_OK) {
            val value = data!!.getDoubleExtra("value", 0.0)
            val time = data.getLongExtra("time", 0)
            val str = data.getStringExtra("text")
            val sb = Snackbar.make(window.decorView, str, Snackbar.LENGTH_LONG)
            val entry = ManualGlucoseEntry(time, value)
            sb.setAction(R.string.undo, View.OnClickListener { _ ->
                DbWorkerThread.getInstance().postTask(Runnable {
                    ErbilDataBase.getInstance(this).manualEntryDao().deleteRecord(entry)
                })
                val sb2 = Snackbar.make(window.decorView, "Removed last inserted manual entry", Snackbar.LENGTH_SHORT)
                sb2.show()
            })
            sb.show()
        }
    }
    private fun launchCalibrate() {
        val intent = Intent(this, CalibrateActivity::class.java)
        startActivity(intent)
    }
    private fun read(intentReader: ShareCompat.IntentReader) : List<GlucoseEntry> {
        val s = intentReader.stream
        val br = BufferedReader(InputStreamReader(contentResolver.openInputStream(s)))
        var lines = br.readLines()
        val entries = mutableListOf<GlucoseEntry>()
        for (line in lines) {
            val entry = GlucoseEntry.fromString(line)
            if(entry != null)
                entries.add(entry)
        }
        return entries
    }
    private inner class NfcVReaderTask : AsyncTask<Tag, Void, Tag>() {

        private val data = ByteArray(360)
        private var success = false
        private var tagId = 0L
        private val LOGTAG = "NFCREADER"
        private var request_fail = -1

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

        override fun onPostExecute(tag: Tag?) {
            if (tag == null || !success) {
                vibrate(300L, false)
                val S = "Failed to read sensor data."
                val out = if(request_fail != -1) S + " " + request_fail + "/40" else S
                Toast.makeText(this@ErbilActivity, out, Toast.LENGTH_LONG).show()

                request_fail = -1
                success = false
                return
            }
            Log.d(LOGTAG, "Long " + Toast.LENGTH_LONG + "Short: " + Toast.LENGTH_SHORT)

            vibrate(100L, true)
            success = false
            val sb = StringBuilder(2880) // At least enough.
            for (i in 0..359) {
                sb.append(i).append(" ").append(RawParser.byte2uns(data[i])).append("\n")
            }
            //Log.d(LOGTAG, sb.toString())
            val now = Time.now()
            val task = Runnable {
                val dc = DataContainer.getInstance(this@ErbilActivity)
                dc.append(data, now, tagId)
            }
            DbWorkerThread.getInstance().postTask(task)
            putOnTop(RecentActivity::class.java)
        }

        override fun doInBackground(vararg params: Tag): Tag? {
            val tag = params[0]
            val nfcvTag = NfcV.get(tag)
            try {
                nfcvTag.connect()
                val uid = tag.id
                tagId = RawParser.bin2long(uid)

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
                                Log.e(LOGTAG, "Timeout: took more than 1 second to read nfctag")
                                request_fail = i
                                return null
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                val sb = StringBuilder()
                for(st in e.stackTrace) {
                    sb.append(st.toString()).append('\n')
                }
                Log.i(LOGTAG, sb.toString())
                return null
            } finally {
                try {
                    nfcvTag.close()
                } catch (e: Exception) {
                    Log.e(LOGTAG, "Error closing tag!")
                }

            }
            success = true

            return tag
        }

    }
    fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
        val intent = Intent(activity.applicationContext, activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)

        val filters = arrayOfNulls<IntentFilter>(1)
        val techList = arrayOf<Array<String>>()

        // Same filter as in manifest.
        filters[0] = IntentFilter()
        filters[0]?.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
        filters[0]?.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        filters[0]?.addAction(NfcAdapter.ACTION_TAG_DISCOVERED)
        filters[0]?.addCategory(Intent.CATEGORY_DEFAULT)
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
    }

    fun stopForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
        adapter.disableForegroundDispatch(activity)
    }

}
