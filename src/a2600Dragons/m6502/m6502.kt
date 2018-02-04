package a2600Dragons.m6502

interface MemoryManager {
    fun read(address:Int):Int
    fun write(address:Int, n:Int)
}

enum class AddressMode {ABSOLUTE, ABSOLUTE_X, ABSOLUTE_Y, ACCUMULATOR, FUTURE_EXPANSION, IMMEDIATE, IMPLIED,
    INDIRECT, INDIRECT_X, INDIRECT_Y, RELATIVE, ZERO_PAGE, ZERO_PAGE_X, ZERO_PAGE_Y}

// Flag constants used by processor state
const val CARRY_FLAG = 1
const val ZERO_FLAG = 2
const val INTERRUPT_FLAG = 4
const val DECIMAL_FLAG = 8
const val BREAK_FLAG = 16
const val OVERFLOW_FLAG = 64
const val NEGATIVE_FLAG = 128

/**
 * Holds the Processors current state (registers and flags)
 */
data class ProcessorState(var acc:Int, var x:Int, var y:Int, var ip:Int, var flags:Int, var sp:Int, var tick:Long) {
    var ipNext = ip

    fun checkState(regmem:String, expected:Int, mem:MemoryManager? = null):Boolean {
        var reg = regmem.toUpperCase()
        var addr = 0
        if (reg.startsWith("M")) {
            addr = reg.substring(1).toInt(16)
            reg = "M"
        }

        return when (reg) {
            "A", "ACC" -> acc == expected
            "X" -> x == expected
            "Y" -> y == expected
            "IP" -> ip == expected
            "S", "SP" -> sp == expected
            "FLAGS" -> flags == expected
            "C" -> ((flags and CARRY_FLAG) > 0) == (expected > 0)
            "Z" -> ((flags and ZERO_FLAG) > 0) == (expected > 0)
            "I"-> ((flags and INTERRUPT_FLAG) > 0) == (expected > 0)
            "D"-> ((flags and DECIMAL_FLAG) > 0) == (expected > 0)
            "B"-> ((flags and BREAK_FLAG) > 0) == (expected > 0)
            "V"-> ((flags and OVERFLOW_FLAG) > 0) == (expected > 0)
            "N" -> ((flags and NEGATIVE_FLAG) > 0) == (expected > 0)
            "M" -> if (mem != null) mem.read(addr) == expected else false
            else -> {
                println("Error: unknown register or memory request $regmem.")
                false
            }
        }
    }
}

@Suppress("MemberVisibilityCanPrivate")
class M6502Instruction(val OPCode:Int, val OPString:String, val size:Int, val addressMode:AddressMode, val cycles:Int,
                       var command:(m6502:M6502)->Unit) {


    fun execute(m6502:M6502) {
        command(m6502)
    }
}


/** The 6502 processor emulation unit handles disassembly of an instruction as well as execution of an instruction
 * using the current virtual state of the processor. Work is done by using a table of operations with a lamda function
 * for performing the execution.
 */
