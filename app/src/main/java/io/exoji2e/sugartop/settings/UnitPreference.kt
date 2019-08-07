package io.exoji2e.sugartop.settings

import android.content.Context
import android.support.v7.preference.ListPreference
import android.util.AttributeSet
import android.util.Log

class UnitPreference(val c: Context, attrs: AttributeSet): ListPreference(c, attrs) {
    init{
        summary = UserData.get_unit(c)
    }

    override fun setValue(value: String?) {
        Log.d("UnitPreference", "Value to be set: $value")

        if(value == "mmol/L"){
            UserData.set_mmol(c)
        } else if(value == "mg/dL"){
            UserData.set_mgdl(c)
        } else if(value != null){
            Log.d("UnitPreference", "Value not valid: $value")
        }
    }
}