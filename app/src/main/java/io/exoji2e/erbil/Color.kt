package io.exoji2e.erbil
import android.graphics.Color as C

class Color {
    companion object {
        val gray = C.rgb(100, 100, 100)
        val hotPink = C.rgb(255, 64, 129)
        val black = C.rgb(0, 0 , 0)
        val colors = intArrayOf(C.rgb(193, 37, 82), C.rgb(42, 109, 130),
                C.rgb(118, 174, 175), C.rgb(106, 150, 31),
                C.rgb(179, 100, 53), C.rgb(136, 180, 187))
        fun lineColor(i : Int) = if(i >= 0) colors[i % colors.size] else 0
    }
}