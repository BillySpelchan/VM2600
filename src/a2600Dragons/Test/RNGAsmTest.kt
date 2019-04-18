package a2600Dragons.Test

import a2600Dragons.a2600.Cartridge
import a2600Dragons.a2600.VM2600
import a2600Dragons.m6502.Assembler
import a2600Dragons.m6502.M6502
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.Button
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File
import java.util.*

open class RNG {
    private val rnd = Random()
    val randomBytes = arrayListOf<Int>()
    var indx = 0

    init {
        for (i in 0..65535) {
            randomBytes.add(0)
        }
    }

    open fun nextUByte():Int {
        val temp = randomBytes[indx]
        ++indx
        if (indx > 65535)
            indx = 0
        return temp and 255
    }


    fun testFill() {
        for (i in 0..65535) {
            randomBytes[i] = rnd.nextInt()
        }

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

    val canvas = Canvas(512.0, 512.8)
    val rng = RNG()
    val randogram = Randogram(rng)


    private fun handleLoadAsm() {
        var memoryManager = Cartridge()
        var m6502 = M6502( memoryManager )
        // right now assuming just single bank rom - multibank in future
        var byteData =  ByteArray(4096)

        val fileChooser = FileChooser()
        var assemblyFile = fileChooser.showOpenDialog(null)
        if (assemblyFile.isFile) {
            var assemblyList:ArrayList<String> = ArrayList(assemblyFile.readLines())

            var assembler = Assembler(m6502)
            assembler.assembleProgram(assemblyList)
            // note - in future add support for multibank assembly
            for (cntrRom in 0..assembler.currentBank.size-1) {
                byteData[cntrRom] = assembler.currentBank.readBankAddress(cntrRom).toByte()
                memoryManager.write(cntrRom, assembler.currentBank.readBankAddress(cntrRom))
            }

            m6502.state.ip = 0
            var ipAddress = m6502.state.ip
            var indx = 0
            while (memoryManager.read( ipAddress ) != 0) {
                if (memoryManager.read( ipAddress )==0xEA) {
                    rng.randomBytes[indx] = m6502.state.acc
                    ++indx
                }
                m6502.step()
                ipAddress = m6502.state.ip
            }
        }

//        rng.testFill()
        randogram.clear()
        randogram.generate()
        randogram.drawToCanvas(canvas, 2.0)
    }

    override fun start(primaryStage: Stage?) {

        randogram.generate()
        randogram.drawToCanvas(canvas, 2.0)

        val btn = Button("Test Assembly")
        btn.setOnAction {handleLoadAsm()}
        val root = VBox(10.0, canvas, btn)
        val scene = Scene(root, 800.0, 600.0)

        primaryStage!!.title = "JavaFX test"
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(RNGAsmTest::class.java, *args)
}
