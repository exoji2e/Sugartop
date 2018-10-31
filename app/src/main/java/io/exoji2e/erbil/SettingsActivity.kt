package io.exoji2e.erbil

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.MenuItem

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fm = getSupportFragmentManager();
        fm.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
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

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }

}

