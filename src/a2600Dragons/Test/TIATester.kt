package a2600Dragons.Test

import a2600Dragons.tia.TIA
import a2600Dragons.tia.TIAColors
import a2600Dragons.tia.TIARegisters
import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
//import javafx.scene.control.Button
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
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

    fun rasterizeTIA(pixelWriter: PixelWriter, scanline:Int, data:Array<Int>) {
        for (cntr in 0..159)
            pixelWriter.setArgb(cntr, scanline, tiaColors.getARGB(data[cntr]))
    }

    override fun start(primaryStage: Stage?) {
//        var btn = Button("Hello JavaFX")
//        btn.setOnAction { message.text = "Hello World!" }
        val tabs = TabPane()
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
        for (cntr in 0..127) {
            tia.writeRegister(TIARegisters.COLUBK.address, cntr * 2)
            tia.renderScanline()
            rasterizeTIA(tiaImage.pixelWriter, cntr, tia.rasterLine)
        }
        for (cntr in 128..191)
            rasterizeTIA(tiaImage.pixelWriter, cntr, tia.rasterLine)

        val tiaView = ImageView(tiaImage)
        tiaView.scaleX = 4.0
        tiaView.scaleY = 2.0
        tiaView.isPreserveRatio = false
        val tab = Tab("Rainbow")
        tab.content = tiaView
        tab.isClosable = false
        return tab

    }
}

fun main(args: Array<String>) {
    Application.launch(TIATester::class.java, *args)
}