package io.exoji2e.sugartop.settings

import android.content.Context
import android.preference.PreferenceManager.getDefaultSharedPreferences

class UserData {
    companion object {
        val mmol = "mmol/L"
        val lo_th = "lo_threshold"
        val hi_th = "hi_threshold"
        val db_path = "db_path"
        fun get_low_threshold(c: Context) : Float {
            val default = get_multiplier(c) *4.0f
            try {
                return getDefaultSharedPreferences(c)
                        .getFloat(lo_th, default)
            } catch(e:Exception) {
                return default
            }
        }
        fun get_hi_threshold(c: Context) : Float {
            val default = get_multiplier(c) *8.0f
            try {
                return getDefaultSharedPreferences(c)
                        .getFloat(hi_th, default)
            } catch(e:Exception) {
                return default
            }
        }
        fun set_thresholds(c: Context, lo: Float, hi : Float) {
            getDefaultSharedPreferences(c).edit()
                    .putFloat(lo_th, Math.min(lo, hi))
                    .putFloat(hi_th, Math.max(lo, hi))
                    .apply()
        }
        fun get_multiplier(c: Context) : Float {
            return if(get_unit(c) == mmol) 1.0f else 18.0f
        }
        fun get_unit(c: Context) : String {
            return getDefaultSharedPreferences(c).getString("unit", mmol)
        }
        fun get_db_path(c : Context) : String {
            return getDefaultSharedPreferences(c).getString(db_path, "Erbil.db")
        }
        fun set_db_path(c : Context, path: String) {
            getDefaultSharedPreferences(c).edit().putString(db_path, path).apply()
        }
    }
}