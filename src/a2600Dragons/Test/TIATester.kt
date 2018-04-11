package a2600Dragons.Test

import a2600Dragons.tia.PlayerMissileGraphic
import a2600Dragons.tia.TIA
import a2600Dragons.tia.TIAColors
import a2600Dragons.tia.TIAPIARegs
import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
//import javafx.scene.control.Button
import javafx.scene.image.ImageView
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.Stage

class TIATester : Application() {
    val SCENE_HEIGHT = 512.0
    val SCENE_WIDTH = 640.0
    val tiaColors = TIAColors()

    private fun pixelBlock(pixelWriter: PixelWriter, x:Int, y:Int, w:Int, h:Int, argb:Int) {
        for (cntrRow:Int in 0..(h-1)) {
            for (cntrCol: Int in 0..(w - 1)) {
                pixelWriter.setArgb(x + cntrCol, y + cntrRow, argb)
            }
        }
    }

    /**
     * Convert the TIA raster line to the appropriate pixel data and apply to the raster image at the appropriate line.
     */
    private fun rasterizeTIA(pixelWriter: PixelWriter, scanline:Int, data:Array<Int>) {
        for (cntr in 0..159)
            pixelWriter.setArgb(cntr, scanline, tiaColors.getARGB(data[cntr]))
    }

    /** Convienience function that renders the line then rasterizes it (reduces duplicated pairs of code) */
    private fun renderThenRasterize(tia:TIA,pixelWriter: PixelWriter, scanline:Int, numCopies:Int = 1) {
        assert(numCopies > 0)
        assert(scanline < 192)
        assert(scanline+numCopies < 192)
        for (cntr in 0 until numCopies) {
            tia.renderScanline()
            rasterizeTIA(pixelWriter, scanline + cntr, tia.rasterLine)
        }
    }

    override fun start(primaryStage: Stage?) {
//        var btn = Button("Hello JavaFX")
//        btn.setOnAction { message.text = "Hello World!" }
        val tabs = TabPane()
        tabs.tabs.add(buildPMGTab())
        tabs.tabs.add(buildTIAConsoleTab())
        tabs.tabs.add(buildColorpickerTab())
        tabs.tabs.add(buildRainbowTab())
        val scene = Scene(tabs, SCENE_WIDTH, SCENE_HEIGHT)

        primaryStage!!.title = "JavaFX test"
        primaryStage.scene = scene
        primaryStage.show()
    }

    private fun buildColorpickerTab(): Tab {
        val message = Text("Click color to get palette index")

        val palette = WritableImage(256, 256)
        val pixelWriter = palette.pixelWriter

        for (cntrRow:Int in 0..15) {
            for (cntrCol:Int in 0..15) {
                pixelBlock(pixelWriter, cntrCol * 16, cntrRow * 16, 16,16, tiaColors.getARGB(cntrRow*16+cntrCol))
            }
        }

        val viewer = ImageView(palette)
        viewer.setOnMouseClicked { event: javafx.scene.input.MouseEvent? ->  run {
            val x:Int = if (event != null) event.x.toInt() / 16 else 0
            val y:Int = if (event != null) event.y.toInt() / 16 else 0
            val indx = (y * 16 + x) and 254
            message.text = "Color byte value is $indx (\$${indx.toString(16)} HTML ${tiaColors.getHTMLColor(indx)})"
        } }

        val root = VBox(10.0, message, viewer)
        root.alignment = Pos.CENTER

        val tab = Tab()
        tab.text = "Palette"
        tab.content = root
        tab.isClosable = false
        return tab
    }


    private fun buildRainbowTab():Tab {

        val tiaImage = WritableImage(160, 192)
        val tia = TIA()
        tia.writeRegister(TIAPIARegs.PF0, 0x50)
        tia.writeRegister(TIAPIARegs.PF1, 0xAA)
        tia.writeRegister(TIAPIARegs.PF2, 0x55)
        tia.writeRegister(TIAPIARegs.COLUPF, 96)
        for (cntr in 0..127) {
            tia.writeRegister(TIAPIARegs.COLUBK, cntr * 2)
            tia.renderScanline()
            rasterizeTIA(tiaImage.pixelWriter, cntr, tia.rasterLine)
        }
        tia.writeRegister(TIAPIARegs.PF0, 0xA0)
        tia.writeRegister(TIAPIARegs.PF1, 0x55)
        tia.writeRegister(TIAPIARegs.PF2, 0xAA)
        for (cntr in 128..191) {
            tia.renderScanline()
            rasterizeTIA(tiaImage.pixelWriter, cntr, tia.rasterLine)
        }

        val tiaView = ImageView(tiaImage)
        tiaView.isPreserveRatio = false
        tiaView.fitWidth = 640.0
        tiaView.fitHeight = 192 * 2.0
//        tiaView.scaleX = 2.0
//        tiaView.scaleY = 1.0
        val tab = Tab("Rainbow")
        tab.content = tiaView
        tab.isClosable = false
        return tab

    }

