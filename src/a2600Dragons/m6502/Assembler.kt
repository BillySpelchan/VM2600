package a2600Dragons.m6502

class AssemblyException(s:String) : Throwable(s)

/** Types of token types used by the assembler */
enum class AssemblerTokenTypes {
    DIRECTIVE,                          // Assembly commands start with a dot
    ERROR,                              // Invalid character or unparsable line
    IMMEDIATE,                          // # symbol indicating to use immediate mode
    INDEX_X, INDEX_Y,                   // ,X or ,Y
    INDIRECT_START, INDIRECT_END,       // ( to start a indirect block, ) to end
    LABEL_DECLARATION, LABEL_LINK,      // tokens for labels. Declarations end in a :, links are just the label name
    NUMBER,                             // Numeric values. loose numbers decimal, $ for hex, and % for binary
    OPCODE,                             // Assembly op code
    WHITESPACE                          // spaces, tabs, and comments
}

/** Information about token on line:
 *      Type is the token type,
 *      contents is actual content that resulted in this token,
 *      num is the numeric value this token represents if any
 */
data class AssemblerToken(val type:AssemblerTokenTypes, val contents:String, val num:Int)


/** Differnt types of labels used in the assembler */
enum class AssemblerLabelTypes {
    TARGET_VALUE,                       // Holds the address or value that the label represents
    HIGH_BYTE,                          // treat label as variable and use high byte of it
    LOW_BYTE,                           // treat label as variable and use low byte of it
    RELATIVE_ADDRESS,                   // for relative addressing mode so distance from target
    ZERO_PAGE_ADDRESS,                  // low byte of address as there should be no high byte
    ADDRESS                             // treat as absolute address

}

/** Information about labels:
 *      label is the name of the label
 *      targetAddress is where (bank and address) is where label leads to
 *      linkAddresses is an array holding where in code the links are used as 2 byte addresses
 *      relativeLinkAddress array of where links used as relative (1 byte range) addresses
 */
data class AssemblerLabel(val labelName:String,
                          val typeOfLabel:AssemblerLabelTypes,
                          val addressOrValue: Int,
                          val bank:Int = 0)

/** Bank for storing resulting machine language data */
class AssemblyBank(val number:Int, var size:Int = 4096, var bankOrigin:Int = 0) {
    //
    var storage:ByteArray = ByteArray(size)
    var curAddress:Int = bankOrigin

    fun curCPUAddress():Int {
        return curAddress + bankOrigin;
    }

    fun readBankAddress(offset:Int):Int {
        if ((offset < 0) or (offset >= size))
            throw OutOfMemoryError("Requested address not in assembly bank")
        return (storage[offset].toInt() and 255)

    }

    /** writes byte to next assembly address incremeting address.
     * @return the next CPU Address
     * @throws AssemblyException if try to write past end of bank
     */
    fun writeNextByte(data:Int):Int {
        if (curAddress >= size)
            throw AssemblyException("Past end of assembly bank")
        storage.set(curAddress, data.toByte())
        ++curAddress
        return curAddress + bankOrigin
    }

    /** Writes byte at indicated bank address (0 being start of bank)
     * @throws AssemblyException if requested byte not within bank range
     */
    fun writeBankByte(offset:Int, data:Int) {
        if ((offset < 0) or (offset >= size))
            throw OutOfMemoryError("Requested address not in assembly bank")
        storage.set(offset, data.toByte())
    }

    /** Writes to bank using CPU relative address (so relative to bank origin)
     * @throws AssemblyException if requested byte not within bank range
     */
    fun writeCPUByte(address:Int, data:Int) {
        writeBankByte(address - bankOrigin, data)
    }

    fun resized(bankSize:Int) {
        if (size == bankSize)
            return;
        val oldSize = size
        val oldStorage = storage
        storage = ByteArray(bankSize)
        size = bankSize
        val bytesToCopy = Math.min(oldSize, size);
        for (offset in 0..(bytesToCopy-1))
            storage[offset] = oldStorage[offset]
    }

