package a2600Dragons.Test

import a2600Dragons.a2600.Cartridge
import a2600Dragons.m6502.*
import com.sun.org.apache.xpath.internal.operations.Bool

import java.io.File


data class TokenTestResults(val line:String, val ignoreWhite:Boolean, val tokens:ArrayList<AssemblerToken>)

/** Testing code for the 6502 subsystem (disassembler, assembler, interpreter)
 * -Test for the disassembler. To make sure the test data is present, we are our own memory manager for the virtual
 * machine so that it will be zero page with 0 through 255 so all possible commands can be tested.
 * -Test for the assembler tokenizer sees if tokenizer generates proper sequence of tokens
 * -Test for the assembler parser sees if generates proper byte code
 */
class M6502Tests : MemoryManager {

    // simply set the test values to the low byte of the address
    override fun read(address: Int): Int {
        return address and 255;
    }

    // not needed for testing
    override fun write(address: Int, n: Int) {
    }

    // Table of what the disassembly should be generating
    val expectedResults = arrayOf(
            "BRK ", "ORA ($2, X)", "X02 FUTURE EXPANSION", "X03 FUTURE EXPANSION", "X04 FUTURE EXPANSION", "ORA $6", "ASL $7", "X07 FUTURE EXPANSION",
            "PHP ", "ORA #\$A", "ASL A", "X0B FUTURE EXPANSION", "X0C FUTURE EXPANSION", "ORA \$F0E", "ASL $100F", "X0F FUTURE EXPANSION",
            "BPL $23", "ORA (12), Y", "X12 FUTURE EXPANSION", "X13 FUTURE EXPANSION", "X14 FUTURE EXPANSION", "ORA $16, X", "ASL $17, X", "X17 FUTURE EXPANSION",
            "CLC ", "ORA $1B1A, Y", "X1A FUTURE EXPANSION", "X1B FUTURE EXPANSION", "X1C FUTURE EXPANSION", "ORA $1F1E, X", "ASL $201F, X", "X1F FUTURE EXPANSION",
            "JSR $2221", "AND ($22, X)", "X22 FUTURE EXPANSION", "X23 FUTURE EXPANSION", "BIT $25", "AND $26", "ROL $27", "X27 FUTURE EXPANSION",
            "PLP ", "AND #$2A", "ROL A", "X2B FUTURE EXPANSION", "BIT $2E2D", "AND $2F2E", "ROL $302F", "X2F FUTURE EXPANSION",
            "BMI $63", "AND (32), Y", "X32 FUTURE EXPANSION", "X33 FUTURE EXPANSION", "X34 FUTURE EXPANSION", "AND $36, X", "ROL $37, X", "X37 FUTURE EXPANSION",
            "SEC ", "AND $3B3A, Y", "X3A FUTURE EXPANSION", "X3B FUTURE EXPANSION", "X3C FUTURE EXPANSION", "AND $3F3E, X", "ROL $403F, X", "X3F FUTURE EXPANSION",
            "RTI ", "EOR ($42, X)", "X42 FUTURE EXPANSION", "X43 FUTURE EXPANSION", "X44 FUTURE EXPANSION", "EOR $46", "LSR $47", "X47 FUTURE EXPANSION",
            "PHA ", "EOR #$4A", "LSR A", "X4B FUTURE EXPANSION", "JMP $4E4D", "EOR $4F4E", "LSR $504F", "X42 FUTURE EXPANSION",
            "BVC \$A3", "EOR (52), Y", "X52 FUTURE EXPANSION", "X53 FUTURE EXPANSION", "X54 FUTURE EXPANSION", "EOR $56, X", "LSR $57, X", "X57 FUTURE EXPANSION",
            "CLI ", "EOR $5B5A, Y", "X5A FUTURE EXPANSION", "X5B FUTURE EXPANSION", "X5C FUTURE EXPANSION", "EOR $5F5E, X", "LSR $605F, X", "X5F FUTURE EXPANSION",
            "RTS ", "ADC ($62, X)", "X62 FUTURE EXPANSION", "X63 FUTURE EXPANSION", "X64 FUTURE EXPANSION", "ADC $66", "ROR $67", "X67 FUTURE EXPANSION",
            "PLA ", "ADC #$6A", "ROR A", "X6B FUTURE EXPANSION", "JMP ($6E6D)", "ADC $6F6E", "ROR $706F", "X6F FUTURE EXPANSION",
            "BVS \$E3", "ADC (72), Y", "X72 FUTURE EXPANSION", "X73 FUTURE EXPANSION", "X74 FUTURE EXPANSION", "ADC $76, X", "ROR $77, X", "X77 FUTURE EXPANSION",
            "SEI ", "ADC $7B7A, Y", "X7A FUTURE EXPANSION", "X7B FUTURE EXPANSION", "X7C FUTURE EXPANSION", "ADC $7F7E, X", "ROR $807F, X", "X7F FUTURE EXPANSION",
            "X80 FUTURE EXPANSION", "STA ($82, X)", "X82 FUTURE EXPANSION", "X83 FUTURE EXPANSION", "STY $85", "STA $86", "STX $87", "X87 FUTURE EXPANSION",
            "DEY ", "X89 FUTURE EXPANSION", "TXA ", "X8B FUTURE EXPANSION", "STY $8E8D", "STA $8F8E", "STX $908F", "X8F FUTURE EXPANSION",
            "BCC $23", "STA (92), Y", "X92 FUTURE EXPANSION", "X93 FUTURE EXPANSION", "STY $95, X", "STA $96, X", "STX $97, Y", "X97 FUTURE EXPANSION",
            "TYA ", "STA $9B9A, Y", "TXS ", "X9B FUTURE EXPANSION", "X9C FUTURE EXPANSION", "STA $9F9E, X", "X9E FUTURE EXPANSION", "X9F FUTURE EXPANSION",
            "LDY #\$A1", "LDA (\$A2, X)", "LDX #\$A3", "XA3 FUTURE EXPANSION", "LDY \$A5", "LDA \$A6", "LDX \$A7", "XA7 FUTURE EXPANSION",
            "TAY ", "LDA #\$AA", "TAX ", "XAB FUTURE EXPANSION", "LDY \$AEAD", "LDA \$AFAE", "LDX \$B0AF", "XAF FUTURE EXPANSION",
            "BCS $63", "LDA (B2), Y", "XB2 FUTURE EXPANSION", "XB3 FUTURE EXPANSION", "LDY \$B5, X", "LDA \$B6, X", "LDX \$B7, Y", "XB7 FUTURE EXPANSION",
            "CLV ", "LDA \$BBBA, Y", "TSX ", "XBB FUTURE EXPANSION", "LDY \$BEBD, X", "LDA \$BFBE, X", "LDX \$C0BF, Y", "XBF FUTURE EXPANSION",
            "CPY #\$C1", "CMP (\$C2, X)", "XC2 FUTURE EXPANSION", "XC3 FUTURE EXPANSION", "CPY \$C5", "CMP \$C6", "DEC \$C7", "XC7 FUTURE EXPANSION",
            "INY ", "CMP #\$CA", "DEX ", "XCB FUTURE EXPANSION", "CPY \$CECD", "CMP \$CFCE", "DEC \$D0CF", "XCF FUTURE EXPANSION",
            "BNE \$A3", "CMP (D2), Y", "XD2 FUTURE EXPANSION", "XD3 FUTURE EXPANSION", "XD4 FUTURE EXPANSION", "CMP \$D6, X", "DEC \$D7, X", "XD7 FUTURE EXPANSION",
            "CLD ", "CMP \$DBDA, Y", "XDA FUTURE EXPANSION", "XDB FUTURE EXPANSION", "XDC FUTURE EXPANSION", "CMP \$DFDE, X", "DEC \$E0DF, X", "XDF FUTURE EXPANSION",
            "CPX #\$E1", "SBC (\$E2, X)", "XE2 FUTURE EXPANSION", "XE3 FUTURE EXPANSION", "CPX \$E5", "SBC \$E6", "INC \$E7", "XE7 FUTURE EXPANSION",
            "INX ", "SBC #\$EA", "NOP ", "XEB FUTURE EXPANSION", "CPX \$EEED", "SBC \$EFEE", "INC \$F0EF", "XEF FUTURE EXPANSION",
            "BEQ \$E3", "SBC (F2), Y", "XF2 FUTURE EXPANSION", "XF3 FUTURE EXPANSION", "XF4 FUTURE EXPANSION", "SBC \$F6, X", "INC \$F7, X", "XF2 FUTURE EXPANSION",
            "SED ", "SBC \$FBFA, Y", "XFA FUTURE EXPANSION", "XFB FUTURE EXPANSION", "XFC FUTURE EXPANSION", "SBC \$FFFE, X", "INC \$FF, X", "XF2 FUTURE EXPANSION"
    )