    /**
     * tests a "Sprite" aka Player/Missile Graphics to see if it draws where it is expected to draw
     * expects the expected pixel array to have values between 0 and 159 with no duplicates
     */
    private fun pmgTestPixels(pmg:PlayerMissileGraphic, expectedPixels:Array<Int>,
            testLog:StringBuilder):Boolean {
        var drawCount = 0
        var successfulTest = true
        // count number of pixels drawn while making sure they are suppose to be drawn
        for (cntr in 0..159) {
            if (pmg.isPixelDrawn(cntr)) {
                ++drawCount
                if (expectedPixels.contains(cntr) == false) {
                    successfulTest = false
                    testLog.append("drew in unexpected column $cntr\n")
                    --drawCount
                    break
                }
            }
        }

        // verify all pixels were drawn
        if (drawCount != expectedPixels.size) {
            successfulTest = false
            testLog.append("did not draw all expected pixels\n")
        }

        if (successfulTest)
            testLog.append("Test succeeded\n")

        return successfulTest
    }


    private fun runPMGTests(verbose:Boolean):String {
        var text = "Player Missile Graphics internal class\n"
        val testLog = StringBuilder()
        val missile = PlayerMissileGraphic(1, 0)
        var allTestsPassed = true

        testLog.append("Missile normal drawing ")
        missile.drawingBits = 1
        missile.x = 11
        allTestsPassed = allTestsPassed and pmgTestPixels(missile, arrayOf(11), testLog)

        testLog.append("Missile scale x2 drawing ")
        missile.scale = 2
        allTestsPassed = allTestsPassed and pmgTestPixels(missile, arrayOf(11,12), testLog)

        testLog.append("Missile scale x4 drawing ")
        missile.scale = 4
        allTestsPassed = allTestsPassed and pmgTestPixels(missile, arrayOf(11,12,13,14), testLog)

        testLog.append("Missile scale x8 drawing ")
        missile.scale = 8
        allTestsPassed = allTestsPassed and pmgTestPixels(missile, arrayOf(11,12,13,14,15,16,17,18), testLog)

        testLog.append("Missile disabled drawing ")
        missile.drawingBits = 0
        allTestsPassed = allTestsPassed and pmgTestPixels(missile, emptyArray(), testLog)

        val player = PlayerMissileGraphic(8,0)
        player.drawingBits = 0xA2
        player.x = 42
        testLog.append("Player mode normal")
        allTestsPassed = allTestsPassed and pmgTestPixels(player, arrayOf(42,44,48), testLog)

        player.setPlayerScaleCopy(TIAPIARegs.PMG_DOUBLE_SIZE)
        testLog.append("Player mode double size")
        allTestsPassed = allTestsPassed and pmgTestPixels(player, arrayOf(42,43,46,47,54,55), testLog)

        player.setPlayerScaleCopy(TIAPIARegs.PMG_QUAD_PLAYER)
        testLog.append("Player mode Quad size")
        allTestsPassed = allTestsPassed and pmgTestPixels(player, arrayOf(42,43,44,45, 50,51,52,53, 66,67,68,69), testLog)

        player.setPlayerScaleCopy(TIAPIARegs.PMG_TWO_CLOSE)
        testLog.append("Player mode Two close")
        allTestsPassed = allTestsPassed and pmgTestPixels(player, arrayOf(42,44,48, 58,60,64), testLog)

        player.setPlayerScaleCopy(TIAPIARegs.PMG_TWO_MEDIUM)
        testLog.append("Player mode Two Medium")
        allTestsPassed = allTestsPassed and pmgTestPixels(player, arrayOf(42,44,48, 74,76,80), testLog)

        player.setPlayerScaleCopy(TIAPIARegs.PMG_TWO_WIDE)
        testLog.append("Player mode Two Wide")
        allTestsPassed = allTestsPassed and pmgTestPixels(player, arrayOf(42,44,48, 106,108,112), testLog)

        player.setPlayerScaleCopy(TIAPIARegs.PMG_THREE_CLOSE)
        testLog.append("Player mode Three close")
        allTestsPassed = allTestsPassed and pmgTestPixels(player, arrayOf(42,44,48, 58,60,64, 74,76,80), testLog)

        player.setPlayerScaleCopy(TIAPIARegs.PMG_THREE_MEDIUM)
        testLog.append("Player mode Three Medium")
        allTestsPassed = allTestsPassed and pmgTestPixels(player, arrayOf(42,44,48, 74,76,80, 106,108,112), testLog)

        player.setPlayerScaleCopy(TIAPIARegs.PMG_THREE_MEDIUM)
        player.mirror = true
        testLog.append("Player mode Three Medium Mirrored")
        allTestsPassed = allTestsPassed and pmgTestPixels(player, arrayOf(43,47,49, 75,79,81, 107,111,113), testLog)

        if (verbose)
            text += testLog.toString()
        text += if (allTestsPassed) "PMG class tests passed!" else "PMG class tests FAILED!"
        return text
    }

