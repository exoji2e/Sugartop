package io.exoji2e.erbil

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat

class SettingsActivity : SimpleActivity() {
    override val TAG = "SettingsActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fm = getSupportFragmentManager();
        fm.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }
}