    // perform a test on the indicated instruction (always at that address) and see that it matches what we expect
    fun testM6502(m6502:M6502, address:Int) : Boolean {
        var disassembly:String = m6502.disassembleStep(address)

        return disassembly == expectedResults[address]
    }


    // Main batch of token data for testing tokenization portion of the assembler
    val tokenTestData = arrayOf(
            TokenTestResults("   ", false,
                    arrayListOf(AssemblerToken(AssemblerTokenTypes.WHITESPACE, " ", 3))),
            TokenTestResults(".BANK \$B", false,
                    arrayListOf(AssemblerToken(AssemblerTokenTypes.DIRECTIVE, "BANK", 0),
                        AssemblerToken(AssemblerTokenTypes.WHITESPACE, " ", 1),
                        AssemblerToken(AssemblerTokenTypes.NUMBER, "\$B", 11))),
            TokenTestResults("Main:", false,
                    arrayListOf(AssemblerToken(AssemblerTokenTypes.LABEL_DECLARATION, "Main", 0))),
            TokenTestResults("\t CLD", false,
                     arrayListOf( AssemblerToken(AssemblerTokenTypes.WHITESPACE, " ", 2),
                            AssemblerToken(AssemblerTokenTypes.OPCODE, "CLD", 0))),
            TokenTestResults("\t LDA #0 ; prepare to clear memory", true,
                    arrayListOf( AssemblerToken(AssemblerTokenTypes.OPCODE, "LDA", 0),
                            AssemblerToken(AssemblerTokenTypes.IMMEDIATE, "#", 0),
                            AssemblerToken(AssemblerTokenTypes.NUMBER, "0", 0))),
            TokenTestResults("\t TAX", true,
                    arrayListOf( AssemblerToken(AssemblerTokenTypes.OPCODE, "TAX", 0))),
            TokenTestResults("clearLoop: STA 0,X ", true,
                    arrayListOf( AssemblerToken(AssemblerTokenTypes.LABEL_DECLARATION, "clearLoop", 0),
                            AssemblerToken(AssemblerTokenTypes.OPCODE, "STA", 0),
                            AssemblerToken(AssemblerTokenTypes.NUMBER, "0", 0),
                            AssemblerToken(AssemblerTokenTypes.INDEX_X, ",X", 0))),
            TokenTestResults("\t INX", true,
                    arrayListOf( AssemblerToken(AssemblerTokenTypes.OPCODE, "INX", 0))),
            TokenTestResults("\t BNE clearLoop", true,
                    arrayListOf( AssemblerToken(AssemblerTokenTypes.OPCODE, "BNE", 0),
                            AssemblerToken(AssemblerTokenTypes.LABEL_LINK, "clearLoop", 0)))
    )

