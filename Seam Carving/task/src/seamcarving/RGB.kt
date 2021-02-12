package seamcarving

/**
 * created by imd on 12.02.2021
 */

class RGB(rgb: Int = 0) {
    val red: Int = rgb.ushr(16) and 0x000000ff
    val green: Int = rgb.ushr(8) and 0x000000ff
    val blue: Int = rgb and 0x000000ff

    fun toInt() = this.red.shl(16) or this.green.shl(8) or this.blue
}