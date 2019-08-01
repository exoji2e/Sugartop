package io.exoji2e.sugartop.activities

import android.os.Bundle
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import io.exoji2e.sugartop.R

class SettingsActivity : SimpleActivity() {
    override val TAG = "SettingsActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fm = getSupportFragmentManager();
        fm.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }
}

