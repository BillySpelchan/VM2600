package a2600Dragons.m6502

class AssemblyException(s:String) : Throwable(s)

// =================================================================================================================

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

// =================================================================================================================

/** Information about token on line:
 *      Type is the token type,
 *      contents is actual content that resulted in this token,
 *      num is the numeric value this token represents if any
 */
data class AssemblerToken(val type:AssemblerTokenTypes, val contents:String, val num:Int)

// =================================================================================================================

/** Different types of labels used in the assembler */
enum class AssemblerLabelTypes {
    TARGET_VALUE,                       // Holds the address or value that the label represents
    HIGH_BYTE,                          // treat label as variable and use high byte of it
    LOW_BYTE,                           // treat label as variable and use low byte of it
    RELATIVE_ADDRESS,                   // for relative addressing mode so distance from target
    ZERO_PAGE_ADDRESS,                  // low byte of address as there should be no high byte
    ADDRESS                             // treat as absolute address

}

// =================================================================================================================

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

// =================================================================================================================

/** Bank for storing resulting machine language data */
class AssemblyBank(val number:Int, var size:Int = 4096, var bankOrigin:Int = 0) {
    //
    private var storage:ByteArray = ByteArray(size)
    var curAddress:Int = bankOrigin

    fun curBankAddress():Int {
        return curAddress - bankOrigin
    }

    fun readBankAddress(offset:Int):Int {
        if ((offset < 0) or (offset >= size))
            throw OutOfMemoryError("Requested address not in assembly bank")
        return (storage[offset].toInt() and 255)
    }


    /** writes byte to next assembly address incrementing address.
     * @return the next CPU Address
     * @throws AssemblyException if try to write past end of bank
     */
    fun writeNextByte(data:Int):Int {
        val offset = curBankAddress()
        if ((offset < 0) or (offset >= size))
            throw AssemblyException("Writing outside of assembly bank")
        storage[offset] = data.toByte()
        ++curAddress
        return curAddress
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

    fun resize(bankSize:Int) {
        if (size == bankSize)
            return
        val oldSize = size
        val oldStorage = storage
        storage = ByteArray(bankSize)
        size = bankSize
        val bytesToCopy = Math.min(oldSize, size)
        for (offset in 0..(bytesToCopy-1))
            storage[offset] = oldStorage[offset]
    }

    fun bankToIntArray():Array<Int> {
        return Array (size, {cntr -> storage[cntr].toInt() and 255})
    }
}

// =================================================================================================================

/**
 * Holds the macro information which is simply an array of tokens that will get passed to the assembler when the
 * macro is invoked. Macros created with .MSTART label p0 .. p9 followed by the code to be inserted. Labels
 * within the code get tracked so internal labels get prefixed so each occurance will be unique labels. .MEND ends
 * the macro with .MACRO name p0 .. p9 used to invoke the macro.
 */
class AssemblerMacro(val label:String) {
    private var paramDefaults = Array(10, {AssemblerToken(AssemblerTokenTypes.NUMBER, "0", 0)})
    private val paramNames = arrayListOf("P0", "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9")
    private var labels = mutableListOf<String>()
    private var executionCount = 0
    private var macroLines = arrayListOf<ArrayList<AssemblerToken>>()

    /** sets the parameter to use if not specified by .MACRO command */
    fun setDefaultParameter(paramID:Int, token:AssemblerToken) {
        paramDefaults[paramID] = token
    }

    /**
     * Adds lines to the macro returns true if macro is finished, false if still waiting for .MEND
     */
    fun addLine(line:ArrayList<AssemblerToken>):Boolean {
        if (line.size < 1)
            return false
        // TODO do we want to do a more thorough check here? Probably should allow anywhere on line?
        if ((line[0].type == AssemblerTokenTypes.DIRECTIVE) and (line[0].contents == "MEND")) {
            return true
        }
        if (line[0].type == AssemblerTokenTypes.LABEL_DECLARATION)
            labels.add(line[0].contents)
        macroLines.add(line)
        return false
    }