    fun bankToIntArray():Array<Int> {
        val list = Array<Int> (size, {cntr -> storage[cntr].toInt() and 255})
        return list;
    }
}


/**
 * Assembles code into assembly language. I am using a 1.5 pass approach where labels are tracked durring the first
 * pass with the addresses set to proper values during a linking phase where the list of labels are parsed and code
 * that uses the label are updated to reflect the proper label.
 */
class Assembler(val m6502: M6502, var isVerbose:Boolean = false) {
    var mapOfOpCodes = HashMap<String, ArrayList<M6502Instruction>>()
    var assemblyLine = 0
//    var addressInMemory = 0
//    var assemblyAddress = 0
    var currentBank = AssemblyBank(0)

    var labelList = HashMap<String, ArrayList<AssemblerLabel>>()
    var banks = ArrayList<AssemblyBank>()

    /** build a hash-map of the mnemonics with all of the op codes so a particular mnemonic can seek out addressing
     * modes th    var currentBank = 0;
    at a mnemonic supports to find the appropriate op code for assembly
      */
    init {
        for (inst in m6502.commands) {
            if (mapOfOpCodes.containsKey(inst.OPString))
                mapOfOpCodes[inst.OPString]!!.add(inst)
            else {
                mapOfOpCodes.put(inst.OPString, arrayListOf<M6502Instruction>(inst))
            }
        }
        banks.add(currentBank)
    }

    /** Verbose mode prints out extra details while assembling the program */
    fun verbose(s:String, newline:Boolean = true) {
        if (isVerbose) {
            print(s)
            if (newline)
                println()
        }
    }

    // ************************
    // *** label management ***
    // ************************

    fun addLabel(label:AssemblerLabel) {
//        verbose("adding label ${label.labelName} at ${label.addressOrValue}")
        // see if already a label with this name
        if ( !labelList.containsKey(label.labelName)) {
            val resultList = ArrayList<AssemblerLabel>();
            resultList.add(label)
            labelList.put(label.labelName, resultList)
        } else {
            val resultList:ArrayList<AssemblerLabel> =  labelList[label.labelName]!!
            resultList.add(label)
        }
    }


    fun linkLabelsInMemory():ArrayList<String> {
        val errorList = ArrayList<String>();
        for ( (label, links) in labelList) {
            verbose("Processing links for label $label:")
            // find target as if no target there is a problem
            var linkTarget:AssemblerLabel? = null
            for (asmlink in links) {
                if (asmlink.typeOfLabel == AssemblerLabelTypes.TARGET_VALUE) {
                    // warn about duplicate targets but default to last found
                    if (linkTarget != null)
                        errorList.add("Warning: ${asmlink.labelName} declared multiple times!")
                    linkTarget = asmlink;
                    verbose(" link target is ${linkTarget.addressOrValue}")
                }
            }

            if (linkTarget == null) {
                val s = "ERROR: No target for $label label!"
                errorList.add(s)
                verbose(s)
            } else {
                for (asmlink in links) {
                    when (asmlink.typeOfLabel) {
                        AssemblerLabelTypes.RELATIVE_ADDRESS -> {
                            val baseAddress = asmlink.addressOrValue + 1
                            val offset = (linkTarget.addressOrValue - baseAddress) and 255
                            banks[asmlink.bank].writeBankByte(asmlink.addressOrValue, offset)
                        }

                        AssemblerLabelTypes.TARGET_VALUE -> {/*Ignore*/}

                        AssemblerLabelTypes.HIGH_BYTE -> {
                            val targetHigh:Int = (linkTarget.addressOrValue / 256) and 255
                            banks[asmlink.bank].writeBankByte(asmlink.addressOrValue,  targetHigh)
                        }

                        AssemblerLabelTypes.LOW_BYTE ,
                        AssemblerLabelTypes.ZERO_PAGE_ADDRESS -> {
                            val targetLow = linkTarget.addressOrValue and 255
                            banks[asmlink.bank].writeBankByte(asmlink.addressOrValue, targetLow)
                        }

                        AssemblerLabelTypes.ADDRESS -> {
                            val targetLow = linkTarget.addressOrValue and 255
                            banks[asmlink.bank].writeBankByte(asmlink.addressOrValue, targetLow)
                            val targetHigh:Int = (linkTarget.addressOrValue / 256) and 255
                            banks[asmlink.bank].writeBankByte(asmlink.addressOrValue+1, targetHigh)
                        }
                    }
                    if (asmlink.typeOfLabel != AssemblerLabelTypes.TARGET_VALUE)
                        verbose("${asmlink.addressOrValue}, ", false)
                }
                verbose("")
            }

        }

        return errorList;
    }