    private fun consoleTests(verbose:Boolean):String {
        val tia = TIA()
        var text = "run tests in verbose mode: ${verbose}\n"

        text += "Reverse 0xF becomes : ${tia.reversePFBits(0xF).toString(16)}\n"

        tia.writeRegister(TIAPIARegs.PF0, 0x50)
        tia.writeRegister(TIAPIARegs.PF1, 0xAA)
        tia.writeRegister(TIAPIARegs.PF2, 0x55)
        text += "PLAYFEILD - EVEN Bits: ${tia.playfieldBits.toString(2)}\n"

        tia.writeRegister(TIAPIARegs.PF0, 0xA0)
        tia.writeRegister(TIAPIARegs.PF1, 0x55)
        tia.writeRegister(TIAPIARegs.PF2, 0xAA)
        text += "PLAYFEILD - ODD Bits : ${tia.playfieldBits.toString(2)}\n"

        text += runPMGTests(true) // forcing verbose for now
        return text
    }

    private fun buildTIAConsoleTab():Tab {
        val console = TextArea("TIA text-based tests")
        console.isWrapText = true
        console.prefRowCount = 100


        val verboseToggle = CheckBox("Run tests in verbose mode")
        val runTestButton = Button("Run Tests")
        runTestButton.setOnAction { event -> run{
            console.text += consoleTests(verboseToggle.isSelected)
        } }
        val root = VBox(10.0, verboseToggle, runTestButton, console)
        val tab = Tab("Console")
        tab.content = root
        tab.isClosable = false
        return tab
    }

    private fun buildPMGTab():Tab {

        val tiaImage = WritableImage(160, 192)
        val tia = TIA()
        tia.writeRegister(TIAPIARegs.PF0, 0x0)
        tia.writeRegister(TIAPIARegs.PF1, 0x0)
        tia.writeRegister(TIAPIARegs.PF2, 0x0)

        // set up colors
        tia.writeRegister(TIAPIARegs.COLUPF, 30)
        tia.writeRegister(TIAPIARegs.COLUP0, 70)
        tia.writeRegister(TIAPIARegs.COLUP1, 130)
        // set up PMG positions - doing work on line above first displayed line
        tia.runToClock(78)
        tia.writeRegister(TIAPIARegs.RESBL,0)
        tia.runToClock(88)
        tia.writeRegister(TIAPIARegs.RESM0, 0)
        tia.runToClock(98)
        tia.writeRegister(TIAPIARegs.RESM1, 0)
        tia.runToClock(108)
        tia.writeRegister(TIAPIARegs.RESP0, 0)
        tia.runToClock(158)
        tia.writeRegister(TIAPIARegs.RESP1, 0)
        tia.renderScanline()

        // set up sprite data
        tia.writeRegister(TIAPIARegs.ENABL, 2)
        tia.writeRegister(TIAPIARegs.ENAM0, 2)
        tia.writeRegister(TIAPIARegs.ENAM1, 2)
        tia.writeRegister(TIAPIARegs.GRP0, 0x55)
        tia.writeRegister(TIAPIARegs.GRP1, 0xAA)
        renderThenRasterize(tia, tiaImage.pixelWriter, 0, 192)

        val tiaView = ImageView(tiaImage)
        tiaView.isPreserveRatio = false
        tiaView.fitWidth = 640.0
        tiaView.fitHeight = 192 * 2.0

//        tiaView.scaleX = 2.0
//        tiaView.scaleY = 1.0
        val tab = Tab("PMGDrawing")
        tab.content = tiaView
        tab.isClosable = false
        return tab

    }
}

fun main(args: Array<String>) {
    Application.launch(TIATester::class.java, *args)
}