    /**
     * Runs the provided macro replacing internal labels with prefixed versions and replacing labels P0..P9
     * with the parameters that were passed using default parameters for missing parameters.
     */
    fun execute(assembler:Assembler, userParams:ArrayList<AssemblerToken>) {
        // set params to default in case user didn't pass all of them
        val params = paramDefaults.copyOf()
        for (cntr in 0..userParams.size-1)
            params[cntr] = userParams[cntr]

        // make prefix for any declared labels
        val prefix = "${executionCount}_"
        ++executionCount

        // run the lines through the assembler
        for (cntrLine in 0 until macroLines.size) {
            val adjustedLine = ArrayList<AssemblerToken>(macroLines[cntrLine])

            for (cntrToken in 0 until adjustedLine.size) {
                // adjust the internal labels and parameter references to the proper properties
                val token:AssemblerToken = adjustedLine[cntrToken]
                if (token.type == AssemblerTokenTypes.LABEL_DECLARATION)
                    adjustedLine[cntrToken] = AssemblerToken(AssemblerTokenTypes.LABEL_DECLARATION,
                            prefix + token.contents, token.num)
                else if (token.type == AssemblerTokenTypes.LABEL_LINK) {
                    if  (labels.contains(token.contents))
                        adjustedLine[cntrToken] = AssemblerToken(AssemblerTokenTypes.LABEL_LINK,
                                prefix + token.contents, token.num)
                    else if (paramNames.contains(token.contents))
                        adjustedLine[cntrToken] = params[paramNames.indexOf(token.contents)]
                }
            }

            // run adjusted line through the parser
            val ml = assembler.parse(adjustedLine)
            if (ml.isNotEmpty()) {
                for (data in ml) {
                    assembler.currentBank.writeNextByte(data)
                }
            }
        }
    }
}


// =================================================================================================================

/**
 * Assembles code into assembly language. I am using a 1.5 pass approach where labels are tracked during the first
 * pass with the addresses set to proper values during a linking phase where the list of labels are parsed and code
 * that uses the label are updated to reflect the proper label.
 */
class Assembler(private val m6502: M6502, private var isVerbose:Boolean = false) {
    private var mapOfOpCodes = HashMap<String, ArrayList<M6502Instruction>>()
    private var assemblyLine = 0
    var currentBank = AssemblyBank(0)

    private var variableList = HashMap<String, AssemblerToken>()
    private var labelList = HashMap<String, ArrayList<AssemblerLabel>>()
    var banks = ArrayList<AssemblyBank>()

    private var macroList = HashMap<String, AssemblerMacro>()
    private var macroInProgress:AssemblerMacro? = null

    /** build a hash-map of the mnemonics with all of the op codes so a particular mnemonic can seek out addressing
     * modes that a mnemonic supports to find the appropriate op code for assembly
      */
    init {
        for (inst in m6502.commands) {
            if (mapOfOpCodes.containsKey(inst.OPString))
                mapOfOpCodes[inst.OPString]!!.add(inst)
            else {
                mapOfOpCodes.put(inst.OPString, arrayListOf(inst))
            }
        }
        banks.add(currentBank)
    }

    /** Verbose mode prints out extra details while assembling the program */
    private fun verbose(s:String, newline:Boolean = true) {
        if (isVerbose) {
            print(s)
            if (newline)
                println()
        }
    }


    // ************************
    // *** label management ***
    // ************************

    @Suppress("MemberVisibilityCanPrivate")
    fun addLabel(label:AssemblerLabel) {
//        verbose("adding label ${label.labelName} at ${label.addressOrValue}")
        // see if already a label with this name
        if ( !labelList.containsKey(label.labelName)) {
            val resultList = ArrayList<AssemblerLabel>()
            resultList.add(label)
            labelList.put(label.labelName, resultList)
        } else {
            val resultList:ArrayList<AssemblerLabel> =  labelList[label.labelName]!!
            resultList.add(label)
        }
    }


