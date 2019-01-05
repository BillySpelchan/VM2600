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
            tabs.tabs.add(buildTIAConsoleTab())
            tabs.tabs.add(buildPMGTab())
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
        // set up simple checker pattern
        tia.writeRegister(TIAPIARegs.PF0, 0x50)
        tia.writeRegister(TIAPIARegs.PF1, 0xAA)
        tia.writeRegister(TIAPIARegs.PF2, 0x55)
        tia.writeRegister(TIAPIARegs.COLUPF, 96)
        // run through tia colors
        for (cntr in 0..127) {
            tia.writeRegister(TIAPIARegs.COLUBK, cntr * 2)
            tia.renderScanline()
            rasterizeTIA(tiaImage.pixelWriter, cntr, tia.rasterLine)
        }
        // flip checker pattern
        tia.writeRegister(TIAPIARegs.PF0, 0xA0)
        tia.writeRegister(TIAPIARegs.PF1, 0x55)
        tia.writeRegister(TIAPIARegs.PF2, 0xAA)
        tia.writeRegister(TIAPIARegs.COLUBK, 4)
        renderThenRasterize(tia, tiaImage.pixelWriter, 128, 8)
        // set up sprites
        tia.writeRegister(TIAPIARegs.COLUP0, 30)
        tia.writeRegister(TIAPIARegs.COLUP1, 142)
        tia.writeRegister(TIAPIARegs.ENAM0, 2)
        tia.writeRegister(TIAPIARegs.ENAM1, 2)
        tia.writeRegister(TIAPIARegs.GRP0, 0x55)
        tia.writeRegister(TIAPIARegs.GRP1, 0xAA)
        // cheat with setting up sprite positions
        tia.sprites[TIAPIARegs.ISPRITE_MISSILE0].x = 20
        tia.sprites[TIAPIARegs.ISPRITE_MISSILE1].x = 118
        tia.sprites[TIAPIARegs.ISPRITE_PLAYER0].x = 138
        tia.sprites[TIAPIARegs.ISPRITE_PLAYER1].x = 40

        // test different playfield modes
        tia.writeRegister(TIAPIARegs.CTRLPF, 1)
        renderThenRasterize(tia, tiaImage.pixelWriter, 136, 8)
        tia.writeRegister(TIAPIARegs.CTRLPF, 2)
        renderThenRasterize(tia, tiaImage.pixelWriter, 144, 8)
        tia.writeRegister(TIAPIARegs.CTRLPF, 3)
        renderThenRasterize(tia, tiaImage.pixelWriter, 152, 8)
        tia.writeRegister(TIAPIARegs.CTRLPF, 4)
        renderThenRasterize(tia, tiaImage.pixelWriter, 160, 8)
        tia.writeRegister(TIAPIARegs.CTRLPF, 5)
        renderThenRasterize(tia, tiaImage.pixelWriter, 168, 8)
        tia.writeRegister(TIAPIARegs.CTRLPF, 6)
        renderThenRasterize(tia, tiaImage.pixelWriter, 176, 8)
        tia.writeRegister(TIAPIARegs.CTRLPF, 7)
        renderThenRasterize(tia, tiaImage.pixelWriter, 184, 8)


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


    /** Loop through possible collisions and verify only collisions that happened happen and that the ones that doe
     * happen are detectable     */
    private fun runPMGCollisionTests(verbose:Boolean):String {
        var text = "PMG Collision Tests\n"
        val tia = TIA()
        for (cntr in 0..63) {
            val isPlayfield = (cntr and 1) == 1
            val isBall = (cntr and 2) == 2
            val isPlayer0 = (cntr and 4) == 4
            val isMissile0 = (cntr and 8) == 8
            val isPlayer1 = (cntr and 16) == 16
            val isMissile1 = (cntr and 32) == 32

            // set up "display" sprites counting expected number of overlaps
            tia.writeRegister(TIAPIARegs.PF0, if (isPlayfield) 0xFF else 0)
            tia.writeRegister(TIAPIARegs.ENABL, if (isBall) 0xFF else 0)
            tia.writeRegister(TIAPIARegs.GRP0, if (isPlayer0) 0xFF else 0)
            tia.writeRegister(TIAPIARegs.ENAM0, if (isMissile0) 0xFF else 0)
            tia.writeRegister(TIAPIARegs.GRP1, if (isPlayer1) 0xFF else 0)
            tia.writeRegister(TIAPIARegs.ENAM1, if (isMissile1) 0xFF else 0)
            if (verbose) text += " $cntr playfield:$isPlayfield ball:$isBall player0:$isPlayer0 missile0:$isMissile0 player1:$isPlayer1 missile1:$isMissile1\n"

            // clear collisions then render scanline to get test results
            tia.writeRegister(TIAPIARegs.CXCLR, 0)
            tia.renderScanline()

            // check the CX registers and see if the appropriate bits have been set for the 15 tests always log fail
            // CXBLPF - Collision between ball and playfield
            var cxResult = tia.readRegister(TIAPIARegs.CXBLPF)
            val ballHitPlayfieldTest = (cxResult and 128 == 128) == ((cntr and 3) == 3)
            if (verbose or !ballHitPlayfieldTest) text += "  ball hit playfield test $cntr: ${ballHitPlayfieldTest}\n"

            // CXM0FB - Collision between missile0 and  ball or playfield
            cxResult= tia.readRegister(TIAPIARegs.CXM0FB)
            val missile0HitPlayfield = (cxResult and 128 == 128) == ((cntr and 9) == 9)
            if (verbose or !missile0HitPlayfield) text += "  Missile0 hit playfield test $cntr: ${missile0HitPlayfield}\n"
            val missile0HitBall = (cxResult and 64 == 64) == ((cntr and 10) == 10)
            if (verbose or !missile0HitBall) text += "  missile0 hit ball test $cntr: ${missile0HitBall}\n"

            // CXM1FB - Collision between missile1 and  ball or playfield
            cxResult= tia.readRegister(TIAPIARegs.CXM1FB)
            val missile1HitPlayfield = (cxResult and 128 == 128) == ((cntr and 33) == 33)
            if (verbose or !missile1HitPlayfield) text += "  Missile1 hit playfield test $cntr: ${missile1HitPlayfield}\n"
            val missile1HitBall = (cxResult and 64 == 64) == ((cntr and 34) == 34)
            if (verbose or !missile1HitBall) text += "  missile1 hit ball test $cntr: ${missile1HitBall}\n"

            // CXM0P - Collision between missile0 and  either player sprite
            cxResult= tia.readRegister(TIAPIARegs.CXM0P)
            val missile0HitPlayer0 = (cxResult and 128 == 128) == ((cntr and 12) == 12)
            if (verbose or !missile0HitPlayer0) text += "  Missile0 hit player0 test $cntr: ${missile0HitPlayer0}\n"
            val missile0HitPlayer1 = (cxResult and 64 == 64) == ((cntr and 24) == 24)
            if (verbose or !missile0HitPlayer1) text += "  missile0 hit player1 test $cntr: ${missile0HitPlayer1}\n"

            // CXM1P - Collision between missile1 and  either player sprite
            cxResult= tia.readRegister(TIAPIARegs.CXM1P)
            val missile1HitPlayer0 = (cxResult and 128 == 128) == ((cntr and 36) == 36)
            if (verbose or !missile1HitPlayer0) text += "  Missile1 hit player0 test $cntr: ${missile1HitPlayer0}\n"
            val missile1HitPlayer1 = (cxResult and 64 == 64) == ((cntr and 48) == 48)
            if (verbose or !missile1HitPlayer1) text += "  missile1 hit player1 test $cntr: ${missile1HitPlayer1}\n"

            // CXP0FB - Collision between player 0 and playfield or ball
            cxResult= tia.readRegister(TIAPIARegs.CXP0FB)
            val player0HitPlayfield = (cxResult and 128 == 128) == ((cntr and 5) == 5)
            if (verbose or !player0HitPlayfield) text += "  player0 hit playfield test $cntr: ${player0HitPlayfield}\n"
            val player0HitBall = (cxResult and 64 == 64) == ((cntr and 6) == 6)
            if (verbose or !player0HitBall) text += "  player0 hit ball test $cntr: ${player0HitBall}\n"

            // CXP1FB - Collision between player1 and playfield or ball
            cxResult= tia.readRegister(TIAPIARegs.CXP1FB)
            val player1HitPlayfield = (cxResult and 128 == 128) == ((cntr and 17) == 17)
            if (verbose or !player1HitPlayfield) text += "  player1 hit playfield test $cntr: ${player1HitPlayfield}\n"
            val player1HitBall = (cxResult and 64 == 64) == ((cntr and 18) == 18)
            if (verbose or !player1HitBall) text += "  player1 hit ball test $cntr: ${player1HitBall}\n"

            // CXPPMM - Collision between players or between missiles
            cxResult= tia.readRegister(TIAPIARegs.CXPPMM)
            val player0HitPlayer1 = (cxResult and 128 == 128) == ((cntr and 20) == 20)
            if (verbose or !player0HitPlayer1) text += "  players hit each other test $cntr: ${player0HitPlayer1}\n"
            val missile0HitMissile1 = (cxResult and 64 == 64) == ((cntr and 40) == 40)
            if (verbose or !missile0HitMissile1) text += "  Missiles hit each other test $cntr: ${missile0HitMissile1}\n"

        }

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

        text += runPMGTests(verbose)
        text += runPMGCollisionTests(verbose)
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

        for (cntr in 0..7) {
            val nusiz = if (cntr < 4) (cntr shl 4) or cntr else ((7 - cntr) shl 4) or cntr
            tia.writeRegister(TIAPIARegs.CTRLPF, nusiz and 0xFC)
            tia.writeRegister(TIAPIARegs.NUSIZ0, nusiz)
            tia.writeRegister(TIAPIARegs.NUSIZ1, nusiz)
            renderThenRasterize(tia, tiaImage.pixelWriter, cntr*8, 4)
            tia.writeRegister(TIAPIARegs.REFP0, 8)
            tia.writeRegister(TIAPIARegs.REFP1, 8)
            renderThenRasterize(tia, tiaImage.pixelWriter, cntr*8+4, 4)
            tia.writeRegister(TIAPIARegs.REFP0, 0)
            tia.writeRegister(TIAPIARegs.REFP1, 0)
        }

        for (cntr in 0..7) {
            val nusiz = if (cntr < 4) (cntr shl 4) or cntr else ((7 - cntr) shl 4) or cntr
            tia.writeRegister(TIAPIARegs.CTRLPF, nusiz and 0xFC)
            tia.writeRegister(TIAPIARegs.NUSIZ0, nusiz)
            tia.writeRegister(TIAPIARegs.NUSIZ1, nusiz)
            for (hmove in 0..15) {
                if (hmove == 8)
                    tia.writeRegister(TIAPIARegs.HMCLR, 0)
                else {
                    tia.writeRegister(TIAPIARegs.HMBL, hmove shl 4)
                    tia.writeRegister(TIAPIARegs.HMM0, hmove shl 4)
                    tia.writeRegister(TIAPIARegs.HMM1, hmove shl 4)
                    tia.writeRegister(TIAPIARegs.HMP0, hmove shl 4)
                    tia.writeRegister(TIAPIARegs.HMP1, hmove shl 4)
                }
                // note that hmove should be executed right after HSYNC started
                tia.writeRegister(TIAPIARegs.HMOVE, 0)
                renderThenRasterize(tia, tiaImage.pixelWriter, 64+cntr*16+hmove, 1)
            }
        }

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