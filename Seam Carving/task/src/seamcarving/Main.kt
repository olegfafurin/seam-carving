package seamcarving

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream
import kotlin.math.pow
import kotlin.system.exitProcess

fun printUsage() {
    println("USAGE: java Main -in <in_filename> -out <out_filename> -width <int> -height <int>")
    exitProcess(0)
}

fun getCliArg(args: Array<String>, name: String): String? {
    assert(name.startsWith("-"))
    val pos = args.indexOf(name)
    return if (pos != -1) args.getOrElse(pos + 1) { null } else null
}

fun <T> transpose(matrix: List<List<T>>): List<List<T>> {
    val w = matrix.size
    val h = matrix[0].size
    return List(h) { i -> List(w) { j -> matrix[j][i] } }
}

fun calcEnergy(pixels: List<List<RGB>>): List<List<Double>> {
    val w = pixels.size
    val h = pixels[0].size
    val energy = MutableList(w) { MutableList(h) { 0.0 } }
    for (i in 0 until w) {
        for (j in 0 until h) {
            val argX = when (i) {
                0 -> 1
                w - 1 -> w - 2
                else -> i
            }
            val argY = when (j) {
                0 -> 1
                h - 1 -> h - 2
                else -> j
            }
            val dx = (pixels[argX + 1][j].red - pixels[argX - 1][j].red).toDouble().pow(2.0) +
                    (pixels[argX + 1][j].green - pixels[argX - 1][j].green).toDouble().pow(2.0) +
                    (pixels[argX + 1][j].blue - pixels[argX - 1][j].blue).toDouble().pow(2.0)
            val dy = (pixels[i][argY + 1].red - pixels[i][argY - 1].red).toDouble().pow(2.0) +
                    (pixels[i][argY + 1].green - pixels[i][argY - 1].green).toDouble().pow(2.0) +
                    (pixels[i][argY + 1].blue - pixels[i][argY - 1].blue).toDouble().pow(2.0)
            energy[i][j] = (dx + dy).pow(0.5)
        }
    }
    return energy
}

fun findSeam(energy: List<List<Double>>): IntArray {
    val w = energy.size
    val h = energy[0].size
    val curRow = MutableList(h) { energy[0][it] }
    val shift = MutableList(w) { MutableList(h) { 0 } }
    for (i in 1 until w) {
        val newRow = MutableList(h) { 0.0 }
        for (j in 0 until h) {
            shift[i][j] = listOf(curRow.getOrNull(j - 1), curRow[j], curRow.getOrNull(j + 1)).withIndex().minByOrNull {
                it.value ?: Double.POSITIVE_INFINITY
            }!!.index - 1
            newRow[j] = energy[i][j] + curRow[j + shift[i][j]]
        }
        for (j in 0 until h) {
            curRow[j] = newRow[j]
        }
    }
    val seam = IntArray(w)
    seam[w - 1] = curRow.withIndex().minByOrNull { it.value }!!.index
    for (i in w - 1 downTo 1) {
        val t = seam[i] + shift[i][seam[i]]
        seam[i - 1] = t
    }
    return seam
}

fun removeSeam(pixels: List<List<RGB>>): List<List<RGB>> {
    val seam = findSeam(calcEnergy(pixels))
    val newPixels = MutableList(pixels.size) { ix -> MutableList(pixels[0].size - 1) { iy -> pixels[ix][iy] } }
    for (i in pixels.indices) {
        var j = seam[i]
        while (j++ < newPixels[i].size - 1)
            newPixels[i][j - 1] = pixels[i][j]
    }
    return newPixels
}

fun main(args: Array<String>) {
    if (args.size < 8) {
        printUsage()
    }
    val inputFileName = getCliArg(args, "-in")!!
    val outputFileName = getCliArg(args, "-out")!!
    val reduceWidth = getCliArg(args, "-width")!!.toInt()
    val reduceHeight = getCliArg(args, "-height")!!.toInt()

    val image = ImageIO.read(FileImageInputStream(File(inputFileName)))
    var pixels: List<List<RGB>> = List(image.height) { i -> List(image.width) { j -> RGB(image.getRGB(j, i)) } }
    for (deletedCols in 0 until reduceWidth)
        pixels = removeSeam(pixels)
    pixels = transpose(pixels)
    for (deletedRows in 0 until reduceHeight)
        pixels = removeSeam(pixels)


    val outImage = BufferedImage(image.width - reduceWidth, image.height - reduceHeight, BufferedImage.TYPE_INT_RGB)
    for (i in 0 until image.width - reduceWidth)
        for (j in 0 until image.height - reduceHeight)
            outImage.setRGB(i, j, pixels[i][j].toInt())
    ImageIO.write(outImage, "png", File(outputFileName))
}