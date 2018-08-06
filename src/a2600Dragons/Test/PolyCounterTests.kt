package a2600Dragons.Test

import javafx.application.Application
import javafx.event.Event
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.stage.Stage


class PolyChangeEvent(val polyFormula:Int) : Event(POLY_CHANGE_EVENT) { }

val POLY_CHANGE_EVENT = EventType<PolyChangeEvent>("POLYNOMIAL_CHANGE")

class PolynomialGroup : HBox() {
    var powerCheckboxes = arrayListOf<CheckBox>(
            CheckBox("^8"), CheckBox("^7"), CheckBox("^6"), CheckBox("^5"),
            CheckBox("^4"), CheckBox("^3"), CheckBox("^2"), CheckBox("^1")
    )
    init {
        for (cb in powerCheckboxes) {
            cb.setOnAction(::changeHandler)
            children.add(cb)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun changeHandler(e:Event) {
        fireEvent(PolyChangeEvent(getPolyFormula()))
    }

    private fun getPolyFormula():Int {
        var formula = 0
        var maskBit = 128
        for (cntr in 0..7) {
            if (powerCheckboxes[cntr].isSelected)
                formula = formula or maskBit
            maskBit = maskBit.shr(1)
        }

        return formula
    }
/*
    fun setPolyFormula(formula:Int) {
        var maskBit:Int = 128
        for (cntr in 0..7) {
            powerCheckboxes[cntr].isSelected = ((formula and maskBit) == maskBit)
            maskBit = maskBit.shr(1)
        }
    }
*/
}

class PolyCounterTests  : Application(), EventHandler<PolyChangeEvent>  {
    override fun handle(event: PolyChangeEvent?) {
        println("Received poly change event ${event?.polyFormula}")
    }

    override fun start(primaryStage: Stage?) {
        val tabs = TabPane()
        tabs.tabs.add(buildPolyTestTab())
        tabs.tabs.add(buildPolycounterConsoleTab())

        val scene = Scene(tabs, 800.0, 600.0)

        primaryStage!!.title = "PolyCounter tests"
        primaryStage.scene = scene
        primaryStage.show()

    }

    private fun nextPoly(polyCounter:Int, formula:Int) : Int {
        var maskBit = 1
        var poly = polyCounter
        var result = polyCounter
        for (cntr:Int in 0..7) {
            poly *= polyCounter
            if ((formula and maskBit) == maskBit) {
                print("^$maskBit = $poly, ")
                result += poly
            }
            maskBit = maskBit.shl(1)
        }

        ++result
        return (result and 0x1FF)
    }

    private fun drawPolygonCounter(pixelWriter: PixelWriter, formula:Int) {
        var number = 0
        var argbColor: Int
        for (cntrCol:Int in 0..511) {
            number = nextPoly(number, formula)
            println(number)
            for (cntrRow:Int in 0..511) {
                argbColor = if ((511 - cntrRow) > number) 0xFF000055.toInt() else 0xFFAAAA00.toInt()
                pixelWriter.setArgb(cntrCol, cntrRow, argbColor)
            }
        }
    }

    private fun buildPolyTestTab() : Tab {
        val message = Text("Click color to get palette index")

        val palette = WritableImage(512, 512)
        val pixelWriter = palette.pixelWriter
        drawPolygonCounter(pixelWriter, 85)

        val viewer = ImageView(palette)
        viewer.setOnMouseClicked { event: javafx.scene.input.MouseEvent? ->  run {
            val x:Int = if (event != null) event.x.toInt() / 16 else 0
            val y:Int = if (event != null) event.y.toInt() / 16 else 0
            val indx = (y * 16 + x) and 254
            message.text = "Clicked on pixel $indx"
        } }

        val polygroup = PolynomialGroup()
        polygroup.addEventHandler(POLY_CHANGE_EVENT, {e->run{println("event ${e.polyFormula}"); drawPolygonCounter(pixelWriter, e.polyFormula)}})
//        val root = VBox(10.0, message, viewer)
        val root = VBox(10.0, polygroup, viewer)
        root.alignment = Pos.CENTER

        val tab = Tab()
        tab.text = "Palette"
        tab.content = root
        tab.isClosable = false
        return tab

    }

    private fun consolePolyTests(verbose:Boolean):String {
        var text = "Polycounter run lengths\n"
        if ( ! verbose) text += "(Only long runs shown)\n"
        for (cntr:Int in 0..511) {
            var number = 0
            val generatedNumbers:ArrayList<Int> = ArrayList()
            do {
                generatedNumbers.add(number)
                number = nextPoly(number, cntr)
            } while ( ! generatedNumbers.contains(number))
            val runLength = generatedNumbers.size - generatedNumbers.indexOf(number)
            if ((verbose) or (runLength >= 256))
                text += "$cntr length is $runLength\n"
        }

        return text
    }

    private fun buildPolycounterConsoleTab():Tab {
        val console = TextArea("PolyCounter text-based tests")
        console.isWrapText = true
        console.prefRowCount = 100


        val verboseToggle = CheckBox("Show only long runs")
        val runTestButton = Button("Run Tests")
        runTestButton.setOnAction { event -> run{
            console.text += consolePolyTests( ! verboseToggle.isSelected)
        } }
        val root = VBox(10.0, verboseToggle, runTestButton, console)
        val tab = Tab("Console")
        tab.content = root
        tab.isClosable = false
        return tab
    }
}

fun main(args: Array<String>) {
    Application.launch(PolyCounterTests::class.java, *args)
}