    // ***********************
    // *** Tokenizing step ***
    // ***********************

    /** convert a given line into tokens, with option to ignore any whitespace tokens */
    fun tokenize(line:String, ignoreWhitespace:Boolean=true):ArrayList<AssemblerToken> {
        val resultList = ArrayList<AssemblerToken>()
        var indx = 0
        var currentTokenData:String = ""
        var currentTokenType:AssemblerTokenTypes = AssemblerTokenTypes.WHITESPACE;
        var currentTokenNum:Int = 0
        val lineLen = line.length
        while (indx < lineLen) {
            val tokenSymbol = line[indx]
            when (tokenSymbol ) {
                // Handle whitespace
                ' ', '\t' -> {
                    currentTokenNum = 1
                    ++indx
                    while ((indx < lineLen) && (line[indx].isWhitespace())) {
                        ++indx; ++currentTokenNum
                    }
                    if (!ignoreWhitespace)
                        resultList.add(AssemblerToken(AssemblerTokenTypes.WHITESPACE, " ", currentTokenNum))
                    --indx
                }

                // handle comments by adding remainder of line as whitespace token
                ';' -> {
                    if (!ignoreWhitespace)
                        resultList.add(AssemblerToken(AssemblerTokenTypes.WHITESPACE, line.substring(indx), lineLen - indx))
                    indx = lineLen;
                }

                // immediate mode indicator
                '#' -> resultList.add(AssemblerToken(AssemblerTokenTypes.IMMEDIATE, "#", 0))

                // index indicator so find out which index
                ',' -> {
                    // skip whitespace
                    ++indx; while ((indx < lineLen) && (line[indx].isWhitespace())) ++indx
                    if (indx >= lineLen) {
                        resultList.add(AssemblerToken(AssemblerTokenTypes.ERROR, "Missing index on index token", -1))
                    } else if (line[indx] == 'X') {
                        resultList.add(AssemblerToken(AssemblerTokenTypes.INDEX_X, ",X", 0))
                    } else if (line[indx] == 'Y') {
                        resultList.add(AssemblerToken(AssemblerTokenTypes.INDEX_Y, ",Y", 0))
                    } else {
                        indx = lineLen
                        resultList.add(AssemblerToken(AssemblerTokenTypes.ERROR, "Missing index on index token", -1))
                    }
                }

                // Assembler directives start with a .
                '.' -> {
                    // find whitespace to mark end
                    currentTokenData = ""
                    ++indx
                    while ((indx < lineLen) && ( !line[indx].isWhitespace())) {
                        currentTokenData += line[indx]
                        ++indx
                    }
                    --indx
                    resultList.add(AssemblerToken(AssemblerTokenTypes.DIRECTIVE, currentTokenData, 0))
                }

                // indirect addresses are in ()
                '(' -> resultList.add(AssemblerToken(AssemblerTokenTypes.INDIRECT_START, "(", 0))
                ')' -> resultList.add(AssemblerToken(AssemblerTokenTypes.INDIRECT_END, ")", 0))

                // Hex number
                '$' -> {
                    ++indx
                    var hexString = ""
                    while ((indx < lineLen) && ( line[indx].isLetterOrDigit())) {
                        hexString += line[indx]
                        ++indx
                    }
                    currentTokenNum = hexString.toInt(16)
                    resultList.add(AssemblerToken(AssemblerTokenTypes.NUMBER, "\$$hexString", currentTokenNum))
                    --indx
                }

                // Binary number
                '%' -> {
                    ++indx
                    var binaryString = ""
                    while ((indx < lineLen) && ( (line[indx] == '1') || (line[indx] == '0') )) {
                        binaryString += line[indx]
                        ++indx
                    }
                    currentTokenNum = binaryString.toInt(2)
                    resultList.add(AssemblerToken(AssemblerTokenTypes.NUMBER, "%$binaryString", currentTokenNum))
                    --indx
                }

                // decimal numbers
                in '0'..'9' -> {
                    ++indx
                    currentTokenData = "$tokenSymbol"
                    while ((indx < lineLen) && ( line[indx].isDigit())) {
                        currentTokenData += line[indx]
                        ++indx
                    }
                    currentTokenNum = currentTokenData.toInt()
                    resultList.add(AssemblerToken(AssemblerTokenTypes.NUMBER, currentTokenData, currentTokenNum))
                    --indx
                }

                // , labels, opcodes
                in 'A'..'Z', in 'a'..'z' -> {
                    ++indx
                    currentTokenData = "$tokenSymbol"
                    while ((indx < lineLen) && ( (line[indx].isLetterOrDigit()) || (line[indx] == '_') )) {
                        currentTokenData += line[indx]
                        ++indx
                    }
                    if ((indx < lineLen) && (line[indx] == ':'))
                        resultList.add(AssemblerToken(AssemblerTokenTypes.LABEL_DECLARATION, currentTokenData, 0))
                    else {
                        if (mapOfOpCodes.containsKey(currentTokenData.toUpperCase()))
                            resultList.add(AssemblerToken(AssemblerTokenTypes.OPCODE, currentTokenData.toUpperCase(), 0))
                        else
                            resultList.add(AssemblerToken(AssemblerTokenTypes.LABEL_LINK, currentTokenData, 0))
                        --indx
                    }
                }

                else -> {
                    indx = lineLen
                    resultList.add(AssemblerToken(AssemblerTokenTypes.ERROR, "Unknown or unexpeted symbol", -1))
                }
            }
            ++indx
        }
        return resultList
    }