    /** Simple test to make sure the tokens are proper */
    fun testTokenizer():Boolean {
        var success = true
        var assembler = Assembler(M6502(this))
        for (test in tokenTestData) {
            var tokens = assembler.tokenize(test.line, test.ignoreWhite)
            if (tokens != test.tokens) {
                println(test.line)
                println("FAILED $tokens")
                println("EXPECT ${test.tokens}")
                success = false
                break
            }
        }

        return success
    }

    fun compareAssembly(testML:Array<Int>, expectedML:Array<Int>, stopAfterError:Boolean = true):Boolean
    {
        var success = true;
        if (testML.size < expectedML.size)
            success = false
        else if (expectedML.size == 0)
            success = true
        else {
            for (cntr in 0..(expectedML.size-1))
                if (testML[cntr] != expectedML[cntr]) {
                    success = false
                    print ("FAILED at address $${cntr.toString(16)}: $${testML[cntr].toString(16)}")
                    println ( " expcted $${expectedML[cntr].toString(16)}")
                    if (stopAfterError) break
                }
        }

        return success
    }


    fun testNoLabelAssembly(verbose:Boolean = false): Boolean {
        var assembler = Assembler(M6502(this), verbose)
        assembler.assembleProgram(arrayListOf ( "ORA ($2, X)", "ORA $6", "ASL $7", "PHP ", "ORA #\$A", "ASL A",
                "ORA \$F0E", "ASL $100F", "BPL $23", "ORA ($12), Y", "ORA $16, X", "ASL $17, X", "CLC ", "ORA $1B1A, Y",
                "ORA $1F1E, X", "ASL $201F, X", "JSR $2221", "AND ($22, X)", "BIT $25", "AND $26", "ROL $27",
                "PLP ", "AND #$2A", "ROL A", "BIT $2E2D", "AND $2F2E", "ROL $302F", "BMI \$6D", "AND ($32), Y",
                "AND $36, X", "ROL $37, X", "SEC ", "AND $3B3A, Y", "AND $3F3E, X", "ROL $403F, X", "RTI ",
                "EOR ($42, X)", "EOR $46", "LSR $47", "PHA ", "EOR #$4A", "LSR A", "JMP $4E4D", "EOR $4F4E",
                "LSR $504F", "BVC \$B3", "EOR ($52), Y", "EOR $56, X", "LSR $57, X", "CLI ", "EOR $5B5A, Y",
                "EOR $5F5E, X", "LSR $605F, X", "RTS ", "ADC ($62, X)", "ADC $66", "ROR $67", "PLA ", "ADC #$6A",
                "ROR A", "JMP ($6E6D)", "ADC $6F6E", "ROR $706F", "BVS \$F9", "ADC ($72), Y", "ADC $76, X", "ROR $77, X",
                "SEI ", "ADC $7B7A, Y", "ADC $7F7E, X", "ROR $807F, X", "STA ($82, X)", "STY $85", "STA $86", "STX $87",
                "DEY ", "TXA ", "STY $8E8D", "STA $8F8E", "STX $908F", "BCC $3E", "STA ($92), Y", "STY $95, X",
                "STA $96, X", "STX $97, Y", "TYA ", "STA $9B9A, Y", "TXS ", "STA $9F9E, X", "LDY #\$A1",
                "LDA (\$A2, X)", "LDX #\$A3", "LDY \$A5", "LDA \$A6", "LDX \$A7", "TAY ", "LDA #\$AA", "TAX ",
                "LDY \$AEAD", "LDA \$AFAE", "LDX \$B0AF", "BCS $89", "LDA (\$B2), Y", "LDY \$B5, X", "LDA \$B6, X",
                "LDX \$B7, Y", "CLV ", "LDA \$BBBA, Y", "TSX ", "LDY \$BEBD, X", "LDA \$BFBE, X", "LDX \$C0BF, Y",
                "CPY #\$C1", "CMP (\$C2, X)", "CPY \$C5", "CMP \$C6", "DEC \$C7", "INY ", "CMP #\$CA", "DEX ",
                "CPY \$CECD", "CMP \$CFCE", "DEC \$D0CF", "BNE \$D8", "CMP (\$D2), Y", "CMP \$D6, X", "DEC \$D7, X",
                "CLD ", "CMP \$DBDA, Y", "CMP \$DFDE, X", "DEC \$E0DF, X", "CPX #\$E1", "SBC (\$E2, X)", "CPX \$E5",
                "SBC \$E6", "INC \$E7", "INX ", "SBC #\$EA", "NOP ", "CPX \$EEED", "SBC \$EFEE", "INC \$F0EF",
                "BEQ \$121", "SBC (\$F2), Y", "SBC \$F6, X", "INC \$F7, X", "SED ", "SBC \$FBFA, Y", "SBC \$FFFE, X",
                "INC \$1FF, X"))


        val expectedOutput = arrayOf(
                  1,  2,  5,  6,  6,  7,  8,  9, 10, 10, 13, 14, 15, 14, 15, 16,
                 16, 17, 17, 18, 21, 22, 22, 23, 24, 25, 26, 27, 29, 30, 31, 30,
                 31, 32, 32, 33, 34, 33, 34, 36, 37, 37, 38, 38, 39, 40, 41, 42,
                 42, 44, 45, 46, 45, 46, 47, 46, 47, 48, 48, 49, 49, 50, 53, 54,
                 54, 55, 56, 57, 58, 59, 61, 62, 63, 62, 63, 64, 64, 65, 66, 69,
                 70, 70, 71, 72, 73, 74, 74, 76, 77, 78, 77, 78, 79, 78, 79, 80,
                 80, 81, 81, 82, 85, 86, 86, 87, 88, 89, 90, 91, 93, 94, 95, 94,
                 95, 96, 96, 97, 98,101,102,102,103,104,105,106,106,108,109,110,
                109,110,111,110,111,112,112,113,113,114,117,118,118,119,120,121,
                122,123,125,126,127,126,127,128,129,130,132,133,133,134,134,135,
                136,138,140,141,142,141,142,143,142,143,144,144,145,145,146,148,
                149,149,150,150,151,152,153,154,155,154,157,158,159,160,161,161,
                162,162,163,164,165,165,166,166,167,168,169,170,170,172,173,174,
                173,174,175,174,175,176,176,177,177,178,180,181,181,182,182,183,
                184,185,186,187,186,188,189,190,189,190,191,190,191,192,192,193,
                193,194,196,197,197,198,198,199,200,201,202,202,204,205,206,205,
                206,207,206,207,208,208,209,209,210,213,214,214,215,216,217,218,
                219,221,222,223,222,223,224,224,225,225,226,228,229,229,230,230,
                231,232,233,234,234,236,237,238,237,238,239,238,239,240,240,241,
                241,242,245,246,246,247,248,249,250,251,253,254,255,254,255,  1)

        return compareAssembly(assembler.banks[0], expectedOutput, false)
//        for (indx in 0 ..(expectedOutput.size-1))
//            if (assembler.banks[0][indx] != expectedOutput[indx])
//                println("Data mismatch at $${indx.toString(16)}. Expecing $${expectedOutput[indx].toString(16)} but got $${assembler.banks[0][indx].toString(16)}!")
    }