    private fun linkLabelsInMemory():ArrayList<String> {
        val errorList = ArrayList<String>()
        for ( (label, links) in labelList) {
            verbose("Processing links for label $label:")
            // find target as if no target there is a problem
            var linkTarget:AssemblerLabel? = null
            for (asmlink in links) {
                if (asmlink.typeOfLabel == AssemblerLabelTypes.TARGET_VALUE) {
                    // warn about duplicate targets but default to last found
                    if (linkTarget != null)
                        errorList.add("Warning: ${asmlink.labelName} declared multiple times!")
                    linkTarget = asmlink
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
                            banks[asmlink.bank].writeCPUByte(asmlink.addressOrValue, offset)
                        }

                        AssemblerLabelTypes.TARGET_VALUE -> {/*Ignore*/}

                        AssemblerLabelTypes.HIGH_BYTE -> {
                            val targetHigh:Int = (linkTarget.addressOrValue / 256) and 255
                            banks[asmlink.bank].writeCPUByte(asmlink.addressOrValue,  targetHigh)
                        }

                        AssemblerLabelTypes.LOW_BYTE ,
                        AssemblerLabelTypes.ZERO_PAGE_ADDRESS -> {
                            val targetLow = linkTarget.addressOrValue and 255
                            banks[asmlink.bank].writeCPUByte(asmlink.addressOrValue, targetLow)
                        }

                        AssemblerLabelTypes.ADDRESS -> {
                            val targetLow = linkTarget.addressOrValue and 255
                            banks[asmlink.bank].writeCPUByte(asmlink.addressOrValue, targetLow)
                            val targetHigh:Int = (linkTarget.addressOrValue / 256) and 255
                            banks[asmlink.bank].writeCPUByte(asmlink.addressOrValue+1, targetHigh)
                        }
                    }
                    if (asmlink.typeOfLabel != AssemblerLabelTypes.TARGET_VALUE)
                        verbose("${asmlink.addressOrValue}, ", false)
                }
                verbose("")
            }

        }