class M6502(var mem:MemoryManager, var stackPage:Int = 1) {
    var state = ProcessorState(0,0,0,0,32,0,0)
    // array of instructions indexed by op code
    val commands = arrayOf(
            M6502Instruction(0x00, "BRK", 1,AddressMode.IMPLIED, 7, {mach->mach.notImplemented()}),
            M6502Instruction(0x01, "ORA",2, AddressMode.INDIRECT_X, 6, {mach->mach.notImplemented()}),
            M6502Instruction(0x02, "X02", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x03, "X03", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x04, "X04", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x05, "ORA",2, AddressMode.ZERO_PAGE, 6, {m->m.notImplemented()}),
            M6502Instruction(0x06, "ASL",2, AddressMode.ZERO_PAGE, 5, {m->m.notImplemented()}),
            M6502Instruction(0x07, "X07", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x08, "PHP",1, AddressMode.IMPLIED, 3, {_->
                pushByteOnStack(state.flags)
            }),
            M6502Instruction(0x09, "ORA",2, AddressMode.IMMEDIATE, 2, {m->m.notImplemented()}),
            M6502Instruction(0x0A, "ASL",1, AddressMode.ACCUMULATOR, 2, {m->m.notImplemented()}),
            M6502Instruction(0x0B, "X0B", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x0C, "X0C", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x0D, "ORA",3, AddressMode.ABSOLUTE, 4, {m->m.notImplemented()}),
            M6502Instruction(0x0E, "ASL",3, AddressMode.ABSOLUTE, 6, {m->m.notImplemented()}),
            M6502Instruction(0x0F, "X0F", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0x10, "BPL",2, AddressMode.RELATIVE, 2, {_->
                processBranch(state.flags and NEGATIVE_FLAG != NEGATIVE_FLAG, mem.read(state.ip+1) )
            }),
            M6502Instruction(0x11, "ORA",2, AddressMode.INDIRECT_Y, 5, {m->m.notImplemented()}),
            M6502Instruction(0x12, "X12", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x13, "X13", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x14, "X14", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x15, "ORA",2, AddressMode.ZERO_PAGE_X, 4, {m->m.notImplemented()}),
            M6502Instruction(0x16, "ASL",2, AddressMode.ZERO_PAGE_X, 6, {m->m.notImplemented()}),
            M6502Instruction(0x17, "X17", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x18, "CLC",1, AddressMode.IMPLIED, 2, {m->
                run {
                    m.state.flags = m.state.flags and (255 xor CARRY_FLAG)
                }
            }),
            M6502Instruction(0x19, "ORA",3, AddressMode.ABSOLUTE_Y, 4, {m->m.notImplemented()}),
            M6502Instruction(0x1A, "X1A", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x1B, "X1B", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x1C, "X1C", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x1D, "ORA",3, AddressMode.ABSOLUTE_X, 4, {m->m.notImplemented()}),
            M6502Instruction(0x1E, "ASL",3, AddressMode.ABSOLUTE_X, 7, {m->m.notImplemented()}),
            M6502Instruction(0x1F, "X1F", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0x20, "JSR",3, AddressMode.ABSOLUTE, 6, {m->m.notImplemented()}),
            M6502Instruction(0x21, "AND",2, AddressMode.INDIRECT_X, 6, {m->m.notImplemented()}),
            M6502Instruction(0x22, "X22", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x23, "X23", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x24, "BIT",2, AddressMode.ZERO_PAGE, 3, {m->m.notImplemented()}),
            M6502Instruction(0x25, "AND",2, AddressMode.ZERO_PAGE, 3, {m->m.notImplemented()}),
            M6502Instruction(0x26, "ROL",2, AddressMode.ZERO_PAGE, 5, {m->m.notImplemented()}),
            M6502Instruction(0x27, "X27", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x28, "PLP",1, AddressMode.IMPLIED, 4, {_->
                state.flags = pullByteFromStack()
            }),
            M6502Instruction(0x29, "AND",2, AddressMode.IMMEDIATE, 2, {m->m.notImplemented()}),
            M6502Instruction(0x2A, "ROL",1, AddressMode.ACCUMULATOR, 2, {m->m.notImplemented()}),
            M6502Instruction(0x2B, "X2B", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x2C, "BIT",3, AddressMode.ABSOLUTE, 4, {m->m.notImplemented()}),
            M6502Instruction(0x2D, "AND",3, AddressMode.ABSOLUTE, 4, {m->m.notImplemented()}),
            M6502Instruction(0x2E, "ROL",3, AddressMode.ABSOLUTE, 6, {m->m.notImplemented()}),
            M6502Instruction(0x2F, "X2F", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0x30, "BMI",2, AddressMode.RELATIVE, 2, {_->
                processBranch(state.flags and NEGATIVE_FLAG == NEGATIVE_FLAG, mem.read(state.ip+1) )
            }),
            M6502Instruction(0x31, "AND",2, AddressMode.INDIRECT_Y, 5, {m->m.notImplemented()}),
            M6502Instruction(0x32, "X32", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x33, "X33", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x34, "X34", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x35, "AND",2, AddressMode.ZERO_PAGE_X, 4, {m->m.notImplemented()}),
            M6502Instruction(0x36, "ROL",2, AddressMode.ZERO_PAGE_X, 6, {m->m.notImplemented()}),
            M6502Instruction(0x37, "X37", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x38, "SEC",1, AddressMode.IMPLIED, 2, {m->
                run {
                    m.state.flags = m.state.flags or CARRY_FLAG
                }
            }),
            M6502Instruction(0x39, "AND",3, AddressMode.ABSOLUTE_Y, 4, {m->m.notImplemented()}),
            M6502Instruction(0x3A, "X3A", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x3B, "X3B", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x3C, "X3C", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x3D, "AND",3, AddressMode.ABSOLUTE_X, 4, {m->m.notImplemented()}),
            M6502Instruction(0x3E, "ROL",3, AddressMode.ABSOLUTE_X, 7, {m->m.notImplemented()}),
            M6502Instruction(0x3F, "X3F", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0x40, "RTI",1, AddressMode.IMPLIED, 6, {m->m.notImplemented()}),
            M6502Instruction(0x41, "EOR",2, AddressMode.INDIRECT_X, 6, {m->m.notImplemented()}),
            M6502Instruction(0x42, "X42", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x43, "X43", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x44, "X44", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x45, "EOR",2, AddressMode.ZERO_PAGE, 3, {m->m.notImplemented()}),
            M6502Instruction(0x46, "LSR",2, AddressMode.ZERO_PAGE, 5, {m->m.notImplemented()}),
            M6502Instruction(0x47, "X47", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x48, "PHA",1, AddressMode.IMPLIED, 3, {_->
                pushByteOnStack(state.acc)
            }),
            M6502Instruction(0x49, "EOR",2, AddressMode.IMMEDIATE, 2, {m->m.notImplemented()}),
            M6502Instruction(0x4A, "LSR",1, AddressMode.ACCUMULATOR, 2, {m->m.notImplemented()}),
            M6502Instruction(0x4B, "X4B", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x4C, "JMP",3, AddressMode.ABSOLUTE, 3, {m->m.notImplemented()}),
            M6502Instruction(0x4D, "EOR",3, AddressMode.ABSOLUTE, 4, {m->m.notImplemented()}),
            M6502Instruction(0x4E, "LSR",3, AddressMode.ABSOLUTE, 6, {m->m.notImplemented()}),
            M6502Instruction(0x4F, "X42", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0x50, "BVC",2, AddressMode.RELATIVE, 2, {m->m.notImplemented()}),
            M6502Instruction(0x51, "EOR",2, AddressMode.INDIRECT_Y, 5, {m->m.notImplemented()}),
            M6502Instruction(0x52, "X52", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x53, "X53", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x54, "X54", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x55, "EOR",2, AddressMode.ZERO_PAGE_X, 4, {m->m.notImplemented()}),
            M6502Instruction(0x56, "LSR",2, AddressMode.ZERO_PAGE_X, 6, {m->m.notImplemented()}),
            M6502Instruction(0x57, "X57", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x58, "CLI",1, AddressMode.IMPLIED, 2, {m->
                run {
                    m.state.flags = m.state.flags and (255 xor INTERRUPT_FLAG)
                } }),
            M6502Instruction(0x59, "EOR",3, AddressMode.ABSOLUTE_Y, 4, {m->m.notImplemented()}),
            M6502Instruction(0x5A, "X5A", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x5B, "X5B", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x5C, "X5C", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x5D, "EOR",3, AddressMode.ABSOLUTE_X, 4, {m->m.notImplemented()}),
            M6502Instruction(0x5E, "LSR",3, AddressMode.ABSOLUTE_X, 7, {m->m.notImplemented()}),
            M6502Instruction(0x5F, "X5F", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0x60, "RTS",1, AddressMode.IMPLIED, 6, {m->m.notImplemented()}),
            M6502Instruction(0x61, "ADC",2, AddressMode.INDIRECT_X, 6, {_->
                state.acc = performAdd(mem.read(findAbsoluteAddress(((mem.read(state.ip+1)+state.x) and 255)-1)), state.acc)
            }),
            M6502Instruction(0x62, "X62", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x63, "X63", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x64, "X64", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x65, "ADC",2, AddressMode.ZERO_PAGE, 3, {_->
                state.acc = performAdd(mem.read(mem.read(state.ip+1)), state.acc)
            }),
            M6502Instruction(0x66, "ROR",2, AddressMode.ZERO_PAGE, 5, {m->m.notImplemented()}),
            M6502Instruction(0x67, "X67", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x68, "PLA",1, AddressMode.IMPLIED, 4, {_->
                state.acc = pullByteFromStack(true)
            }),
            M6502Instruction(0x69, "ADC",2, AddressMode.IMMEDIATE, 2, {_->
                state.acc = performAdd(state.acc, mem.read(state.ip+1))
            }),
            M6502Instruction(0x6A, "ROR",1, AddressMode.ACCUMULATOR, 2, {m->m.notImplemented()}),
            M6502Instruction(0x6B, "X6B", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x6C, "JMP",3, AddressMode.INDIRECT, 5, {m->m.notImplemented()}),
            M6502Instruction(0x6D, "ADC",3, AddressMode.ABSOLUTE, 4, {_->
                state.acc = performAdd(mem.read(findAbsoluteAddress(state.ip)), state.acc)
            }),
            M6502Instruction(0x6E, "ROR",3, AddressMode.ABSOLUTE, 6, {m->m.notImplemented()}),
            M6502Instruction(0x6F, "X6F", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0x70, "BVS",2, AddressMode.RELATIVE, 2, {m->m.notImplemented()}),
            M6502Instruction(0x71, "ADC",2, AddressMode.INDIRECT_Y, 5, {_->
                state.acc = performAdd(mem.read(findAbsoluteAddress(mem.read(state.ip+1) -1) + state.y), state.acc)
            }),
            M6502Instruction(0x72, "X72", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x73, "X73", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x74, "X74", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x75, "ADC",2, AddressMode.ZERO_PAGE_X, 4, {_->
                state.acc = performAdd(mem.read(mem.read(state.ip+1) + state.x), state.acc)
            }),
            M6502Instruction(0x76, "ROR",2, AddressMode.ZERO_PAGE_X, 6, {m->m.notImplemented()}),
            M6502Instruction(0x77, "X77", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x78, "SEI",1, AddressMode.IMPLIED, 2, {m->
                run {
                    m.state.flags = m.state.flags or INTERRUPT_FLAG
                } }),
            M6502Instruction(0x79, "ADC",3, AddressMode.ABSOLUTE_Y, 4, {_->
                state.acc = performAdd(mem.read(findAbsoluteAddress(state.ip) + state.y), state.acc)
            }),
            M6502Instruction(0x7A, "X7A", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x7B, "X7B", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x7C, "X7C", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x7D, "ADC",3, AddressMode.ABSOLUTE_X, 4, {_->
                state.acc = performAdd(mem.read(findAbsoluteAddress(state.ip)+state.x), state.acc)
            }),
            M6502Instruction(0x7E, "ROR",3, AddressMode.ABSOLUTE_X, 7, {m->m.notImplemented()}),
            M6502Instruction(0x7F, "X7F", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0x80, "X80", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x81, "STA",2, AddressMode.INDIRECT_X, 6, {_->
                mem.write(findAbsoluteAddress(((mem.read(state.ip+1)+state.x) and 255)-1), state.acc) }),
            M6502Instruction(0x82, "X82", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x83, "X83", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x84, "STY",2, AddressMode.ZERO_PAGE, 3, {_->
                mem.write(mem.read(state.ip+1), state.y)
            }),
            M6502Instruction(0x85, "STA",2, AddressMode.ZERO_PAGE, 3, {m->
                mem.write(mem.read(state.ip+1), state.acc)
            }),
            M6502Instruction(0x86, "STX",2, AddressMode.ZERO_PAGE, 3, {_->
                mem.write(mem.read(state.ip+1), state.x)
            }),
            M6502Instruction(0x87, "X87", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x88, "DEY",1, AddressMode.IMPLIED, 2, {_->
                state.y = setNumberFlags((state.y-1) and 255)
            }),
            M6502Instruction(0x89, "X89", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x8A, "TXA",1, AddressMode.IMPLIED, 2, {_->
                state.acc = setNumberFlags(state.x)
            }),
            M6502Instruction(0x8B, "X8B", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x8C, "STY",3, AddressMode.ABSOLUTE, 4, {_->
                mem.write(findAbsoluteAddress(state.ip), state.y)
            }),
            M6502Instruction(0x8D, "STA",3, AddressMode.ABSOLUTE, 4, {_->
                mem.write(findAbsoluteAddress(state.ip), state.acc) }),
            M6502Instruction(0x8E, "STX",3, AddressMode.ABSOLUTE, 4, {_->
                mem.write(findAbsoluteAddress(state.ip), state.x)
            }),
            M6502Instruction(0x8F, "X8F", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0x90, "BCC",2, AddressMode.RELATIVE, 2, {m->m.notImplemented()}),
            M6502Instruction(0x91, "STA",2, AddressMode.INDIRECT_Y, 6, {_->
                mem.write(findAbsoluteAddress(mem.read(state.ip+1) -1) + state.y, state.acc) }),
            M6502Instruction(0x92, "X92", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x93, "X93", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x94, "STY",2, AddressMode.ZERO_PAGE_X, 4, {_->
                mem.write(mem.read(state.ip+1) + state.x, state.y)
            }),
            M6502Instruction(0x95, "STA",2, AddressMode.ZERO_PAGE_X, 4, {_->
                mem.write(mem.read(state.ip+1) + state.x, state.acc)}),
            M6502Instruction(0x96, "STX",2, AddressMode.ZERO_PAGE_Y, 4, {_->
                mem.write(mem.read(state.ip+1) + state.y, state.x)
            }),
            M6502Instruction(0x97, "X97", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x98, "TYA",1, AddressMode.IMPLIED, 2, {_->
                state.acc = setNumberFlags(state.y)
            }),
            M6502Instruction(0x99, "STA",3, AddressMode.ABSOLUTE_Y, 5, {_->
                mem.write(findAbsoluteAddress(state.ip)+state.y, state.acc) }),
            M6502Instruction(0x9A, "TXS",1, AddressMode.IMPLIED, 2, {_->
                state.sp = state.x
            }),
            M6502Instruction(0x9B, "X9B", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x9C, "X9C", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x9D, "STA",3, AddressMode.ABSOLUTE_X, 5, {_->
                mem.write(findAbsoluteAddress(state.ip)+state.x, state.acc)}),
            M6502Instruction(0x9E, "X9E", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0x9F, "X9F", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0xA0, "LDY",2, AddressMode.IMMEDIATE, 2, {m->run {
                m.state.y = m.loadByteFromAddress(m.state.ip, 1)
            } }),
            M6502Instruction(0xA1, "LDA",2, AddressMode.INDIRECT_X, 6, {m->
                m.state.acc = m.loadByteFromAddress(m.findAbsoluteAddress(
                        // need to deduct 1 from loadByteFromAddress method as it is designed to skip opcode
                        ((m.loadByteFromAddress(m.state.ip, 1)+m.state.x) and 255)-1))
            }),
            M6502Instruction(0xA2, "LDX",2, AddressMode.IMMEDIATE, 2, {m->run {
                m.state.x = m.loadByteFromAddress(m.state.ip, 1)
            } }),
            M6502Instruction(0xA3, "XA3", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xA4, "LDY",2, AddressMode.ZERO_PAGE, 3, {m->run {
                m.state.y = m.loadByteFromAddress(m.loadByteFromAddress(m.state.ip, 1))
            } }),
            M6502Instruction(0xA5, "LDA",2, AddressMode.ZERO_PAGE, 3, {m->run {
                m.state.acc = m.loadByteFromAddress(m.loadByteFromAddress(m.state.ip, 1))
            } }),
            M6502Instruction(0xA6, "LDX",2, AddressMode.ZERO_PAGE, 3, {m->run {
                m.state.x = m.loadByteFromAddress(m.loadByteFromAddress(m.state.ip, 1))
            } }),
            M6502Instruction(0xA7, "XA7", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xA8, "TAY",1, AddressMode.IMPLIED, 2, {_->
                state.y = setNumberFlags(state.acc)
            }),
            M6502Instruction(0xA9, "LDA",2, AddressMode.IMMEDIATE, 2, {m->run {
                m.state.acc = m.loadByteFromAddress(m.state.ip, 1)
            } }),
            M6502Instruction(0xAA, "TAX",1, AddressMode.IMPLIED, 2, {_->
                state.x = setNumberFlags(state.acc)
            }),
            M6502Instruction(0xAB, "XAB", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xAC, "LDY",3, AddressMode.ABSOLUTE, 4, {m->run {
                m.state.y = m.loadByteFromAddress(m.findAbsoluteAddress(m.state.ip))
            } }),
            M6502Instruction(0xAD, "LDA",3, AddressMode.ABSOLUTE, 4, {m->run {
                m.state.acc = m.loadByteFromAddress(m.findAbsoluteAddress(m.state.ip))
            } }),
            M6502Instruction(0xAE, "LDX",3, AddressMode.ABSOLUTE, 4, {m->run {
                m.state.x = m.loadByteFromAddress(m.findAbsoluteAddress(m.state.ip))
            } }),
            M6502Instruction(0xAF, "XAF", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0xB0, "BCS",2, AddressMode.RELATIVE, 2, {m->m.notImplemented()}),
            M6502Instruction(0xB1, "LDA",2, AddressMode.INDIRECT_Y, 5, {m->
                m.state.acc = m.loadByteFromAddress(m.findAbsoluteAddress(
                        // need to deduct 1 from loadByteFromAddress method as it is designed to skip opcode
                        (m.loadByteFromAddress(m.state.ip, 1)-1)), m.state.y, true)
            }),
            M6502Instruction(0xB2, "XB2", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xB3, "XB3", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xB4, "LDY",2, AddressMode.ZERO_PAGE_X, 4, {m->run {
                m.state.y = m.loadByteFromAddress(m.loadByteFromAddress(m.state.ip, 1), m.state.x)
            } }),
            M6502Instruction(0xB5, "LDA",2, AddressMode.ZERO_PAGE_X, 4, {m->run {
                m.state.acc = m.loadByteFromAddress(m.loadByteFromAddress(m.state.ip, 1), m.state.x)
            } }),
            M6502Instruction(0xB6, "LDX",2, AddressMode.ZERO_PAGE_Y, 4, {m->run {
                m.state.x = m.loadByteFromAddress(m.loadByteFromAddress(m.state.ip, 1), m.state.y)
            } }),
            M6502Instruction(0xB7, "XB7", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xB8, "CLV",1, AddressMode.IMPLIED, 2, {m->
                run {
                    m.state.flags = m.state.flags and (255 xor OVERFLOW_FLAG)
                } }),
            M6502Instruction(0xB9, "LDA",3, AddressMode.ABSOLUTE_Y, 4, {	m->run {
                m.state.acc = m.loadByteFromAddress(m.findAbsoluteAddress(m.state.ip), m.state.y, true)
            }}),
            M6502Instruction(0xBA, "TSX",1, AddressMode.IMPLIED, 2, {_->
                state.x = setNumberFlags(state.sp)
            }),
            M6502Instruction(0xBB, "XBB", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xBC, "LDY",3, AddressMode.ABSOLUTE_X, 4, {	m->run {
                m.state.y = m.loadByteFromAddress(m.findAbsoluteAddress(m.state.ip), m.state.x, true)
            }}),
            M6502Instruction(0xBD, "LDA",3, AddressMode.ABSOLUTE_X, 4, {m->run {
                m.state.acc = m.loadByteFromAddress(m.findAbsoluteAddress(m.state.ip), m.state.x, true)
            } }),
            M6502Instruction(0xBE, "LDX",3, AddressMode.ABSOLUTE_Y, 4, {	m->run {
                m.state.x = m.loadByteFromAddress(m.findAbsoluteAddress(m.state.ip), m.state.y, true)
            }}),
            M6502Instruction(0xBF, "XBF", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0xC0, "CPY",2, AddressMode.IMMEDIATE, 2, {m->m.notImplemented()}),
            M6502Instruction(0xC1, "CMP",2, AddressMode.INDIRECT_X, 6, {m->m.notImplemented()}),
            M6502Instruction(0xC2, "XC2", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xC3, "XC3", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xC4, "CPY",2, AddressMode.ZERO_PAGE, 3, {m->m.notImplemented()}),
            M6502Instruction(0xC5, "CMP",2, AddressMode.ZERO_PAGE, 3, {m->m.notImplemented()}),
            M6502Instruction(0xC6, "DEC",2, AddressMode.ZERO_PAGE, 3, {_-> run {
                val addressToDec = mem.read(state.ip+1)
                mem.write(addressToDec, setNumberFlags((mem.read(addressToDec) - 1) and 255))
            }}),
            M6502Instruction(0xC7, "XC7", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xC8, "INY",1, AddressMode.IMPLIED, 2, {_->
                state.y = setNumberFlags((state.y+1) and 255)
            }),
            M6502Instruction(0xC9, "CMP",2, AddressMode.IMMEDIATE, 2, {m->m.notImplemented()}),
            M6502Instruction(0xCA, "DEX",1, AddressMode.IMPLIED, 2, {m->
                state.x = setNumberFlags((state.x-1) and 255)
            }),
            M6502Instruction(0xCB, "XCB", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xCC, "CPY",3, AddressMode.ABSOLUTE, 4, {m->m.notImplemented()}),
            M6502Instruction(0xCD, "CMP",3, AddressMode.ABSOLUTE, 4, {m->m.notImplemented()}),
            M6502Instruction(0xCE, "DEC",3, AddressMode.ABSOLUTE, 4, {_->run{
                val addressToDec = findAbsoluteAddress(state.ip)
                mem.write(addressToDec, setNumberFlags((mem.read(addressToDec) - 1) and 255))
            }}),
            M6502Instruction(0xCF, "XCF", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0xD0, "BNE",2, AddressMode.RELATIVE, 2, {_->
                processBranch(state.flags and ZERO_FLAG != ZERO_FLAG, mem.read(state.ip+1) )
            }),
            M6502Instruction(0xD1, "CMP",2, AddressMode.INDIRECT_Y, 5, {m->m.notImplemented()}),
            M6502Instruction(0xD2, "XD2", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xD3, "XD3", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xD4, "XD4", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xD5, "CMP",2, AddressMode.ZERO_PAGE_X, 4, {m->m.notImplemented()}),
            M6502Instruction(0xD6, "DEC",2, AddressMode.ZERO_PAGE_X, 4, {_->run {
                val addressToDec = mem.read(state.ip+1) + state.x
                mem.write(addressToDec, setNumberFlags((mem.read(addressToDec) - 1) and 255))
            }}),
            M6502Instruction(0xD7, "XD7", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->futureExpansion()}),
            M6502Instruction(0xD8, "CLD",1, AddressMode.IMPLIED, 2, {m->
                run {
                    m.state.flags = m.state.flags and (255 xor DECIMAL_FLAG)
                }}),
            M6502Instruction(0xD9, "CMP",3, AddressMode.ABSOLUTE_Y, 4, {m->m.notImplemented()}),
            M6502Instruction(0xDA, "XDA", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xDB, "XDB", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xDC, "XDC", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xDD, "CMP",3, AddressMode.ABSOLUTE_X, 4, {m->m.notImplemented()}),
            M6502Instruction(0xDE, "DEC",3, AddressMode.ABSOLUTE_X, 4, {_->run{
                val addressToDec = findAbsoluteAddress(state.ip) + state.x
                mem.write(addressToDec, setNumberFlags((mem.read(addressToDec) - 1) and 255))
            }}),
            M6502Instruction(0xDF, "XDF", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0xE0, "CPX",2, AddressMode.IMMEDIATE, 2, {m->m.notImplemented()}),
            M6502Instruction(0xE1, "SBC",2, AddressMode.INDIRECT_X, 6, {m->m.notImplemented()}),
            M6502Instruction(0xE2, "XE2", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xE3, "XE3", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xE4, "CPX",2, AddressMode.ZERO_PAGE, 3, {m->m.notImplemented()}),
            M6502Instruction(0xE5, "SBC",2, AddressMode.ZERO_PAGE, 3, {m->m.notImplemented()}),
            M6502Instruction(0xE6, "INC",2, AddressMode.ZERO_PAGE, 3, {_-> run {
                    val addressToInc = mem.read(state.ip+1)
                    mem.write(addressToInc, setNumberFlags((mem.read(addressToInc) + 1) and 255))
                }
            }),
            M6502Instruction(0xE7, "XE7", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xE8, "INX",1, AddressMode.IMPLIED, 2, {_->
                state.x = setNumberFlags((state.x+1) and 255)
            }),
            M6502Instruction(0xE9, "SBC",2, AddressMode.IMMEDIATE, 2, {m->m.notImplemented()}),
            M6502Instruction(0xEA, "NOP",1, AddressMode.IMPLIED, 2, {m->m.notImplemented()}),
            M6502Instruction(0xEB, "XEB", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xEC, "CPX",3, AddressMode.ABSOLUTE, 4, {m->m.notImplemented()}),
            M6502Instruction(0xED, "SBC",3, AddressMode.ABSOLUTE, 4, {m->m.notImplemented()}),
            M6502Instruction(0xEE, "INC",3, AddressMode.ABSOLUTE, 4, {_-> run {
                val addressToInc = findAbsoluteAddress(state.ip)
                mem.write(addressToInc, setNumberFlags((mem.read(addressToInc) + 1) and 255))
            }}),
            M6502Instruction(0xEF, "XEF", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),

