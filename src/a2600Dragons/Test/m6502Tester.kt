package a2600Dragons.Test

import a2600Dragons.a2600.Cartridge
import a2600Dragons.m6502.*

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
        return address and 255
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
        val disassembly:String = m6502.disassembleStep(address)

        return disassembly == expectedResults[address]
    }


    // Main batch of token data for testing tokenization portion of the assembler
    private val tokenTestData = arrayOf(
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
        val assembler = Assembler(M6502(this))
        for (test in tokenTestData) {
            val tokens = assembler.tokenize(test.line, test.ignoreWhite)
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

    private fun compareAssembly(testML:Array<Int>, expectedML:Array<Int>, stopAfterError:Boolean = true):Boolean
    {
        var success = true
        if (testML.size < expectedML.size)
            success = false
        else if (expectedML.isEmpty())
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
        val assembler = Assembler(M6502(this), verbose)
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

        return compareAssembly(assembler.currentBank.bankToIntArray(), expectedOutput, false)
//        for (indx in 0 ..(expectedOutput.size-1))
//            if (assembler.banks[0][indx] != expectedOutput[indx])
//                println("Data mismatch at $${indx.toString(16)}. Expecing $${expectedOutput[indx].toString(16)} but got $${assembler.banks[0][indx].toString(16)}!")
    }

    fun testLabelAssembly(verbose:Boolean = false): Boolean {
        val assembler = Assembler(M6502(this), verbose)
        assembler.assembleProgram(arrayListOf ( "start: ldx #0", "loop: jsr test", "dex", " bne loop", "jmp end", "test:", "rts", "end: brk"))
        val expectedOutput = arrayOf(
                162,  0,  32,  11, 0, 202,  208,  250,  76, 12, 0, 96, 0)

        return compareAssembly(assembler.currentBank.bankToIntArray(), expectedOutput, false)
    }

    fun testDirectives(verbose:Boolean = false):Boolean {
        val assembler = Assembler(M6502(this), verbose)

        // .BANK tests
        var errorCode = assembler.assembleProgram(arrayListOf (
                ".BANK 0 $1000 1024", "jsr bank1", "jsr bank2", ".bank 1 $2000 1024",
                "bank1:	LDA #1", "RTS",
                ".BANK 2 $2000 1024", "bank2:", "LDA #2", "RTS"))
        if (errorCode > 0) {println("ERROR unable to assemble bank test!"); return false}
        if (assembler.banks[0].bankOrigin != 4096){println("ERROR bank 0 origin incorrect!"); return false}
        if (assembler.banks[0].size != 1024){println("ERROR bank 0 size incorrect!"); return false}
        if (assembler.banks[1].bankOrigin != 8192){
            println("ERROR bank 1 origin incorrect! ${assembler.banks[1].bankOrigin}"); return false}
        if (assembler.banks[1].size != 1024){println("ERROR bank 1 size incorrect!"); return false}
        if (assembler.banks[2].bankOrigin != 8192){println("ERROR bank 2 origin incorrect!"); return false}
        if (assembler.banks[2].size != 1024){println("ERROR bank 2 size incorrect!"); return false}
        val bank_b0expected = arrayOf(32, 0, 32, 32, 0, 32)
        if ( ! compareAssembly(assembler.banks[0].bankToIntArray(),bank_b0expected))
            {println("ERROR bank 0 machine language incorrect!"); return false}
        val bank_b1expected = arrayOf(169, 1)
        if ( ! compareAssembly(assembler.banks[1].bankToIntArray(),bank_b1expected))
        {println("ERROR bank 1 machine language incorrect!"); return false}
        val bank_b2expected = arrayOf(169, 2)
        if ( ! compareAssembly(assembler.banks[2].bankToIntArray(),bank_b2expected))
            {println("ERROR bank 2 machine language incorrect!"); return false}
        if (verbose) println("Bank directive tests passed!")

        // .ORG tests
        errorCode = assembler.assembleProgram(arrayListOf (
                ".BANK 0 $1000", "jmp ten",
                ".ORG 4106", "ten: LDA #10", "brk"))
        if (errorCode > 0) {println("ERROR unable to assemble ORG test!"); return false}
        if (assembler.banks[0].bankOrigin != 4096){println("ERROR org bank 0 origin incorrect!"); return false}
        if (assembler.banks[0].size != 4096){println("ERROR org bank 0 size incorrect!"); return false}
        val org_b0expected = arrayOf(76, 10, 16, 0,0,0,0,0,0,0, 169, 10, 0)
        if ( ! compareAssembly(assembler.banks[0].bankToIntArray(),org_b0expected))
            {println("ERROR bank 2 machine language incorrect!"); return false}
        if (verbose) println("ORG directive tests passed!")

        // .EQU tests
        errorCode = assembler.assembleProgram(arrayListOf (
            ".EQU ONE 1", ".EQU TWO $2", ".EQU THREE %11",
            "LDA #ONE", "LDA #TWO", "STA THREE", "BRK" ))
        if (errorCode > 0) {println("ERROR unable to assemble EQU test!"); return false}
        val equ_expected = arrayOf(169,1, 169,2, 133,3,0)
        if ( ! compareAssembly(assembler.banks[0].bankToIntArray(),equ_expected))
            {println("ERROR .EQU machine language incorrect!"); return false}
        if (verbose) println("EQU directive tests passed!")

        // .HIGH .LOW tests
        errorCode = assembler.assembleProgram(arrayListOf (
                ".BANK 0 $1000 1024",
                ".EQU hilow $1234",
                "LDA #.HIGH hilow",
                "labelhl: LDA #.LOW hilow",
                "LDA #.HIGH labelhl",
                "LDA #.LOW labelhl",
                "BRK" ))
        if (errorCode > 0) {println("ERROR unable to assemble HIGH/LOW test!"); return false}
        val highlow_expected = arrayOf(169,0x12, 169,0x34, 169,16, 169,2, 0)
        if ( ! compareAssembly(assembler.banks[0].bankToIntArray(),highlow_expected))
        {println("ERROR .HIGH .LOW machine language incorrect!"); return false}
        if (verbose) println("HIGH LOW directive tests passed!")

        // Storage (.BYTE .WORD) tests
        errorCode = assembler.assembleProgram(arrayListOf (
                ".BANK 0 $1000 1024",
                ".EQU two 2",
                ".EQU fivesix $0605",
                "LDA bytes",
                "LDA words",
                "bytes: .BYTE 1 two 3 4",
                "words: .WORD fivesix $0807 $0A09 bytes",
                "BRK" ))
        if (errorCode > 0) {println("ERROR unable to assemble Storage test!"); return false}
        val storage_expected = arrayOf(173,0x06,0x10, 173,0x0A,0x10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 6, 16, 0)
        if ( ! compareAssembly(assembler.banks[0].bankToIntArray(),storage_expected))
        {println("ERROR Storage (.BYTE .WORD)machine language incorrect!"); return false}
        if (verbose) println("Storage directive tests passed!")

        // Macro testing (.MSTART .MEND .MACRO) tests
        errorCode = assembler.assembleProgram(arrayListOf (
                ".MSTART DELAY 255", "LDX #P0","delayLoop:", "DEX", "BNE delayLoop", ".MEND",
                ".MSTART LONGDELAY 2 255", "LDY #P0", "longDelayLoop:", ".MACRO DELAY P1", "DEY", "BNE longDelayLoop",
                ".MEND", ".MACRO DELAY 1", ".MACRO DELAY 3", ".MACRO LONGDELAY 10 100" ))
        if (errorCode > 0) {println("ERROR unable to assemble Macro test!"); return false}
        val macro_expected = arrayOf(0xa2,0x01, 0xca, 0xd0,0xFD,
                0xa2,0x03, 0xca, 0xd0,0xFD,
                0xa0,10,  0xa2,100, 0xca, 0xd0,0xFD, 0x88, 0xd0,0xF8,
                0)//0x10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 6, 16, 0)
        if ( ! compareAssembly(assembler.banks[0].bankToIntArray(), macro_expected))
        {println("ERROR Macro (.MSTART .MEND .MACRO) machine language incorrect!"); return false}
        if (verbose) println("Macro directive tests passed!")

        return true
    }


    fun testAssemblySnippet(testName:String, assembly:ArrayList<String>, anticipatedResults:ArrayList<Pair<String,Int>>, verbose:Boolean = false):Boolean {
        val cart = Cartridge()
        val m6502 = M6502(cart)

        // assemble the file
        val assembler = Assembler(m6502, verbose)
        val errorLevel = assembler.assembleProgram(assembly)
        if (errorLevel > 0) {
            println("$testName failed to compile properly!")
            return false
        }

        // run until break
        for (cntr in 0..4095)
            cart.write(cntr, assembler.currentBank.readBankAddress(cntr))
        m6502.runToBreak(0)

        // compare results
        val processorState = m6502.grabProcessorState()
        var testsPassed = true
        for (test in anticipatedResults) {
            val passed = processorState.checkState(test.first, test.second, cart)
            val passString = if (passed) "Passed" else "Failed"
            if (verbose) println("$testName: ${test.first}=${test.second} $passString")
            testsPassed = testsPassed and passed
        }
        return testsPassed
    }

    /**
     * Processor Instructions tests in implementation order
     */
    fun testRunningInstrutions(verbose:Boolean = false):Boolean {
        var testResults = true
        // * Flag setting tests *
        // SEC
        testResults = testResults and testAssemblySnippet("SEC Test",
                arrayListOf( "CLC", "SEC", "BRK"),
                arrayListOf(Pair("C", 1)), verbose)

        // CLC
        testResults = testResults and testAssemblySnippet("CLC Test",
                arrayListOf( "SEC", "CLC", "BRK"),
                arrayListOf(Pair("C", 0)), verbose)

        // SED
        testResults = testResults and testAssemblySnippet("SED Test",
                arrayListOf( "CLD", "SED", "BRK"),
                arrayListOf(Pair("D", 1)), verbose)

        // CLD
        testResults = testResults and testAssemblySnippet("CLD Test",
                arrayListOf( "SED", "CLD", "BRK"),
                arrayListOf(Pair("D", 0)), verbose)

        // SEI
        testResults = testResults and testAssemblySnippet("SEI Test",
                arrayListOf( "CLI", "SEI", "BRK"),
                arrayListOf(Pair("I", 1)), verbose)

        // CLI
        testResults = testResults and testAssemblySnippet("CLI Test",
                arrayListOf( "SEI", "CLI", "BRK"),
                arrayListOf(Pair("I", 0)), verbose)

        // CLV - need better test, redo when ADC has been implemented!
        testResults = testResults and testAssemblySnippet("CLV Test",
                arrayListOf( "CLV", "BRK"),
                arrayListOf(Pair("V", 0)), verbose)

        if ( ! testResults) { println("Failed during Flag setting tests!"); return false }

        // * Register Loading Tests*
        // let us be able to force Loading section to be verbose for debugging by setting verLoad to true
        val verLoad = verbose

        // LDA Immediate - zero test ; expect a=0; z=1; n=0
        testResults = testResults and testAssemblySnippet("LDA Immediate",
                arrayListOf( "LDA #0", "BRK"),
                arrayListOf(Pair("a", 0), Pair("z", 1), Pair("n", 0)), verLoad)

        // LDX Immediate - negative test ; expect x=255; z=0; n=1
        testResults = testResults and testAssemblySnippet("LDX Immediate",
                arrayListOf( "LDX #\$FF", "BRK"),
                arrayListOf(Pair("x", 255), Pair("z", 0), Pair("n", 1)), verLoad)

        // LDY Immediate - positive test
        testResults = testResults and testAssemblySnippet("LDY Immediate",
                arrayListOf( "LDY #127", "BRK"),
                arrayListOf(Pair("Y", 127), Pair("z", 0), Pair("n", 0)), verLoad)

        // LDA Zero page - positive test
        testResults = testResults and testAssemblySnippet("LDA Zero page",
                arrayListOf( "LDA 5", "BRK", ".ORG 5", ".BYTE 42"),
                arrayListOf(Pair("A", 42), Pair("z", 0), Pair("n", 0)), verLoad)

        // LDX Zero page - zero test
        testResults = testResults and testAssemblySnippet("LDX Zero Page",
                arrayListOf( "LDX 200", "BRK", ".ORG 200", ".BYTE 0"),
                arrayListOf(Pair("X", 0), Pair("z", 1), Pair("n", 0)), verLoad)

        // LDY Zero page - negative test
        testResults = testResults and testAssemblySnippet("LDY Zero Page",
                arrayListOf( "LDY 100", "BRK", ".ORG 100", ".BYTE 128"),
                arrayListOf(Pair("Y", 128), Pair("z", 0), Pair("n", 1)), verLoad)

        // LDA Zero page,x
        testResults = testResults and testAssemblySnippet("LDA Zero page,x",
                arrayListOf( "LDX #10", "LDA 5,X", "BRK", ".ORG 15", ".BYTE 99"),
                arrayListOf(Pair("A", 99)), verLoad)

        // LDY Zero page,x
        testResults = testResults and testAssemblySnippet("LDY Zero page,x",
                arrayListOf( "LDX #5", "LDY 5,X", "BRK", ".ORG 10", ".BYTE 12"),
                arrayListOf(Pair("Y", 12)), verLoad)

        // LDX Zero page,Y
        testResults = testResults and testAssemblySnippet("LDX Zero page,Y",
                arrayListOf( "LDY #15", "LDX 10,Y", "BRK", ".ORG 25", ".BYTE 52"),
                arrayListOf(Pair("X", 52)), verLoad)

        // LDA, LDX, LDY Absolute
        testResults = testResults and testAssemblySnippet("LDA, LDX, LDY Absolute",
                arrayListOf( "LDA 1024", "LDX 1025", "LDY 1026", "BRK", ".ORG 1024", ".BYTE 1 2 3"),
                arrayListOf(Pair("A", 1), Pair("X", 2), Pair("Y", 3)), verLoad)

        // LDA and LDY Absolute,X
        testResults = testResults and testAssemblySnippet("LDA and LDY Absolute,X",
                arrayListOf( "LDX #10", "LDA 512,X", "LDY 513,X", "BRK", ".ORG 522", ".BYTE 1 2 3"),
                arrayListOf(Pair("A", 1), Pair("Y", 2)), verLoad)

        // LDA and LDX Absolute,Y
        testResults = testResults and testAssemblySnippet("LDA and LDX Absolute,Y",
                arrayListOf( "LDY #10", "LDA 512,Y", "LDX 513,Y", "BRK", ".ORG 522", ".BYTE 1 2 3"),
                arrayListOf(Pair("A", 1), Pair("X", 2)), verLoad)

        // LDA (Indirect,X)
        testResults = testResults and testAssemblySnippet("LDA (Indirect,X)",
                arrayListOf( "LDX #200", "LDA (54,X)", "BRK", ".ORG 254", ".WORD 1024", ".ORG 1024", ".BYTE 88"),
                arrayListOf(Pair("A", 88)), verLoad)

        // LDA (Indirect),Y
        testResults = testResults and testAssemblySnippet("LDA (Indirect),Y",
                arrayListOf( "LDY #10", "LDA (254),Y", "BRK", ".ORG 254", ".WORD 1024", ".ORG 1034", ".BYTE 88"),
                arrayListOf(Pair("A", 88)), verLoad)

        if ( ! testResults) { println("Failed Register Loading tests!"); return false }

        // * Register Storing Tests*

        // let us be able to force Storing section to be verbose for debugging by setting verLoad to true
        val verStore = verbose

        // STA Tests (all three)
        testResults = testResults and testAssemblySnippet("STA tests (all seven modes)",
                arrayListOf(	"LDA #123",	"LDX #\$10", "LDY #5", "STA \$AB", "STA \$AB,X", "STA \$250",
                        "STA \$250,X", "STA \$250,Y", "STA (\$50,X)", "STA (\$60),Y",".ORG $60",".WORD $600"),
                arrayListOf(Pair("MAB", 123), Pair("MBB", 123), Pair("M250", 123),Pair("M260", 123),
                        Pair("M255", 123), Pair("M600", 123),Pair("M605", 123) ), verStore)

        // STX Tests (all three)
        testResults = testResults and testAssemblySnippet("STX Tests (all three)",
                arrayListOf(	"LDX #22", "LDY #5", "STX \$50", "STX \$50,Y", "STX \$250", "BRK" ),
                arrayListOf(Pair("M50", 22), Pair("M55", 22),Pair("M250", 22) ), verStore)

        // STY Tests (all three)
        testResults = testResults and testAssemblySnippet("STY Tests (all three)",
                arrayListOf(	"LDX #5", "LDY #33", "STY \$50", "STY \$50,X", "STY \$250", "BRK" ),
                arrayListOf(Pair("M50", 33), Pair("M55", 33),Pair("M250", 33) ), verStore)

        if ( ! testResults) { println("Failed Register Storing tests!"); return false }

        // * Transferring between registers tests *

        // let us be able to force transfer section to be verbose for debugging by setting verLoad to true
        val verTrans = verbose

        // TXA TAY test
        testResults = testResults and testAssemblySnippet("TXA TAY test",
                arrayListOf(	"LDX #179", "TXA", "TAY", "BRK" ),
                arrayListOf(Pair("A", 179), Pair("Y", 179), Pair("N", 1), Pair("Z", 0) ), verTrans)

        // TYA TAX test
        testResults = testResults and testAssemblySnippet("TYA TAX test",
                arrayListOf(	"LDY #24", "TYA", "TAX", "BRK" ),
                arrayListOf(Pair("A", 24), Pair("X", 24), Pair("N", 0), Pair("Z", 0) ), verTrans)

        // Transfer 0 test
        testResults = testResults and testAssemblySnippet("Transfer 0 test",
                arrayListOf(	"LDA #0", "LDX #23", "TAX", "BRK" ),
                arrayListOf(Pair("A", 0), Pair("X", 0), Pair("N", 0), Pair("Z", 1) ), verTrans)

        if ( ! testResults) { println("Failed Register transfer tests!"); return false }

        // * Stack tests *

        val verStack = verbose

        // TSX and TXS
        testResults = testResults and testAssemblySnippet("TSX and TXS",
                arrayListOf(	"LDX #128", "TXS", "LDX #0", "TSX", "BRK" ),
                arrayListOf(Pair("SP", 128), Pair("X", 128) ), verStack)

        // Stack Pushing test
        testResults = testResults and testAssemblySnippet("Stack Pushing test",
                arrayListOf(	"LDX #255", "TXS", "LDA #11", "SED", "LDY #0", "PHP", "PHA", "BRK" ),
                arrayListOf(Pair("SP", 253), Pair("M1FE", 11), Pair("M1FF", 42) ), verStack)

        // Stack Pulling test
        testResults = testResults and testAssemblySnippet("Stack Pulling test",
                arrayListOf(	"LDX #253", "TXS", "PLA", "PLP", "BRK", ".ORG \$1FE", ".BYTE 11 42" ),
                arrayListOf(Pair("SP", 255), Pair("A", 11), Pair("Flags", 42) ), verStack)

        if ( ! testResults) { println("Failed Stack tests!"); return false }

        // * Incrementing and decrementing tests *

        val verInc =  verbose

        // Incrementing
        testResults = testResults and testAssemblySnippet("Incrementing",
                arrayListOf("LDX #$10", "LDY #255", "INC $50", "INC $41,X", "INC $110", "INC $101,X",
                        "INX", "INY", "BRK", ".ORG $50", ".BYTE 1 2", ".ORG $110", ".BYTE 3 4" ),
                arrayListOf(Pair("M50", 2), Pair("M51", 3), Pair("M110", 4), Pair("M111", 5), Pair("X", 17),
                        Pair("Y", 0), Pair("Z", 1) ), verInc)

        // Decrementing
        testResults = testResults and testAssemblySnippet("Decrementing",
                arrayListOf("LDX #$10", "LDY #0", "DEC $50", "DEC $41,X", "DEC $110", "DEC $101,X",
                        "DEX", "DEY", "BRK", ".ORG $50", ".BYTE 1 2", ".ORG $110", ".BYTE 3 4" ),
                arrayListOf(Pair("M50", 0), Pair("M51", 1), Pair("M110", 2), Pair("M111", 3), Pair("X", 15),
                        Pair("Y", 255), Pair("N", 1) ), verInc)

        if ( ! testResults) { println("Failed INC/DEC tests!"); return false }

        // * basic branching (BEQ,BNE,BPL,BMI) tests *

        val verBBranch = verbose

        // BEQ
        testResults = testResults and testAssemblySnippet("BEQ",
                arrayListOf("LDY #1", "LDX #1", "BEQ done", "INY", "DEX", "BEQ done", "DEY", "done: BRK" ),
                arrayListOf(Pair("Y", 2) ), verBBranch)

        // BNE test 5 + 5 using iny and looping
        testResults = testResults and testAssemblySnippet("BNE",
                arrayListOf("LDY #5", "LDX #5", "add: INY", "DEX", "BNE add", "BRK" ),
                arrayListOf(Pair("Y", 10) ), verBBranch)

        // BMI -> ABS(-16) the hard way
        testResults = testResults and testAssemblySnippet("BMI",
                arrayListOf("LDX #\$F0", "LDY #0", "add: INY", "INX", "BMI add", "BRK" ),
                arrayListOf(Pair("Y", 16) ), verBBranch)

        // BPL - Count to 28 the hard way
        testResults = testResults and testAssemblySnippet("BPL",
                arrayListOf("LDX #100", "LDY #0", "count: INY", "INX", "BPL count", "BRK" ),
                arrayListOf(Pair("Y", 28) ), verBBranch)

        if ( ! testResults) { println("Failed Basic Branching tests!"); return false }

        // * Arithmetic (adc and sbc) *

        val verAdd = verbose

        // ADC immediate with overflow
        testResults = testResults and testAssemblySnippet("ADC immediate with overflow",
                arrayListOf("CLD", "LDA #64", "CLC", "ADC #64", "BRK" ),
                arrayListOf(Pair("A", 128), Pair("V", 1), Pair("N", 1), Pair("C", 0), Pair("Z", 0) ), verAdd)

        // zero page using BCD
        testResults = testResults and testAssemblySnippet("zero page using BCD",
                arrayListOf("SED", "LDA #\$10", "CLC", "ADC 25", "BRK", ".ORG 25", ".BYTE $15"),
                arrayListOf(Pair("A", 0x25), Pair("N", 0), Pair("C", 0), Pair("Z", 0) ), verAdd)

        // Zero page,x with BCD and carry no carry result
        testResults = testResults and testAssemblySnippet("Zero page,x with BCD and carry no carry result",
                arrayListOf("SED", "LDA #\$10", "LDX #0", "SEC", "ADC 25,X", "BRK", ".ORG 25", ".BYTE $15"),
                arrayListOf(Pair("A", 0x26), Pair("N", 0), Pair("C", 0), Pair("Z", 0) ), verAdd)

        // absolute with carry resulting in carry result and zero
        testResults = testResults and testAssemblySnippet("absolute with carry resulting in carry result and zero",
                arrayListOf("CLD", "LDA #128", "SEC", "ADC 512", "BRK", ".ORG 512", ".BYTE 127"),
                arrayListOf(Pair("A", 0), Pair("V", 0), Pair("N", 0), Pair("C", 1), Pair("Z", 1) ), verAdd)

        // absolute,x overflow resulting in zero
        testResults = testResults and testAssemblySnippet("absolute,x overflow resulting in zero",
                arrayListOf("CLD", "LDX #2", "LDA #128", "CLC", "ADC 512,X", "BRK", ".ORG 512", ".BYTE 0 0 128"),
                arrayListOf(Pair("A", 0), Pair("V", 1), Pair("N", 0), Pair("C", 1), Pair("Z", 1) ), verAdd)

        // absolute,y normal add
        testResults = testResults and testAssemblySnippet("absolute,y normal add",
                arrayListOf("CLD", "LDY #0", "LDA #50", "CLC", "ADC 512,Y", "BRK", ".ORG 512", ".BYTE 25"),
                arrayListOf(Pair("A", 75), Pair("V", 0), Pair("N", 0), Pair("C", 0), Pair("Z", 0) ), verAdd)

        // (indirect, x) negative but normal add
        testResults = testResults and testAssemblySnippet("(indirect, x) negative but normal add",
                arrayListOf("CLD", "LDX #1", "LDA #255", "CLC", "ADC (25,X)", "BRK", ".ORG 26", ".WORD 512", ".ORG 512", ".BYTE 254"),
                arrayListOf(Pair("A", 253), Pair("V", 0), Pair("N", 1), Pair("C", 1), Pair("Z", 0) ), verAdd)

        // (indirect),y negative + positive (carry but no overflow)
        testResults = testResults and testAssemblySnippet("(indirect),y negative + positive (carry but no overflow)",
                arrayListOf("CLD", "LDY #1", "LDA #255", "CLC", "ADC (25),Y", "BRK", ".ORG 25", ".WORD 512", ".ORG 512", ".BYTE 0 127"),
                arrayListOf(Pair("A", 126), Pair("V", 0), Pair("N", 0), Pair("C", 1), Pair("Z", 0) ), verAdd)

        // ADC immediate mixed that shouldn't overflow
        testResults = testResults and testAssemblySnippet("N+P shouldn't overflow",
                arrayListOf("CLD", "LDA #128", "CLC", "ADC #127", "BRK" ),
                arrayListOf(Pair("A", 255), Pair("V", 0) ), verAdd)

        if ( ! testResults) { println("Failed ADC tests!"); return false }

        // * Arithmetic (sbc) *

        val verSub = true // verbose

        // SBC immediate with underflow
        testResults = testResults and testAssemblySnippet("SBC immediate with overflow",
                arrayListOf("CLD", "LDA #64", "SEC", "SBC #191", "BRK" ),
                arrayListOf(Pair("A", 129), Pair("V", 1), Pair("N", 1), Pair("C", 0), Pair("Z", 0) ), verSub)

        // zero page using BCD
        testResults = testResults and testAssemblySnippet("SBC zero page using BCD",
                arrayListOf("SED", "LDA #\$15", "SEC", "SBC 25", "BRK", ".ORG 25", ".BYTE $10"),
                arrayListOf(Pair("A", 0x05), Pair("N", 0), Pair("C", 1), Pair("Z", 0) ), verSub)

        // Zero page,x with BCD and underflow
        testResults = testResults and testAssemblySnippet("SVC Zero page,x with BCD and underflow",
                arrayListOf("SED", "LDA #\$10", "LDX #0", "SEC", "SBC 25,X", "BRK", ".ORG 25", ".BYTE $95"),
                arrayListOf(Pair("A", 0x15), Pair("N", 0), Pair("C", 0), Pair("Z", 0) ), verSub)

        // sbc bsolute with no carry res zero
        testResults = testResults and testAssemblySnippet("sbc bsolute with no carry res zero",
                arrayListOf("CLD", "LDA #64", "CLC", "SBC 512", "BRK", ".ORG 512", ".BYTE 63"),
                arrayListOf(Pair("A", 0), Pair("V", 0), Pair("N", 0), Pair("C", 1), Pair("Z", 1) ), verSub)

        // absolute,x overflow (-128-1 = 127?)
        testResults = testResults and testAssemblySnippet("sbc absolute,x overflow",
                arrayListOf("CLD", "LDX #2", "LDA #128", "SEC", "SBC 512,X", "BRK", ".ORG 512", ".BYTE 0 0 1"),
                arrayListOf(Pair("A", 127), Pair("V", 1), Pair("N", 0), Pair("C", 1), Pair("Z", 0) ), verSub)

        // sbc absolute,y normal
        testResults = testResults and testAssemblySnippet("sbc absolute,y normal",
                arrayListOf("CLD", "LDY #0", "LDA #50", "SEC", "SBC 512,Y", "BRK", ".ORG 512", ".BYTE 25"),
                arrayListOf(Pair("A", 25), Pair("V", 0), Pair("N", 0), Pair("C", 1), Pair("Z", 0) ), verSub)

        // sbc (indirect, x) negative but normal
        testResults = testResults and testAssemblySnippet("sbc (indirect, x) negative but normal",
                arrayListOf("CLD", "LDX #1", "LDA #254", "SEC", "SBC (25,X)", "BRK", ".ORG 26", ".WORD 512", ".ORG 512", ".BYTE 255"),
                arrayListOf(Pair("A", 255), Pair("V", 0), Pair("N", 1), Pair("C", 0), Pair("Z", 0) ), verSub)

        // sbc (indirect),y negative - positive
        testResults = testResults and testAssemblySnippet("SBC (indirect),y negative - positive",
                arrayListOf("CLD", "LDY #1", "LDA #255", "SEC", "SBC (25),Y", "BRK", ".ORG 25", ".WORD 512", ".ORG 512", ".BYTE 0 2"),
                arrayListOf(Pair("A", 253), Pair("V", 0), Pair("N", 1), Pair("C", 1), Pair("Z", 0) ), verSub)

        return testResults
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
        if ( ! m6502Tests.testM6502(m6502, cntr)) {
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

    println(if (m6502Tests.testDirectives(verbose)) "Can work with directives" else "*** PROBLEMS WITH DIRECTIVES ***")
    println(if (m6502Tests.testRunningInstrutions(verbose)) "Processor instructions work" else "*** PROBLEMS WITH PROCESSOR EMULATION ***")

}