        return errorList
    }

    // ***********************
    // *** Tokenizing step ***
    // ***********************

    /** convert a given line into tokens, with option to ignore any whitespace tokens */
    fun tokenize(line:String, ignoreWhitespace:Boolean=true):ArrayList<AssemblerToken> {
        val resultList = ArrayList<AssemblerToken>()
        var indx = 0
        var currentTokenData: String
        var currentTokenNum: Int
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
                    indx = lineLen
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

    // ******************
    // *** Directives ***
    // ******************

    private fun processDirectives(tokens: ArrayList<AssemblerToken>) {
        var indx = 0
        while (indx < tokens.size) {
            val token:AssemblerToken = tokens[indx]
            if (token.type == AssemblerTokenTypes.LABEL_LINK) {
                // see if label is a variable and if so replace with value
                if (variableList.containsKey(token.contents)) {
                    tokens[indx] = variableList.get(token.contents)!!
                }
                ++indx
            } else if (token.type == AssemblerTokenTypes.DIRECTIVE) {
                // find token with directive
                val directive = token.contents.toUpperCase()
                tokens.removeAt(indx)
                // based of directive,
                when (directive) {
                // .BANK id [base_address] [size] switches bank, optional bank orgin and size
                    "BANK" -> {
                        if ((indx >= tokens.size) or (tokens[indx].type != AssemblerTokenTypes.NUMBER)) {
                            throw AssemblyException("Invalid bank specified $assemblyLine")
                        }
                        // get parameters for bank directive
                        val bankID = tokens[indx].num
                        tokens.removeAt(indx)
                        var bankOrg = 0
                        var orginParamSet = false
                        var bankSize = 4096
                        var sizeParamSet = false
                        if (indx < tokens.size)
                            if (tokens[indx].type == AssemblerTokenTypes.NUMBER) {
                                bankOrg = tokens[indx].num
                                orginParamSet = true
                                tokens.removeAt(indx)
                                if (indx < tokens.size)
                                    if (tokens[indx].type == AssemblerTokenTypes.NUMBER) {
                                        bankSize = tokens[indx].num
                                        sizeParamSet = true
                                        tokens.removeAt(indx)
                                    }
                            }
                        // apply bank directive
                        if (banks.size > bankID) {
                            currentBank = banks[bankID]
                            if ((orginParamSet) and (currentBank.bankOrigin != bankOrg)) {
                                currentBank.bankOrigin = bankOrg
                                currentBank.curAddress = bankOrg
                            }
                            if ((sizeParamSet) and (currentBank.size != bankSize))
                                currentBank.resize(bankSize)
                        } else {
                            while(banks.size < bankID) {
                                val skippedBank = banks.size
                                banks.add(AssemblyBank(skippedBank))
                            }
                            banks.add(AssemblyBank(bankID, bankSize, bankOrg))
                            currentBank = banks[bankID]
                        }
                    }

                // .BYTE one or more bytes separated by whitespace. only low byte of labels are used if in line.
                    "BYTE" -> {
                        var validEntry = true
                        while ((indx < tokens.size) and (validEntry)) {
                            // replace labels with varible token if they are variables
                            if (tokens[indx].type == AssemblerTokenTypes.LABEL_LINK) {
                                val varToken = variableList.get(tokens[indx].contents)
                                if (varToken != null)
                                    tokens[indx] = varToken
                            }
                            // determine token value (use low byte if label used)
                            val byteToAdd = if (tokens[indx].type == AssemblerTokenTypes.NUMBER) tokens[indx].num
                            else if (tokens[indx].type == AssemblerTokenTypes.LABEL_LINK) {
                                    addLabel(AssemblerLabel(tokens[indx].contents, AssemblerLabelTypes.LOW_BYTE, currentBank.curAddress, currentBank.number))
                                    0
                            } else {
                                validEntry = false
                                0
                            }
                            // if valid then write the value to memory and remove token
                            if (validEntry) {
                                currentBank.writeNextByte(byteToAdd)
                                tokens.removeAt(indx)
                            }
                        }
                    }

                // .EQU set up or change variable - should be done before they are used in code
                    "EQU" -> {
                        if (indx >= (tokens.size + 1)) {
                            throw AssemblyException("Missing parameters for .EQU statement line $assemblyLine")
                        }
                        if (tokens[indx].type != AssemblerTokenTypes.LABEL_LINK) {
                            throw AssemblyException("Missing EQU variable name on line $assemblyLine")
                        }
                        val varName = tokens[indx].contents
                        tokens.removeAt(indx)
                        variableList.put(varName, tokens[indx])
                        tokens.removeAt(indx)
                    }

                // .HIGH takes the high order byte of a number or label address resulting in single byte value
                    "HIGH" -> {
                        if (indx >= tokens.size) {
                            throw AssemblyException("Missing parameters for .HIGH statement line $assemblyLine")
                        }
                        val num = when (tokens[indx].type) {
                            AssemblerTokenTypes.NUMBER -> tokens[indx].num
                            AssemblerTokenTypes.LABEL_LINK -> {
                                val rep = variableList.get(tokens[indx].contents)
                                if (rep == null) {
                                    addLabel(AssemblerLabel(tokens[indx].contents, AssemblerLabelTypes.HIGH_BYTE, currentBank.curAddress+1, currentBank.number))
                                    0
                                } else {
                                    rep.num
                                }
                            }
                            else -> throw AssemblyException("Missing HIGH variable name on line $assemblyLine")
                        }
                        tokens[indx] = AssemblerToken(AssemblerTokenTypes.NUMBER, "high", (num / 256) and 255)
                    }

                // .LOW takes the low order byte of a number or label address resulting in single byte value
                    "LOW" -> {
                        if (indx >= tokens.size) {
                            throw AssemblyException("Missing parameters for .LOW statement line $assemblyLine")
                        }
                        val num = when (tokens[indx].type) {
                            AssemblerTokenTypes.NUMBER -> tokens[indx].num
                            AssemblerTokenTypes.LABEL_LINK -> {
                                val rep = variableList.get(tokens[indx].contents)
                                if (rep == null) {
                                    addLabel(AssemblerLabel(tokens[indx].contents, AssemblerLabelTypes.LOW_BYTE, currentBank.curAddress+1, currentBank.number))
                                    0
                                } else {
                                    rep.num
                                }
                            }
                            else -> throw AssemblyException("Missing .LOW variable name on line $assemblyLine")
                        }
                        tokens[indx] = AssemblerToken(AssemblerTokenTypes.NUMBER, "high", num and 255)
                    }

                // .MSTART label P0 P1 ... P9 -> sets up macro with rest of line being treated as default parameters
                    "MSTART" -> {
                        if (indx >= tokens.size) {
                            throw AssemblyException("Missing Label for .MSTART statement line $assemblyLine")
                        }
                        val macroLabel = tokens[indx].contents
                        tokens.removeAt(indx)
                        macroInProgress = AssemblerMacro(macroLabel)
                        var paramIndx = 0
                        while (tokens.size > indx) {
                            macroInProgress!!.setDefaultParameter(paramIndx, tokens[indx])
                            ++paramIndx
                            tokens.removeAt(indx)
                        }
                    }

                    "MACRO" -> {
                        // find macro to run or throw exception if can't be found
                        if (indx >= tokens.size) {
                            throw AssemblyException("Missing Label for .MACRO statement line $assemblyLine")
                        }
                        val macroToRun = macroList.get(tokens[indx].contents)
                        tokens.removeAt(indx)
                        if (macroToRun == null)
                            throw AssemblyException("Macro ${tokens[indx].contents} Not defined! line $assemblyLine")

                        // set up macro parameters that were passed
                        val macParams = ArrayList<AssemblerToken>()
                        while (indx < tokens.size) {
                            macParams.add(tokens[indx])
                            tokens.removeAt(indx)
                        }

                        // run the macro
                        macroToRun.execute(this, macParams)
                    }

                // .ORG changes address where code is to be generated within current bank
                    "ORG" -> {
                        if (indx >= tokens.size) {
                            throw AssemblyException("Missing parameters for .ORG statement line $assemblyLine")
                        }
                        if (tokens[indx].type != AssemblerTokenTypes.NUMBER) {
                            throw AssemblyException("Invalid origin parameter at line $assemblyLine")
                        }
                        // get parameters for .ORG directive
                        val address = tokens[indx].num
                        tokens.removeAt(indx)
                        if ((address < currentBank.bankOrigin) or
                                (address >= currentBank.bankOrigin + currentBank.size))
                            throw AssemblyException("Invalid address for current bank specified at line $assemblyLine")
                        currentBank.curAddress = address
                    }

                // .WORD is like byte except results stored in two bytes as low then high
                    "WORD" -> {
                        var validEntry = true
                        while ((indx < tokens.size) and (validEntry)) {
                            // replace labels with varible token if they are variables
                            if (tokens[indx].type == AssemblerTokenTypes.LABEL_LINK) {
                                val varToken = variableList.get(tokens[indx].contents)
                                if (varToken != null)
                                    tokens[indx] = varToken
                            }
                            // determine token value (use low byte if label used)
                            val wordToAdd = if (tokens[indx].type == AssemblerTokenTypes.NUMBER) tokens[indx].num
                            else if (tokens[indx].type == AssemblerTokenTypes.LABEL_LINK) {
                                addLabel(AssemblerLabel(tokens[indx].contents, AssemblerLabelTypes.ADDRESS, currentBank.curAddress, currentBank.number))
                                0
                            } else {
                                validEntry = false
                                0
                            }
                            // if valid then write the value to memory (low then high bytes) and remove token
                            if (validEntry) {
                                currentBank.writeNextByte(wordToAdd and 255)
                                currentBank.writeNextByte((wordToAdd / 256) and 255)
                                tokens.removeAt(indx)
                            }
                        }
                    }

                    else -> {
                        println("WARNING: Unknown directive used $assemblyLine $directive")
                    }
                }
            } else
                ++indx
        }
    }

    // ***************************************
    // *** PARSING And Assembly generation ***
    // ***************************************
    /**
     * returns op code with that address mode, returning -1 for invalid requests
     */
    private fun getOpcodeWithAddressMode(opString:String, mode:AddressMode) : Int {
        if (!mapOfOpCodes.containsKey(opString))
            return -1
        val instList = mapOfOpCodes[opString]
        var opCode = -1
        for (inst in instList!!)
            if (inst.addressMode == mode)
                opCode = inst.OPCode
        return opCode
    }

    /**
     *
     */
    fun createAssemblyInstruction(opString:String, mode:AddressMode, target:Int):Array<Int> {
        val opCode = getOpcodeWithAddressMode(opString, mode)
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

        // if recording a macro then send lines to macro instead
        if (macroInProgress != null) {
            if (macroInProgress!!.addLine(tokens)) {
                macroList.put(macroInProgress!!.label, macroInProgress!!)
                macroInProgress = null
            }
            return arrayOf()
        }
        var indx = 0

        // see if line starts with a label - note label declarations take precedence so .byte,.word,.string, etc will
        // be mapped to the proper address.
        if (tokens[indx].type == AssemblerTokenTypes.LABEL_DECLARATION) {
            // add label system
            addLabel(AssemblerLabel(tokens[indx].contents, AssemblerLabelTypes.TARGET_VALUE, currentBank.curAddress, currentBank.number))
            ++indx
            if (indx >= tokens.size)
                return resultArray
        }

        // process the directives in this line. This will result in the token list being changed if directives exist
        processDirectives(tokens)
        if (indx >= tokens.size)
            return resultArray

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
                    val labelString = tokens[indx].contents
                    ++indx
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
                        val indexToken = tokens[indx]
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

                    if (resultArray.isEmpty())
                        throw AssemblyException("Provided instructions not valid machine language")
                }

                AssemblerTokenTypes.NUMBER -> {
                    val num = tokens[indx].num
                    ++indx
                    if (getOpcodeWithAddressMode(opString, AddressMode.RELATIVE) > 0) {
                        val target = num - (currentBank.curAddress + 2)
                        resultArray = createAssemblyInstruction(opString, AddressMode.RELATIVE, target)
                    } else if (indx >= tokens.size) {
                        if (num > 255)
                            resultArray = createAssemblyInstruction(opString, AddressMode.ABSOLUTE, num)
                        else
                            resultArray = createAssemblyInstruction(opString, AddressMode.ZERO_PAGE, num)
                    } else {
                        val indexToken = tokens[indx]
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
                        addr = tokens[indx].num
                    } else {
                        throw AssemblyException("Expected number or label but got ${tokens[indx].type}")
                    }
                    ++indx
                    if (indx >= tokens.size) throw AssemblyException("Unexpected end of statement")
                    if (tokens[indx].type == AssemblerTokenTypes.INDEX_X) {
                        resultArray = createAssemblyInstruction(opString, AddressMode.INDIRECT_X, addr)
                        // assume proper closing of statement
                        indx += 2
                    } else if (tokens[indx].type == AssemblerTokenTypes.INDIRECT_END) {
                        ++indx
                        if (indx >= tokens.size) {
                            resultArray = createAssemblyInstruction(opString, AddressMode.INDIRECT, addr)
                        } else if (tokens[indx].type == AssemblerTokenTypes.INDEX_Y) {
                            resultArray = createAssemblyInstruction(opString, AddressMode.INDIRECT_Y, addr)
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

        if (resultArray.isEmpty())
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
        assemblyLine = 0
        variableList.clear()
        labelList.clear()

        var errorCode = 0
        for (line in source) {
            ++assemblyLine
            verbose("$assemblyLine $line -> ${currentBank.curAddress.toString(16)}: ", false)
            val tokens = tokenize(line)
            try {
                val ml = parse(tokens)
                if (ml.isNotEmpty())
                    for (data in ml) {
                        currentBank.writeNextByte(data)
                        verbose("$${data.toString(16)},", false)
                    }
                verbose("")
            } catch (iae:AssemblyException) {
                errorCode = 2
                verbose("${iae.message}", true)
            }
        }
        val errors = linkLabelsInMemory()
        if (errors.size > 0) {
            errorCode = 1
            for (errmessage in errors)
                println(errmessage)
        }

        return errorCode
    }

}