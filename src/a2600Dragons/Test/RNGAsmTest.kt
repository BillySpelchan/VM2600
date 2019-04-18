package a2600Dragons.Test

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.Stage
import java.util.*

open class RNG {
    private val rnd = Random()

    open fun nextUByte():Int {
        return rnd.nextInt() and 255
    }
}


class Randogram (val rng:RNG) {
    var grid = Array(256, {Array(256, {0} )})

    fun clear() {
        for (cntrX in 0..255)
            for (cntrY in 0..255)
                grid[cntrX][cntrY] = 0
    }

    fun generate() {
        for (cntr in 0..32767) {
            val x = rng.nextUByte()
            val y = rng.nextUByte()
            ++grid[x][y]
        }
    }

    fun drawToCanvas(canvas: Canvas, scale:Double) {
        val gc = canvas.graphicsContext2D
        for (cntrX in 0..255)
            for (cntrY in 0..255) {
                gc.fill = Color.gray(1.0 / (grid[cntrX][cntrY].toDouble() + 1.0))
                gc.fillRect(cntrX.toDouble()*scale, cntrY.toDouble()*scale, scale, scale)
            }
    }
}

class RNGAsmTest : Application() {
    override fun start(primaryStage: Stage?) {

        val canvas = Canvas(512.0, 512.8)
        val rng = RNG()
        val randogram = Randogram(rng)
        randogram.generate()
        randogram.drawToCanvas(canvas, 2.0)
        val root = VBox(10.0, canvas)
        val scene = Scene(root, 800.0, 600.0)

        primaryStage!!.title = "JavaFX test"
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(RNGAsmTest::class.java, *args)
}
