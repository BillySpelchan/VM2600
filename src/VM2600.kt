import a2600Dragons.m6502.*
import a2600Dragons.a2600.*
import com.sun.javaws.exceptions.InvalidArgumentException
import java.io.File
import kotlin.system.exitProcess

fun printVerbose(s:String, v:Boolean ) {
    if (v)
        print(s);
}

fun printlnVerbose(s:String, v:Boolean ) {
    if (v)
        println(s);
}



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

    }

    if (fileToWrite != null) {
        println("TODO writing ROM file ")
        val romFile = File(fileToWrite)
        romFile.writeBytes(byteData)
    }
}
