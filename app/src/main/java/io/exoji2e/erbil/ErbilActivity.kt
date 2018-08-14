package io.exoji2e.erbil

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import android.os.*
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import java.io.*
import kotlinx.android.synthetic.main.element_bottom_navigation.*
import java.util.*


abstract class ErbilActivity : AppCompatActivity() {
    abstract val TAG : String
    private lateinit var nfcAdapter: NfcAdapter
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
                        putOnTop(ResultActivity::class.java)
                        true
                    }
                    else -> {
                        true
                    }
                }
        }
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

            else -> return super.onOptionsItemSelected(item)
        }
    }

    fun shareDB() {
        val path = getDatabasePath(ErbilDataBase.NAME).getAbsolutePath()
        share_file(path)
    }
    fun shareAsCSV() {
        val task = Runnable {
            val v : List<GlucoseEntry> = ErbilDataBase.getInstance(this)!!.glucoseEntryDao().getAll()
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
        startActivity(intent)
    }
    private inner class NfcVReaderTask : AsyncTask<Tag, Void, Tag>() {

        private val data = ByteArray(360)
        private var success = false
        private var tagId = 0L

        private fun vibrate(t : Long, double : Boolean) {
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if(double) {
                    val wave = LongArray(4, { i -> t })
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
                Toast.makeText(this@ErbilActivity, "Failed to read sensor data.", Toast.LENGTH_LONG).show()
                success = false
                return
            }

            vibrate(100L, true)
            success = false
            val sb = StringBuilder(2880) // At least enough.
            for (i in 0..359) {
                sb.append(i).append(" ").append(RawParser.byte2uns(data[i])).append("\n")
            }
            Log.d(TAG, sb.toString())
            val now = Time.now()
            val task = Runnable {
                val dc = DataContainer.getInstance(this@ErbilActivity)
                dc.append(data, now, tagId)
            }
            DbWorkerThread.getInstance().postTask(task)
            putOnTop(ResultActivity::class.java)
        }

        override fun doInBackground(vararg params: Tag): Tag? {
            val tag = params[0]
            val nfcvTag = NfcV.get(tag)
            try {
                nfcvTag.connect()
                val uid = tag.id
                tagId = RawParser.bin2long(uid)

                Log.d(TAG, "TAGid: %d".format(tagId))
                // Get bytes [i*8:(i+1)*8] from sensor memory and stores in data
                for (i in 0..40) {
                    val cmd = byteArrayOf(0x60, 0x20, 0, 0, 0, 0, 0, 0, 0, 0, i.toByte(), 0)
                    System.arraycopy(uid, 0, cmd, 2, 8)
                    var resp: ByteArray
                    val time = Time.now()
                    while (true) {
                        try {
                            resp = nfcvTag.transceive(cmd)
                            break
                        } catch (e: IOException) {
                            if (Time.now() > time + Time.SECOND) {
                                Log.e(TAG, "Timeout: took more than 1 second to read nfctag")
                                return null
                            }
                        }
                    }
                    resp = Arrays.copyOfRange(resp, 2, resp.size)
                    System.arraycopy(resp, 0, data, i * 8, resp.size)
                }

            } catch (e: Exception) {
                Log.i(TAG, e.toString())
                return null
            } finally {
                try {
                    nfcvTag.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing tag!")
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
