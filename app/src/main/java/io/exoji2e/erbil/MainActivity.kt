package io.exoji2e.erbil

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    class MainActivity : AppCompatActivity() {

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)
            startReadActivity()
        }

        fun startReadActivity() {
            val intent = Intent(this, ReadActivity::class.java)
            startActivity(intent)
        }

    }

}
