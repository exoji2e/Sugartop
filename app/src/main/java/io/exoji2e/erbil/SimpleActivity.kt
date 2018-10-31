package io.exoji2e.erbil

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem



abstract class SimpleActivity : AppCompatActivity() {
    abstract val TAG : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bar = getSupportActionBar()
        if(bar!=null) bar.setDisplayHomeAsUpEnabled(true)
    }
    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.getItemId()) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(menuItem)
        }
    }
}
