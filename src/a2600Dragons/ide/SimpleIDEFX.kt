package a2600Dragons.ide

import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.VBox
import javafx.stage.Stage


class SimpleIDEFX  : Application() {

    override fun start(primaryStage: Stage?) {
        val tabs = TabPane()
        tabs.tabs.add(buildEditorTab())
        tabs.tabs.add(buildTIATab())
        tabs.tabs.add(buildDebuggerTab())

        val scene = Scene(tabs, 800.0, 600.0)

        primaryStage!!.title = "PolyCounter tests"
        primaryStage.scene = scene
        primaryStage.show()

    }


    // ******************* EDITOR ********************

    private fun buildEditorTab(): Tab {
        val console = TextArea("PolyCounter text-based tests")
        console.isWrapText = true
        console.prefRowCount = 100


        val verboseToggle = CheckBox("Show only long runs")
        val runTestButton = Button("Run Tests")
        runTestButton.setOnAction { event -> run{
//            console.text += consolePolyTests( ! verboseToggle.isSelected)
        } }
        val root = VBox(10.0, verboseToggle, runTestButton, console)
        val tab = Tab("Console")
        tab.content = root
        tab.isClosable = false
        return tab
    }


    // ********************* TIA Monitor *****************

    private fun buildTIATab(): Tab {
        val label = Label("NOT IMPLEMENTED YET")
        val root = VBox(10.0, label)
        val tab = Tab("TIA Monitor")
        tab.content = root
        tab.isClosable = false
        return tab
    }


    // ********************* DEBUGGER ************************

    private fun buildDebuggerTab(): Tab {
        val label = Label("NOT IMPLEMENTED YET")
        val root = VBox(10.0, label)
        val tab = Tab("Debugger")
        tab.content = root
        tab.isClosable = false
        return tab
    }
}

fun main(args: Array<String>) {
    Application.launch(SimpleIDEFX::class.java, *args)
}