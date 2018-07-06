package io.exoji2e.erbil

class Util {
    companion object {
        fun bin2int(a: Byte, b: Byte) : Int = byte2uns(a)*256 + byte2uns(b)

        fun byte2uns(a: Byte) : Int = (a.toInt() + 256)%256

        fun sensor2mmol(v: Int) : Double = v/200.0

        fun last(data: ByteArray) : Double {
            val (iR, iH) = Pair(data[26], data[27])
            val (startR, startH) = Pair(28, 124)
            //val history = ByteArray(6*32)
            val recent = data.sliceArray(IntRange(startR + iR*6, startR + 16*6 - 1)).
                    plus(data.sliceArray(IntRange(startR, startR + iR*6 - 1)))
            val history = data.sliceArray(IntRange(startH + iH*6, startH + 32*6 - 1)).
                    plus(data.sliceArray(IntRange(startH, startH + iH*6 - 1)))
            return sensor2mmol(bin2int(recent[6*15+1], recent[6*15]))
        }
    }
}