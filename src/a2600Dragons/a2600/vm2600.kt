package a2600Dragons.a2600

import a2600Dragons.m6502.M6502
import a2600Dragons.m6502.MemoryManager

class Cartridge : MemoryManager {
    var mem = Array<Int>(0x6000) {n->n and 255}

    override fun read(address:Int):Int { return (mem[address] and 255)}
    override fun write(address:Int, n:Int) { mem[address] = n and 255}
}




class VM2600(cartridge:Cartridge) {
    var m6502 = M6502(cartridge)

    fun disassemble() {
        m6502.disassembleStep(m6502.state.ip)
    }

    fun runCommand() {
        m6502.runCommand()
    }

    fun dump(start:Int, end:Int, entriesPerLine:Int = 16) {
        var n:Int;
        var indx = start;
        var lineCount = 0;
        while (indx < end) {
            if (lineCount == 0)
                print("${indx.toString(16)}: ");
            n = m6502.mem.read(indx);
            if (n < 16) print ("0")
            print(n.toString(16));
            ++lineCount;
            if (lineCount == entriesPerLine) {
                println();
                lineCount = 0;
            } else
                print(", ");
            ++indx;
        }
        if (lineCount > 0)
            println()
    }
}
