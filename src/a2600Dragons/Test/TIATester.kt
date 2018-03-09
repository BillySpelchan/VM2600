package a2600Dragons.Test

import a2600Dragons.tia.TIAColors
import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.image.ImageView
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.Stage
import java.awt.event.MouseEvent

class TIATester : Application() {
    fun pixelBlock(pixelWriter: PixelWriter, x:Int, y:Int, w:Int, h:Int, argb:Int) {
        for (cntrRow:Int in 0..(h-1)) {
            for (cntrCol: Int in 0..(w - 1)) {
                pixelWriter.setArgb(x + cntrCol, y + cntrRow, argb)
            }
        }
    }

    override fun start(primaryStage: Stage?) {
        var btn = Button("Hello JavaFX")
        var message = Text("JavaFX in Kotlin!")

        var tv = WritableImage(256, 256)
        val pixelWriter = tv.pixelWriter

        val tiaColors = TIAColors()
        for (cntrRow:Int in 0..15) {
            for (cntrCol:Int in 0..15) {
                pixelBlock(pixelWriter, cntrCol * 16, cntrRow * 16, 16,16, tiaColors.getARGB(cntrRow*16+cntrCol))
            }
        }


        var viewer = ImageView(tv)
        viewer.setOnMouseClicked { event: javafx.scene.input.MouseEvent? ->  run {
            val x:Int = if (event != null) event.x.toInt() / 16 else 0
            val y:Int = if (event != null) event.y.toInt() / 16 else 0
            val indx = (y * 16 + x) and 254
            message.text = "Color byte value is $indx (\$${indx.toString(16)})"
        } }
        btn.setOnAction { message.text = "Hello World!" }


        var root = VBox(10.0, btn, message, viewer)
        root.alignment = Pos.CENTER

        var scene = Scene(root, 320.0, 320.0)

        primaryStage!!.title = "JavaFX test"
        primaryStage.scene = scene
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(TIATester::class.java, *args)
}