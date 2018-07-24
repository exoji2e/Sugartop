package io.exoji2e.erbil

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcV
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.util.*


class ReadActivity : AppCompatActivity() {
    private var nfcAdapter: NfcAdapter? = null
    private var raw_data = ByteArray(0)
    private var dc : DataContainer? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)
        dc = DataContainer.getInstance(this)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)


        if (nfcAdapter == null) {
            Toast.makeText(this, "No NFC access.", Toast.LENGTH_LONG).show()
            finish()
            return

        }

        if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "NFC not enabled?", Toast.LENGTH_LONG).show()
        }

        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        setupForegroundDispatch(this, nfcAdapter!!)
    }

    override fun onPause() {
        stopForegroundDispatch(this, nfcAdapter!!)

        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        // Switch case on intents.
        if (NfcAdapter.ACTION_TECH_DISCOVERED == action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            NfcVReaderTask().execute(tag)
        }
    }



    private inner class NfcVReaderTask : AsyncTask<Tag, Void, Tag>() {

        private val data = ByteArray(360)
        private var success = false

        override fun onPostExecute(tag: Tag?) {
            if (tag == null) return
            if (!success) return
            success = false
            raw_data = data
            val sb = StringBuilder(1200)
            for (i in 0..359) {
                sb.append(i).append(" ").append(RawParser.byte2uns(raw_data[i])).append("\n")
            }
            Log.i(TAG, sb.toString())
            val now = Time.now()
            dc!!.append(raw_data, now)
            val intent = Intent(this@ReadActivity, ResultActivity::class.java)
            startActivity(intent)
        }

        override fun doInBackground(vararg params: Tag): Tag? {
            val tag = params[0]
            val nfcvTag = NfcV.get(tag)
            try {
                nfcvTag.connect()
                val uid = tag.id
                val sb = StringBuilder()
                for (i in 0..7) sb.append(RawParser.byte2uns(uid[i])).append(" ")
                Log.d(TAG, "TAGid: %s".format(sb.toString()))
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

    companion object {
        private const val TAG = "Sensor-NFC"

        fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
            val intent = Intent(activity.applicationContext, activity.javaClass)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

            val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)

            val filters = arrayOfNulls<IntentFilter>(1)
            val techList = arrayOf<Array<String>>()

            // Same filter as in manifest.
            filters[0] = IntentFilter()
            filters[0]?.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
            filters[0]?.addCategory(Intent.CATEGORY_DEFAULT)

            adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
        }

        fun stopForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
            adapter.disableForegroundDispatch(activity)
        }
    }
}
