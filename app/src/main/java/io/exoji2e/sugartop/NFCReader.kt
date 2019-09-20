package io.exoji2e.sugartop

import android.nfc.Tag
import android.nfc.tech.NfcV
import android.util.Log
import java.util.*

class NFCReader {
    companion object {
        val TAG = "NFCReader"
        val start_str = "Started Sensor"
        fun start_cmd(): ByteArray = byteArrayOf(0x02, 0xA0 - 0x100, 0x07, 0xC2 - 0x100, 0xAD - 0x100, 0x75, 0x21)

        fun status_cmd(): ByteArray = byteArrayOf(0x02, 0xA1 - 0x100, 0x07)

        fun onTag(tag: Tag): Pair<ByteArray?, String> {
            val nfcvTag = NfcV.get(tag)
            val data = ByteArray(360)
            val uid: ByteArray = tag.id
            var resp: ByteArray
            try {
                nfcvTag.connect()
                resp = nfcvTag.transceive(status_cmd())
            } catch (e: Exception) {
                return Pair(null, "Couldn't connect to sensor")
            }
            val zero: Byte = 0x00
            if (resp[5] == zero && resp[6] == zero) {
                return start(nfcvTag)
            } else {
                return readout(nfcvTag, uid)
            }


        }

        fun start(nfcvTag: NfcV): Pair<ByteArray?, String> {
            val resp = nfcvTag.transceive(start_cmd())
            Log.d(TAG, start_str)
            return Pair(null, start_str)
        }

        fun readout(nfcvTag: NfcV, uid: ByteArray): Pair<ByteArray?, String> {
            val LOGTAG = "READINGNFC"
            val data = ByteArray(360)
            val sb = StringBuilder()
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
                        if (Time.now() > time + Time.SECOND * 5) {
                            Log.e(LOGTAG, "Timeout: took more than 5 seconds to read nfctag")
                            return Pair(null, "Failed to read sensor data. %d/40".format(i))
                        }
                    }
                }
            }

            try {
                nfcvTag.close()
            } catch (e: Exception) {
                Log.e(LOGTAG, "Error closing tag!")
            }
            return Pair(data, "")
        }
    }
}