    fun testLabelAssembly(verbose:Boolean = false): Boolean {
        var assembler = Assembler(M6502(this), true)//verbose)
        assembler.assembleProgram(arrayListOf ( "start: ldx #0", "loop: jsr test", "dex", " bne loop", "jmp end", "test:", "rts", "end: brk"))
        val expectedOutput = arrayOf(
                162,  0,  32,  11, 0, 202,  208,  250,  76, 12, 0, 96, 0)

        return compareAssembly(assembler.banks[0], expectedOutput, false)
    }

    fun testAssemblySnippet(testName:String, assembly:ArrayList<String>, anticipatedResults:ArrayList<Pair<String,Int>>, verbose:Boolean = false):Boolean {
        val cart = Cartridge()
        val m6502 = M6502(cart)

        // assemble the file
        var assembler = Assembler(m6502, verbose)
        val errorLevel = assembler.assembleProgram(assembly)
        if (errorLevel > 0) {
            println("$testName failed to compile properly!")
            return false
        }

        // run until break
        for (cntr in 0..4095)
            cart.write(cntr, assembler.banks[0][cntr])
        m6502.runToBreak()

        // compare results
        var processorState = m6502.GrabProcessorState()
        var testsPassed = true;
        for (test in anticipatedResults) {
            var passed = processorState.checkState(test.first, test.second, cart)
            var passString = if (passed) "Passed" else "Failed"
            println("${test.first} $passString")
            testsPassed = testsPassed and passed
        }
        return testsPassed;
    }
}



/**
 * Tester code.
 */
fun main(args: Array<String>) {
    val verbose = false
    val m6502Tests = M6502Tests()
    val m6502 = M6502(m6502Tests)
    var success = true
    for (cntr in 0..255)
        if (m6502Tests.testM6502(m6502, cntr) != true) {
            println("Operation \$${cntr.toString(16).toUpperCase()} ${m6502.disassembleStep(cntr)} did not match ${m6502Tests.expectedResults[cntr]}")
            success = false
        }

    if (success)
        println("Test completed successfully!")
    else
        println("There were problems with the test...please check the mismatched instructions.")

    println (if (m6502Tests.testTokenizer()) "Tokenizer test passed" else "*** PROBLEM With TOKENIZING ***")

    println(if (m6502Tests.testNoLabelAssembly(verbose)) "Can assemble properly (without labels)" else "*** PROBLEMS WITH ASSEMBLING ***")
    println(if (m6502Tests.testLabelAssembly(verbose)) "Can assemble properly (with labels)" else "*** PROBLEMS WITH ASSEMBLING LABELS ***")

//    loadTest("a.out")
}