            M6502Instruction(0xF0, "BEQ",2, AddressMode.RELATIVE, 2, {_->
                processBranch(state.flags and ZERO_FLAG == ZERO_FLAG, mem.read(state.ip+1) )
            }),
            M6502Instruction(0xF1, "SBC",2, AddressMode.INDIRECT_Y, 5, {m->m.notImplemented()}),
            M6502Instruction(0xF2, "XF2", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xF3, "XF3", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xF4, "XF4", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xF5, "SBC",2, AddressMode.ZERO_PAGE_X, 4, {m->m.notImplemented()}),
            M6502Instruction(0xF6, "INC",2, AddressMode.ZERO_PAGE_X, 4, {_->run {
                val addressToInc = mem.read(state.ip + 1) + state.x
                mem.write(addressToInc, setNumberFlags((mem.read(addressToInc) + 1) and 255))
            } }),
            M6502Instruction(0xF7, "XF2", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xF8, "SED",1, AddressMode.IMPLIED, 2, {m->
                run {
                    m.state.flags = m.state.flags or DECIMAL_FLAG
                }}),
            M6502Instruction(0xF9, "SBC",3, AddressMode.ABSOLUTE_Y, 4, {m->m.notImplemented()}),
            M6502Instruction(0xFA, "XFA", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xFB, "XFB", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xFC, "XFC", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()}),
            M6502Instruction(0xFD, "SBC",3, AddressMode.ABSOLUTE_X, 4, {m->m.notImplemented()}),
            M6502Instruction(0xFE, "INC",3, AddressMode.ABSOLUTE_X, 4, {_->run{
                val addressToInc = findAbsoluteAddress(state.ip) + state.x
                mem.write(addressToInc, setNumberFlags((mem.read(addressToInc) + 1) and 255))
            }}),
            M6502Instruction(0xFF, "XF2", 1 , AddressMode.FUTURE_EXPANSION, 6, {m->m.futureExpansion()})

    )

    /** This is a placeholder function called by future expansion operations so that if such an opcode is ever executed
     * it will be notificed so that in the future the invalid instruction can be implemented
     */
    private fun futureExpansion() {
        println("WARNING Future Expansion OPeration $${mem.read(state.ip)} called at ${state.ip} ")
    }

    /** During the development of the emulator, not all instructions will be available so this is used to indicate that
     * the given instruction has yet to be completed.
     */
    private fun notImplemented() {
        val ip = state.ip
//        val inst = commands[mem.read(ip)]
        println("$ip : ${disassembleStep(ip)} has not yet been implemented")
    }


    /** Utility function that sets a particular flag to a particular boolan value */
    private fun adjustFlag(flag:Int, isSet:Boolean) {
        if (isSet)
            state.flags = state.flags or flag
        else
            state.flags = state.flags and (255 xor flag)
    }

    /* Utility function for checking if an offset address crosses page bounds and adjusts ticks accordingly */
    private fun pageBoundsCheck(baseAddress:Int, targetAddress:Int) {
        val basePage =  baseAddress / 256
        val actualPage = targetAddress / 256
        if (basePage != actualPage)
            ++state.tick
    }


    /** Utility function to calculate the address that an instruction is referring to. The 6502 uses little endian so
     * the low order byte  is followed by the high order (page)
     */
    private fun findAbsoluteAddress(address:Int):Int {
        return (mem.read(address+2) * 256 + mem.read(address+1))
    }


    /** Sets zero and negative flags to match particular number and returns that number for chaining */
    fun setNumberFlags(num:Int):Int {
        adjustFlag (ZERO_FLAG, num == 0)
        adjustFlag (NEGATIVE_FLAG, (num and 128) > 0)
        return num
    }

    /** Loads a byte from the indicated address setting the zero and negative flags if appropriate. */
    fun loadByteFromAddress(address:Int, offset:Int = 0, checkBounds:Boolean = false):Int {
        val result:Int = mem.read(address + offset)
        adjustFlag (ZERO_FLAG, result == 0)
        adjustFlag (NEGATIVE_FLAG, (result and 128) > 0)
        if (checkBounds)
            pageBoundsCheck(address, address+offset)

        return result
    }

    /** write indicated byte to stack adjusting stack pointer */
    fun pushByteOnStack(num:Int) {
        val stackAddress = stackPage * 256 + state.sp
        state.sp = (state.sp - 1) and 255
        mem.write(stackAddress, num)
    }

    /** Read byte from stack while adjusting stack pointer */
    fun pullByteFromStack(adjustFlags:Boolean = false):Int {
        state.sp = (state.sp +1) and 255
        val num = mem.read(stackPage * 256 + state.sp)
        if (adjustFlags)
            setNumberFlags(num)
        return num
    }

    /** interpreter aid that takes result of condition check and branches accordingly adding cycles if
     * branch taken and more cycles if page boundary crossed during branch
      */
    fun processBranch(check:Boolean, offset:Int) {
        if (check) {
            ++state.tick
            val adjOff = if (offset > 127) -( (offset xor 255)+1) else offset
            val target = state.ipNext + adjOff
            pageBoundsCheck(state.ipNext, target)
            state.ipNext = target
        }
    }

    /** convert a binary coded decimal number into a proper binary number */
    private fun BCDtoBinary(bcdNum:Int):Int {
        val lowNibble = (bcdNum and 15) % 10
        val highNibble = ((bcdNum ushr 4) and 15) % 10
        return highNibble * 10 + lowNibble
    }

    /** converts a binary number into the equivalent Binary Coded Decimal (carry if > 99) */
    private fun binaryToBCD(bin:Int):Int {
        adjustFlag(CARRY_FLAG, bin > 99)
        val lowNibble = bin % 10
        val highNibble = (bin / 10) % 10
        return (highNibble shl 4) or lowNibble
    }

    private fun performAdd(first:Int, second:Int, ignoreBCD:Boolean = false):Int {
        if ( ( ! ignoreBCD ) and ((state.flags and DECIMAL_FLAG) == DECIMAL_FLAG) ) {
            return binaryToBCD(performAdd(BCDtoBinary(first), BCDtoBinary(second), true))
        }
        // if we reach here we are now in binary mode
        var addition = first + second
        if ((state.flags and CARRY_FLAG) == CARRY_FLAG)
            ++addition

        // set nev state of carry flag
        adjustFlag (CARRY_FLAG, addition > 255)
        // zero and negative flags
        addition = setNumberFlags(addition and 255)
        // overflow flag is a bit of work
        val firstNeg = (first and 128) == 128
        val secondNeg = (second and 128) == 128
        val resultNeg = (addition and 128) == 128
        adjustFlag (OVERFLOW_FLAG, (firstNeg and secondNeg) xor resultNeg)

        return addition
    }

    /** Create a dissaembly of the instruction that is located at a particular IP address
     * Any address can be used so that this can be used to display dissaembly of code as
     * it runs (such as in a slow-motion mode that I would like to have in my emulator)
     * @param address The IP address to use for generating this dissasembly
    */
    fun disassembleStep(address:Int):String {
        val sb = StringBuilder()
        val cmd = commands[mem.read(address)]
        sb.append(cmd.OPString)
        sb.append(' ')

        // output of the instruction will be based on teh address mode
        when(cmd.addressMode) {
            AddressMode.ABSOLUTE -> {
                sb.append('$')
                sb.append(findAbsoluteAddress(address).toString(16).toUpperCase())
            }

            AddressMode.ABSOLUTE_X -> {
                sb.append('$')
                sb.append(findAbsoluteAddress(address).toString(16).toUpperCase())
                sb.append(", X")
            }

            AddressMode.ABSOLUTE_Y-> {
                sb.append('$')
                sb.append(findAbsoluteAddress(address).toString(16).toUpperCase())
                sb.append(", Y")
            }

            AddressMode.ACCUMULATOR-> { sb.append("A") }
            AddressMode.FUTURE_EXPANSION-> { sb.append("FUTURE EXPANSION")}

            AddressMode.IMMEDIATE -> {
                sb.append("#$")
                sb.append(mem.read(address+1).toString(16).toUpperCase())
            }

            AddressMode.IMPLIED ->{}

            AddressMode.INDIRECT -> {
                // Note jumps to the address at the memory location indicated
                sb.append('(')
                sb.append('$')
                sb.append(findAbsoluteAddress(address).toString(16).toUpperCase())
                sb.append(')')
            }

            AddressMode.INDIRECT_X-> {
                sb.append("($")
                sb.append(mem.read(address+1).toString(16).toUpperCase())
                sb.append(", X)")
            }

            AddressMode.INDIRECT_Y-> {
                sb.append('(')
                sb.append(mem.read(address+1).toString(16).toUpperCase())
                sb.append("), Y")
            }

            AddressMode.RELATIVE -> {
                // todo - verify that the relative address is correctly calculated
                val offset = mem.read(address+1).toByte()
                val addr = address + 2 + offset
                sb.append('$')
                sb.append(addr.toString(16).toUpperCase())
            }

            AddressMode.ZERO_PAGE -> {
                sb.append('$')
                sb.append(mem.read(address+1).toString(16).toUpperCase())
            }

            AddressMode.ZERO_PAGE_X -> {
                sb.append('$')
                sb.append(mem.read(address+1).toString(16).toUpperCase())
                sb.append(", X")
            }

            AddressMode.ZERO_PAGE_Y -> {
                sb.append('$')
                sb.append(mem.read(address+1).toString(16).toUpperCase())
                sb.append(", Y")
            }

            //else -> sb.append("Unknown addressing mode")
        }

        return sb.toString()
    }

    // runs the next instruction. The address of this is determined by the IP address
    fun runCommand() {
        val ip = state.ip
        commands[mem.read(ip)].execute(this)
    }

    fun runToBreak(address:Int = -1) {
        if (address >=0)
            state.ip = address
        var opCode = mem.read(state.ip)
        while (opCode != 0) {
            state.ipNext = state.ip + commands[opCode].size
            state.tick += commands[opCode].cycles
            commands[opCode].execute(this)
            state.ip = state.ipNext
            opCode = mem.read(state.ip)
        }
    }

    fun grabProcessorState(): ProcessorState {
        return ProcessorState(state.acc, state.x, state.y, state.ip, state.flags, state.sp, state.tick)
    }
}