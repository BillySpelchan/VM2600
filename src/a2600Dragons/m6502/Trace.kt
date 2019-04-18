package a2600Dragons.m6502

import a2600Dragons.a2600.Cartridge
import a2600Dragons.a2600.VM2600
import java.io.File
import kotlin.system.exitProcess


fun main(args: Array<String>) {
    var argIndex = 0
    var fileToAssemble: String? = null
    var fileToWrite: String? = null
    var verbose = false

    println("VM2600 command line by Billy D. Spelchan")
    while (argIndex < args.size) {
        when (args[argIndex]) {
            "-a" -> {
                ++argIndex
                if (argIndex >= args.size) {
                    println("Missing name of file to be assembled")
                    exitProcess(1)
                } else {
                    fileToAssemble = args[argIndex]
                    println("Assemble file: $fileToAssemble")
                    if (fileToWrite == null)
                        fileToWrite = "a.bin"
                }
            }

            "-o" -> {
                ++argIndex
                if (argIndex >= args.size) {
                    println("Missing name of file to be assembled")
                    exitProcess(1)
                } else {
                    fileToWrite = args[argIndex]
                    println("Output ROM to file: $fileToWrite")
                }
            }

            "-v" -> verbose = true;
            else -> println("Unknown command or option ${args[argIndex]}")
        }
        ++argIndex
    }

    var memoryManager = Cartridge()
    var m6502 = M6502( memoryManager )
    // right now assuming just single bank rom - multibank in future
    var byteData =  ByteArray(4096)

    // handle the commmands in appropriate order
    if (fileToAssemble != null) {
        println("TODO process the assembly language file provided")
        val assemblyFile = File(fileToAssemble)
        var assemblyList:ArrayList<String> = ArrayList(assemblyFile.readLines())
        if (verbose)
            for (line in assemblyList)
                println(line)
        var assembler = Assembler(m6502, verbose)
        assembler.assembleProgram(assemblyList)

        // note - in future add support for multibank assembly
        for (cntrRom in 0..assembler.currentBank.size-1) {
            byteData[cntrRom] = assembler.currentBank.readBankAddress(cntrRom).toByte()
            memoryManager.write(cntrRom, assembler.currentBank.readBankAddress(cntrRom))
        }
        VM2600(memoryManager).dump(0, 255)

        // run until break

        m6502.state.ip = 0
        val lastState = ProcessorState(0,0,0,0,0,0,0)
        lastState.copyState(m6502.state)
        var traceString = ""
        var ipAddress = m6502.state.ip
        while (memoryManager.read( ipAddress ) != 0) {
            traceString = "(" + m6502.state.tick +") " +
                    ipAddress.toString(16) + ":" +
                    m6502.disassembleStep(m6502.state.ip)
            m6502.step()
            ipAddress = m6502.state.ip
            if (m6502.state.acc != lastState.acc)
                traceString = traceString + " ; A:$" + lastState.acc + " -> $" + m6502.state.acc.toString(16)
            if (m6502.state.x != lastState.x)
                traceString = traceString + ", X:" + lastState.x + " -> $"  + m6502.state.x.toString(16)
            if (m6502.state.y != lastState.y)
                traceString = traceString + ", Y:" + lastState.y + " -> $"  + m6502.state.y.toString(16)
            if (m6502.state.flags != lastState.flags)
                traceString = traceString + ", Flags:"  + lastState.flags + " -> $" + m6502.state.flags.toString(16)
            println(traceString)
            lastState.copyState(m6502.state)
        }

    }
/*
    if (fileToWrite != null) {
        println("TODO writing ROM file ")
        val romFile = File(fileToWrite)
        romFile.writeBytes(byteData)
    }
*/
}