    /**
     * returns op code with that address mode, returning -1 for invalid requests
     */
    fun getOpcodeWithAddressMode(opString:String, mode:AddressMode) : Int {
        if (!mapOfOpCodes.containsKey(opString))
            return -1;
        val instList = mapOfOpCodes[opString];
        var opCode = -1;
        for (inst in instList!!)
            if (inst.addressMode == mode)
                opCode = inst.OPCode;
        return opCode
    }

    /**
     *
     */
    fun createAssemblyInstruction(opString:String, mode:AddressMode, target:Int):Array<Int> {
        val opCode = getOpcodeWithAddressMode(opString, mode);
        if (opCode == -1)
            return arrayOf()
        val instructionSize = m6502.commands[opCode].size
        if (instructionSize == 1)
            return arrayOf(opCode)
        else if (instructionSize == 2) {
            return arrayOf(opCode, target and 255)
        } else {
            val targetHigh:Int = (target / 256) and 255
            val targetLow = target and 255
            return arrayOf(opCode, targetLow, targetHigh)
        }
    }

    fun parse(tokens:ArrayList<AssemblerToken>):Array<Int> {
        // empty (comments, whitespace only) lines are ignored
        var resultArray:Array<Int> = arrayOf()
        if (tokens.size == 0) return resultArray

        var indx = 0;

        // see if line starts with a label
        if (tokens[indx].type == AssemblerTokenTypes.LABEL_DECLARATION) {
            // add label system
            addLabel(AssemblerLabel(tokens[indx].contents, AssemblerLabelTypes.TARGET_VALUE, currentBank.curAddress, currentBank.number))
            ++indx
            if (indx >= tokens.size)
                return resultArray
        }

        // see if line is a dirrective
        if (tokens[indx].type == AssemblerTokenTypes.DIRECTIVE) {
            println("TODO Directives not implemented yet")
            return resultArray
        }

        // if reach here and not an opcode not a valid line
        if (tokens[indx].type != AssemblerTokenTypes.OPCODE)
            throw AssemblyException("Expecting an OP code, got ${tokens[indx].type}")

        val opString = tokens[indx].contents
        ++indx

        if (indx >= tokens.size)
            resultArray = createAssemblyInstruction(opString, AddressMode.IMPLIED, 0)
        else {
            when (tokens[indx].type) {
                AssemblerTokenTypes.LABEL_LINK -> {
                    // special case label a or A for accumulator mode
                    val labelString = tokens[indx].contents;
                    ++indx;
                    if ((labelString == "A") || (labelString == "a")) {
                        resultArray = createAssemblyInstruction(opString, AddressMode.ACCUMULATOR, 0)
                    } else if (getOpcodeWithAddressMode(opString, AddressMode.RELATIVE) > 0) {
                        // add relative label link
                        addLabel(AssemblerLabel(labelString, AssemblerLabelTypes.RELATIVE_ADDRESS, currentBank.curAddress+1, currentBank.number))
                        resultArray = createAssemblyInstruction(opString, AddressMode.RELATIVE, 0)
                    } else if (indx >= tokens.size) {
                        // add address label link
                        addLabel(AssemblerLabel(labelString, AssemblerLabelTypes.ADDRESS, currentBank.curAddress+1, currentBank.number))
                        resultArray = createAssemblyInstruction(opString, AddressMode.ABSOLUTE, 0)
                    } else {
                        val indexToken = tokens[indx];
                        ++indx
                        if (indexToken.type == AssemblerTokenTypes.INDEX_X) {
                            // add address label link
                            addLabel(AssemblerLabel(labelString, AssemblerLabelTypes.ADDRESS, currentBank.curAddress+1, currentBank.number))
                            resultArray = createAssemblyInstruction(opString, AddressMode.ABSOLUTE_X, 0)
                        } else if (indexToken.type == AssemblerTokenTypes.INDEX_Y) {
                            // add address label link
                            addLabel(AssemblerLabel(labelString, AssemblerLabelTypes.ADDRESS, currentBank.curAddress+1, currentBank.number))
                            resultArray = createAssemblyInstruction(opString, AddressMode.ABSOLUTE_Y, 0)
                        } else
                            throw AssemblyException("Unexpected tokens after label")
                    }

                    if (resultArray.size == 0)
                        throw AssemblyException("Provided instructions not valid machine language")
                }

                AssemblerTokenTypes.NUMBER -> {
                    val num = tokens[indx].num;
                    ++indx
                    if (getOpcodeWithAddressMode(opString, AddressMode.RELATIVE) > 0) {
                        val target = num - (currentBank.curAddress + 2);
                        resultArray = createAssemblyInstruction(opString, AddressMode.RELATIVE, target)
                    } else if (indx >= tokens.size) {
                        if (num > 255)
                            resultArray = createAssemblyInstruction(opString, AddressMode.ABSOLUTE, num)
                        else
                            resultArray = createAssemblyInstruction(opString, AddressMode.ZERO_PAGE, num)
                    } else {
                        val indexToken = tokens[indx];
                        ++indx
                        if (indexToken.type == AssemblerTokenTypes.INDEX_X) {
                            if (num > 255)
                                resultArray = createAssemblyInstruction(opString, AddressMode.ABSOLUTE_X, num)
                            else
                                resultArray = createAssemblyInstruction(opString, AddressMode.ZERO_PAGE_X, num)
                        } else if (indexToken.type == AssemblerTokenTypes.INDEX_Y) {
                            if (num > 255)
                                resultArray = createAssemblyInstruction(opString, AddressMode.ABSOLUTE_Y, num)
                            else
                                resultArray = createAssemblyInstruction(opString, AddressMode.ZERO_PAGE_Y, num)
                        }
                        else
                            throw AssemblyException("Unexpected tokens after label")
                    }
                }

                AssemblerTokenTypes.IMMEDIATE -> {
                    ++indx
                    if ((indx >= tokens.size) || (tokens[indx].type != AssemblerTokenTypes.NUMBER))
                        throw AssemblyException("Expecting a number and got ${tokens[indx].type}")
                    else
                        resultArray = createAssemblyInstruction(opString, AddressMode.IMMEDIATE, tokens[indx].num)
                    ++indx
                }

                AssemblerTokenTypes.INDIRECT_START -> {
                    var addr = 0
                    ++indx
                    if (indx >= tokens.size) throw AssemblyException("Unexpected end of statement")
                    if (tokens[indx].type == AssemblerTokenTypes.LABEL_LINK) {
                        // process label link for indirect address modes
                        addLabel(AssemblerLabel(tokens[indx-1].contents, AssemblerLabelTypes.ADDRESS, currentBank.curAddress+1, currentBank.number))
                    } else if (tokens[indx].type == AssemblerTokenTypes.NUMBER) {
                        addr = tokens[indx].num;
                    } else {
                        throw AssemblyException("Expected number or label but got ${tokens[indx].type}")
                    }
                    ++indx
                    if (indx >= tokens.size) throw AssemblyException("Unexpected end of statement")
                    if (tokens[indx].type == AssemblerTokenTypes.INDEX_X) {
                        resultArray = createAssemblyInstruction(opString, AddressMode.INDIRECT_X, addr);
                        // assume proper closing of statement
                        indx += 2
                    } else if (tokens[indx].type == AssemblerTokenTypes.INDIRECT_END) {
                        ++indx
                        if (indx >= tokens.size) {
                            resultArray = createAssemblyInstruction(opString, AddressMode.INDIRECT, addr);
                        } else if (tokens[indx].type == AssemblerTokenTypes.INDEX_Y) {
                            resultArray = createAssemblyInstruction(opString, AddressMode.INDIRECT_Y, addr);
                        } else {
                            throw AssemblyException("Unknown indirect address mode specified")
                        }
                        ++indx
                    } else {
                        throw AssemblyException("Malformed indirect adress instruction")
                    }
                }
                else -> {
                    throw AssemblyException("Unable to determine instruction address mode")
                }
            }
        }

        if (resultArray.size == 0)
            throw AssemblyException("Provided instructions not valid machine language")

        if (indx < tokens.size)
            println("WARNING $assemblyLine additional code after parsed assembly exists but ignored.")
        return resultArray
    }

    fun assembleProgram(source:ArrayList<String>):Int {
        // clear previous assembly (if any)
        banks.clear()
        currentBank = AssemblyBank(0)
        banks.add(currentBank)
        assemblyLine = 0;

        var errorCode = 0;
        for (line in source) {
            ++assemblyLine
            verbose("$assemblyLine $line -> ${currentBank.curAddress.toString(16)}: ", false)
            var tokens = tokenize(line)
            try {
                var ml = parse(tokens)
                if (ml.size > 0)
                    for (data in ml) {
                        currentBank.writeNextByte(data)
                        verbose("$${data.toString(16)},", false)
                    }
                verbose("")
            } catch (iae:AssemblyException) {
                errorCode = 2;
            }
        }
        var errors = linkLabelsInMemory()
        if (errors.size > 0) {
            errorCode = 1;
            for (errmessage in errors)
                println(errmessage)
        }

        return errorCode
    }

}