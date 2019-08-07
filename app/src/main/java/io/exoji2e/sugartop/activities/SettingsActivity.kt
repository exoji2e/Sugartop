package io.exoji2e.sugartop.activities

import android.os.Bundle
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import io.exoji2e.sugartop.R
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v4.app.FragmentManager
import io.exoji2e.sugartop.settings.HiThresholdPreference
import io.exoji2e.sugartop.settings.LowThresholdPreference
import io.exoji2e.sugartop.settings.UserData


class SettingsActivity : SimpleActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    val fm: FragmentManager = supportFragmentManager
    val fragment = SettingsFragment()
    override val TAG = "SettingsActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fm.beginTransaction().replace(android.R.id.content, fragment).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences,
                                  key: String) {

        (fragment.findPreference(UserData.lo_th) as LowThresholdPreference).refresh()
        (fragment.findPreference(UserData.hi_th) as HiThresholdPreference).refresh()

        fragment.findPreference(UserData.UNIT).summary = UserData.get_unit(this)
    }
}

