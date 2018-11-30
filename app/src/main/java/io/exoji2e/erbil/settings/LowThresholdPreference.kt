package io.exoji2e.erbil.settings

import android.content.Context
//import android.support.v7.preference.EditTextPreference
import com.takisoft.fix.support.v7.preference.EditTextPreference
import android.util.AttributeSet
import android.util.Log

class LowThresholdPreference(val c: Context, attrs: AttributeSet): EditTextPreference(c, attrs) {
    init{
        summary = UserData.get_low_threshold(c).toString()
    }
    override fun getPersistedString(defaultReturnValue: String?) : String {
        val v : String =  UserData.get_low_threshold(c).toString()
        Log.d("LOWTHRESH", v)
        return v
    }

    override fun persistString(value: String) : Boolean {
        val v = value.toFloat()/UserData.get_multiplier(c)
        if(v < 2 || v > 20) return false
        Log.d("LOWTHRESH", "persistString:" + v.toString())
        UserData.set_thresholds(c, value.toFloat(), UserData.get_hi_threshold(c))
        summary = UserData.get_low_threshold(c).toString()
        return true
    }
}