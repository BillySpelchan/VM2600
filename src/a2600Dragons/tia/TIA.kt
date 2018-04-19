package a2600Dragons.tia

import kotlin.math.min
import kotlin.math.max

/*
enum class TIAPlayerSize(val size:Int) {
    ONE (0),
    TWO_CLOSE (1),
    TWO_MEDIUM (2),
    THREE_CLOSE (3),
    TWO_WIDE (4),
    DOUBLE_SIZE (5),
    THREE_MEDIUM (6),
    QUAD_PLAYER (7),
}
*/

@Suppress("unused")
object TIAPIARegs {
    const val VSYNC  = 0x00     // vertical sync - set to 2 to start sync and to 0 to stop
    const val VBLANK = 0x01     // vertical blank
    const val WSYNC  = 0x02     // wait for sync
    const val RSYNC  = 0x03     // Reset sync - for testing
    const val NUSIZ0 = 0x04     // number-size player-missile 0  -> xxMMxPPP =Missile size, Player size/copies)
    const val NUSIZ1 = 0x05     // number-size player-missile 1  -> xxMMxPPP =Missile size, Player size/copies)
    const val COLUP0 = 0x06     // color-luminosity player 0
    const val COLUP1 = 0x07     // color-luminosity player 2
    const val COLUPF = 0x08     // color-luminosity playfield
    const val COLUBK = 0x09     // color-luminosity background
    const val CTRLPF = 0x0A     // control playfield ball size & collisions
    const val REFP0  = 0x0B     // reflect player 0
    const val REFP1  = 0x0C     // reflect player 1
    const val PF0    = 0x0D     // Playfield register 0 =upper nibble display order 4..7)
    const val PF1    = 0x0E     // Playfield register 1 =display order 7..0)
    const val PF2    = 0x0F     // Playfield register 2 =display order 0..7)
    const val RESP0  = 0x10     // reset player 0
    const val RESP1  = 0x11     // reset player 1
    const val RESM0  = 0x12     // reset missile 0
    const val RESM1  = 0x13     // reset missile 1
    const val RESBL  = 0x14     // reset Ball
    const val AUDC0  = 0x15     // audio control 0
    const val AUDC1  = 0x16     // audio control 1
    const val AUDF0  = 0x17     // audio frequency 0
    const val AUDF1  = 0x18     // audio frequency 1
    const val AUDV0  = 0x19     // audio volume 0
    const val AUDV1  = 0x1A     // audio volume 1
    const val GRP0   = 0x1B     // graphics player 0 =bit pattern)
    const val GRP1   = 0x1C     // graphics player 1
    const val ENAM0  = 0x1D     // graphics =enable) missile 0
    const val ENAM1  = 0x1E     // graphics =enable) missile 1
    const val ENABL  = 0x1F     // graphics =enable) ball
    const val HMP0   = 0x20     // horizontal motion player 0
    const val HMP1   = 0x21     // horizontal motion player 1
    const val HMM0   = 0x22     // horizontal motion missile 0
    const val HMM1   = 0x23     // horizontal motion missile 1
    const val HMBL   = 0x24     // horizontal motion ball
    const val VDELP0 = 0x25     // vertical delay player 0
    const val VDELP1 = 0x26     // vertical delay player 1
    const val VDELBL = 0x27     // vertical delay ball
    const val RESMP0 = 0x28     // Reset missile 0 to player 0
    const val RESMP1 = 0x29     // Reset missile 1 to player 1
    const val HMOVE  = 0x2A     // Apply horizontal motion
    const val HMCLR  = 0x2B     // clear horizontal motion registers
    const val CXCLR  = 0x2C     // clear collision latches

    // Read registers
    const val CXM0P  = 0x30     // read collision =bit 7) MO with P1 =Bit 6) M0 with P0
    const val CXM1P  = 0x31     // read collision =bit 7) M1 with P0 =Bit 6) M1 with P1
    const val CXP0FB = 0x32     // read collision =bit 7) PO with PF =Bit 6) P0 with BL
    const val CXP1FB = 0x33     // read collision =bit 7) P1 with PF =Bit 6) P1 with BL
    const val CXM0FB = 0x34     // read collision =bit 7) MO with PF =Bit 6) M0 with BL
    const val CXM1FB = 0x35     // read collision =bit 7) M1 with PF =Bit 6) M1 with BL
    const val CXBLPF = 0x36     // read collision =bit 7) BL with PF =Bit 6) unused
    const val CXPPMM = 0x37     // read collision =bit 7) P0 with P1 =Bit 6) M0 with M1
    const val INPT0  = 0x38     // Dumped =Paddle) input port 0
    const val INPT1  = 0x39     // Dumped =Paddle) input port 1
    const val INPT2  = 0x3A     // Dumped =Paddle) input port 2
    const val INPT3  = 0x3B     // Dumped =Paddle) input port 3
    const val INPT4  = 0x3C     // Latched =Joystick) input port 4
    const val INPT5  = 0x3D     // Latched =Joystick) input port 5

    const val SWCHA  = 0x280
    const val SWACNT = 0x281
    const val SWCHB  = 0x282
    const val SWBCNT = 0x283
    const val INTIM  = 0x284
    const val TIMINT = 0x285

    const val TIM1T  = 0x294
    const val TIM8T  = 0x295
    const val TIM64T = 0x296
    const val T1024T = 0x297

    const val TIM1I  = 0x29c
    const val TIM8I  = 0x29d
    const val TIM64I = 0x29e
    const val T1024I = 0x29f

    // Player sprite scale/copy modes
    const val PMG_NORMAL = 0
    const val PMG_TWO_CLOSE = 1
    const val PMG_TWO_MEDIUM = 2
    const val PMG_THREE_CLOSE = 3
    const val PMG_TWO_WIDE = 4
    const val PMG_DOUBLE_SIZE = 5
    const val PMG_THREE_MEDIUM = 6
    const val PMG_QUAD_PLAYER = 7

    // Internal collision bits
    const val ICOL_PFBL = 1
    const val ICOL_PFP0 = 2
    const val ICOL_PFM0 = 8
    const val ICOL_PFP1 = 64
    const val ICOL_PFM1 = 1024
    const val ICOL_BLP0 = 4
    const val ICOL_BLM0 = 16
    const val ICOL_BLP1 = 128
    const val ICOL_BLM1 = 2048
    const val ICOL_P0M0 = 32
    const val ICOL_P0P1 = 256
    const val ICOL_P0M1 = 4096
    const val ICOL_M0P1 = 512
    const val ICOL_M0M1 = 8192
    const val ICOL_P1M1 = 16384

    // internal sprite indexes
    const val ISPRITE_BALL = 0
    const val ISPRITE_PLAYER1 = 1
    const val ISPRITE_MISSILE1 = 2
    const val ISPRITE_PLAYER0 = 3
    const val ISPRITE_MISSILE0 = 4

    /*
        Playfield	Ball	Player 0	Missile 0	Player 1
   Playfield	X	1	2	8	64	1024
   Ball	    1	X	4	16	128	2048
   Player0	    2	4	X	32	256	4096
   Missile0	8	16	32	X	512	8192
   Player1 	64	128	256	512	X	16384
   Missile1    1024	2048	4096	8192	16384	X

     */
}

class TIAColors() {
    val colorTable:Array<Int> = arrayOf(
// Hue 0
            0xFF000000.toInt(),0xFF000000.toInt(),0xFF404040.toInt(), 0xFF404040.toInt(),0xFF6c6c6c.toInt(), 0xFF6c6c6c.toInt(),0xFF909090.toInt(), 0xFF909090.toInt(),
            0xFFb0b0b0.toInt(),0xFFb0b0b0.toInt(),0xFFc8c8c8.toInt(),0xFFc8c8c8.toInt(),0xFFdcdcdc.toInt(),0xFFdcdcdc.toInt(),0xFFececec.toInt(),0xFFececec.toInt(),
// Hue 1
            0xFF444400.toInt(),0xFF444400.toInt(),0xFF646410.toInt(),0xFF646410.toInt(),0xFF848424.toInt(),0xFF848424.toInt(),0xFFa0a034.toInt(),0xFFa0a034.toInt(),
            0xFFb8b840.toInt(),0xFFb8b840.toInt(),0xFFd0d050.toInt(),0xFFd0d050.toInt(),0xFFe8e85c.toInt(),0xFFe8e85c.toInt(),0xFFfcfc68.toInt(),0xFFfcfc68.toInt(),
//Hue 2
            0xFF702800.toInt(),0xFF702800.toInt(),0xFF844414.toInt(),0xFF844414.toInt(),0xFF985c28.toInt(),0xFF985c28.toInt(),0xFFac783c.toInt(),0xFFac783c.toInt(),
            0xFFbc8c4c.toInt(),0xFFbc8c4c.toInt(),0xFFcca05c.toInt(),0xFFcca05c.toInt(),0xFFdcb468.toInt(),0xFFdcb468.toInt(),0xFFecc878.toInt(),0xFFecc878.toInt(),
// Hue 3
            0xFF841800.toInt(),0xFF841800.toInt(),0xFF983418.toInt(),0xFF983418.toInt(),0xFFac5030.toInt(),0xFFac5030.toInt(),0xFFc06848.toInt(),0xFFc06848.toInt(),
            0xFFd0805c.toInt(),0xFFd0805c.toInt(),0xFFe09470.toInt(),0xFFe09470.toInt(),0xFFeca880.toInt(),0xFFeca880.toInt(),0xFFfcbc94.toInt(),0xFFfcbc94.toInt(),
// Hue 4
            0xFF880000.toInt(),0xFF880000.toInt(),0xFF9c2020.toInt(),0xFF9c2020.toInt(),0xFFb03c3c.toInt(),0xFFb03c3c.toInt(),0xFFc05858.toInt(),0xFFc05858.toInt(),
            0xFFd07070.toInt(),0xFFd07070.toInt(),0xFFe08888.toInt(),0xFFe08888.toInt(),0xFFeca0a0.toInt(),0xFFeca0a0.toInt(),0xFFfcb4b4.toInt(),0xFFfcb4b4.toInt(),
// Hue 5
            0xFF78005c.toInt(),0xFF78005c.toInt(),0xFF8c2074.toInt(),0xFF8c2074.toInt(),0xFFa03c88.toInt(),0xFFa03c88.toInt(),0xFFb0589c.toInt(),0xFFb0589c.toInt(),
            0xFFc070b0.toInt(),0xFFc070b0.toInt(),0xFFd084c0.toInt(),0xFFd084c0.toInt(),0xFFdc9cd0.toInt(),0xFFdc9cd0.toInt(),0xFFecb0e0.toInt(),0xFFecb0e0.toInt(),
// Hue 6
            0xFF480078.toInt(),0xFF480078.toInt(),0xFF602090.toInt(),0xFF602090.toInt(),0xFF783ca4.toInt(),0xFF783ca4.toInt(),0xFF8c58b8.toInt(),0xFF8c58b8.toInt(),
            0xFFa070cc.toInt(),0xFFa070cc.toInt(),0xFFb484dc.toInt(),0xFFb484dc.toInt(),0xFFc49cec.toInt(),0xFFc49cec.toInt(),0xFFd4b0fc.toInt(),0xFFd4b0fc.toInt(),
// Hue 7
            0xFF140084.toInt(),0xFF140084.toInt(),0xFF302098.toInt(),0xFF302098.toInt(),0xFF4c3cac.toInt(),0xFF4c3cac.toInt(),0xFF6858c0.toInt(),0xFF6858c0.toInt(),
            0xFF7c70d0.toInt(),0xFF7c70d0.toInt(),0xFF9488e0.toInt(),0xFF9488e0.toInt(),0xFFa8a0ec.toInt(),0xFFa8a0ec.toInt(),0xFFbcb4fc.toInt(),0xFFbcb4fc.toInt(),
// Hue 8
            0xFF000088.toInt(),0xFF000088.toInt(),0xFF1c209c.toInt(),0xFF1c209c.toInt(),0xFF3840b0.toInt(),0xFF3840b0.toInt(),0xFF505cc0.toInt(),0xFF505cc0.toInt(),
            0xFF6874d0.toInt(),0xFF6874d0.toInt(),0xFF7c8ce0.toInt(),0xFF7c8ce0.toInt(),0xFF90a4ec.toInt(),0xFF90a4ec.toInt(),0xFFa4b8fc.toInt(),0xFFa4b8fc.toInt(),
// Hue 9
            0xFF00187c.toInt(),0xFF00187c.toInt(),0xFF1c3890.toInt(),0xFF1c3890.toInt(),0xFF3854a8.toInt(),0xFF3854a8.toInt(),0xFF5070bc.toInt(),0xFF5070bc.toInt(),
            0xFF6888cc.toInt(),0xFF6888cc.toInt(),0xFF7c9cdc.toInt(),0xFF7c9cdc.toInt(),0xFF90b4ec.toInt(),0xFF90b4ec.toInt(),0xFFa4c8fc.toInt(),0xFFa4c8fc.toInt(),
// Hue 10
            0xFF002c5c.toInt(),0xFF002c5c.toInt(),0xFF1c4c78.toInt(),0xFF1c4c78.toInt(),0xFF386890.toInt(),0xFF386890.toInt(),0xFF5084ac.toInt(),0xFF5084ac.toInt(),
            0xFF689cc0.toInt(),0xFF689cc0.toInt(),0xFF7cb4d4.toInt(),0xFF7cb4d4.toInt(),0xFF90cce8.toInt(),0xFF90cce8.toInt(),0xFFa4e0fc.toInt(),0xFFa4e0fc.toInt(),
// Hue 11
            0xFF003c2c.toInt(),0xFF003c2c.toInt(),0xFF1c5c48.toInt(),0xFF1c5c48.toInt(),0xFF387c64.toInt(),0xFF387c64.toInt(),0xFF509c80.toInt(),0xFF509c80.toInt(),
            0xFF68b494.toInt(),0xFF68b494.toInt(),0xFF7cd0ac.toInt(),0xFF7cd0ac.toInt(),0xFF90e4c0.toInt(),0xFF90e4c0.toInt(),0xFFa4fcd4.toInt(),0xFFa4fcd4.toInt(),
// Hue 12
            0xFF003c00.toInt(),0xFF003c00.toInt(),0xFF205c20.toInt(),0xFF205c20.toInt(),0xFF407c40.toInt(),0xFF407c40.toInt(),0xFF5c9c5c.toInt(),0xFF5c9c5c.toInt(),
            0xFF74b474.toInt(),0xFF74b474.toInt(),0xFF8cd08c.toInt(),0xFF8cd08c.toInt(),0xFFa4e4a4.toInt(),0xFFa4e4a4.toInt(),0xFFb8fcb8.toInt(),0xFFb8fcb8.toInt(),
// Hue 13
            0xFF143800.toInt(),0xFF143800.toInt(),0xFF345c1c.toInt(),0xFF345c1c.toInt(),0xFF507c38.toInt(),0xFF507c38.toInt(),0xFF6c9850.toInt(),0xFF6c9850.toInt(),
            0xFF84b468.toInt(),0xFF84b468.toInt(),0xFF9ccc7c.toInt(),0xFF9ccc7c.toInt(),0xFFb4e490.toInt(),0xFFb4e490.toInt(),0xFFc8fca4.toInt(),0xFFc8fca4.toInt(),
// Hue 14
            0xFF2c3000.toInt(),0xFF2c3000.toInt(),0xFF4c501c.toInt(),0xFF4c501c.toInt(),0xFF687034.toInt(),0xFF687034.toInt(),0xFF848c4c.toInt(),0xFF848c4c.toInt(),
            0xFF9ca864.toInt(),0xFF9ca864.toInt(),0xFFb4c078.toInt(),0xFFb4c078.toInt(),0xFFccd488.toInt(),0xFFccd488.toInt(),0xFFe0ec9c.toInt(),0xFFe0ec9c.toInt(),
// Hue 15
            0xFF442800.toInt(),0xFF442800.toInt(),0xFF644818.toInt(),0xFF644818.toInt(),0xFF846830.toInt(),0xFF846830.toInt(),0xFFa08444.toInt(),0xFFa08444.toInt(),
            0xFFb89c58.toInt(),0xFFb89c58.toInt(),0xFFd0b46c.toInt(),0xFFd0b46c.toInt(),0xFFe8cc7c.toInt(),0xFFe8cc7c.toInt(),0xFFfce08c.toInt(),0xFFfce08c.toInt()
    )

    fun getARGB(indx:Int):Int {
        if ((indx < 0) or (indx > 255))
            return 0
        else
            return colorTable[indx]
    }

    fun getHTMLColor(indx:Int):String {
        val noAlphaColor = getARGB(indx) and 0xFFFFFF
        return "#${noAlphaColor.toString(16)}"
    }
}

class PlayerMissileGraphic(var size:Int, var collisionMask:Int) {
    var scale = 1
    var copies = 1
    var distanceBetweenCopies = 0   // close = 2, med = 4 far = 8
    var x = 0
    var deltaX = 0
    var drawingBits = 0
    var delayedDraw = false
    var mirror = false

    fun isPixelDrawn(col:Int):Boolean {
        if (col < x) return false
        if (delayedDraw) return false

        var pixelAtCol = false
        for (copyCount in 1..copies) {
            val copyStart = x + (copyCount - 1) * size * scale * distanceBetweenCopies
            val copyEnd = copyStart + size * scale
            if ((col >= copyStart) and (col < copyEnd)) {
                val bitPos = (col - copyStart) / scale
                val bit = if (mirror) 1 shl bitPos else (1 shl (size-1)) ushr bitPos
                if ((bit and drawingBits) > 0) pixelAtCol = true
            }
        }

        return pixelAtCol
    }

    fun hmove() {
        x = min(159, max(0, x + deltaX))
    }

    fun setPlayerScaleCopy(scmode:Int) {
        when (scmode) {
            TIAPIARegs.PMG_NORMAL -> {scale = 1; copies = 1; distanceBetweenCopies = 1}
            TIAPIARegs.PMG_TWO_CLOSE  -> {scale = 1; copies = 2; distanceBetweenCopies = 2}
            TIAPIARegs.PMG_TWO_MEDIUM  -> {scale = 1; copies = 2; distanceBetweenCopies = 4}
            TIAPIARegs.PMG_THREE_CLOSE -> {scale = 1; copies = 3; distanceBetweenCopies = 2}
            TIAPIARegs.PMG_TWO_WIDE  -> {scale = 1; copies = 2; distanceBetweenCopies = 8}
            TIAPIARegs.PMG_DOUBLE_SIZE  -> {scale = 2; copies = 1; distanceBetweenCopies = 1}
            TIAPIARegs.PMG_THREE_MEDIUM  -> {scale = 1; copies = 3; distanceBetweenCopies = 4}
            TIAPIARegs.PMG_QUAD_PLAYER  -> {scale = 4; copies = 1; distanceBetweenCopies = 1}
        }

    }
}

@Suppress("MemberVisibilityCanPrivate", "CanBeVal")
class TIA ( ) {
    var colorClock:Int = 0

    // colors
    var backgroundColor:Int = 0
    var playfieldBallColor:Int = 0
    var playerMissile0Color:Int = 0
    var playerMissile1Color:Int = 0

    // playfield
    var playfieldBits:Int = 0
    var mirror = false
    var scoreMode = false
    var priorityPlayfield = false

    // sprites (Player Missile Graphics in Atari parlance)
    val sprites = arrayOf(PlayerMissileGraphic(1, 30570), // Ball
            PlayerMissileGraphic(8, 15423), // Player 1
            PlayerMissileGraphic(1, 1023), // Missile 1
            PlayerMissileGraphic(8, 28377), // Player 0
            PlayerMissileGraphic(1, 24007) ) // Missile 0
    var collisonState = 0


    // raster data
    var rasterLine:Array<Int> = Array<Int>(160, {_->0})

    // *****************
    // ***** METHODS ***
    // *****************

    /**
     * Inbternal method to reverse the bits in a byte
     */
    fun reversePFBits(value:Int):Int {
        var reversed = 0
        var testBit = 1
        for (cntr in 0..7) {
            reversed *= 2
            reversed += if ((testBit and value) > 0) 1 else 0
            testBit *= 2
        }
        return reversed
    }

    private fun convertNibbleToSignedInt(nibble:Int):Int {
        val nib = nibble and 15
        return if (nib > 7) nib - 16 else nib
    }

    /**
     * Handle a write to one of the TIA registers by doing the appropriate internal work.
     */
    fun writeRegister(address:Int, value:Int) {
        val column = colorClock - 68
        when (address) {
            // color control registers
            TIAPIARegs.COLUBK -> backgroundColor = value
            TIAPIARegs.COLUPF -> playfieldBallColor = value
            TIAPIARegs.COLUP0 -> playerMissile0Color = value
            TIAPIARegs.COLUP1 -> playerMissile1Color = value
            // Playfield registers
            TIAPIARegs.PF0 -> {
                var bits = reversePFBits(value)
                playfieldBits = (playfieldBits and 0xFFFF) or (bits shl(16))
            }
            TIAPIARegs.PF1 -> {
                var bits = value
                playfieldBits = (playfieldBits and 0xF00FF) or (bits shl(8))
            }
            TIAPIARegs.PF2 -> {
                var bits = reversePFBits(value)
                playfieldBits = (playfieldBits and 0xFFF00) or bits
            }
            TIAPIARegs.CTRLPF -> {
                mirror = (value and 1) == 1             // bit 0 handles mirror mode
                scoreMode = (value and 2) == 2          // bit 1 handles score mode
                priorityPlayfield = (value and 4) == 4  // bit 2 Places playfield above sprites

                val ballSize = (value shr 4) and 3      // bits 4 and 5 are ball size (scale)
                sprites[TIAPIARegs.ISPRITE_BALL].scale = 1 shl ballSize
            }
            // Player Missile Graphics (PMG) drawing enable/data
            TIAPIARegs.ENABL -> sprites[TIAPIARegs.ISPRITE_BALL].drawingBits = if ((value and 2) == 2) 1 else 0
            TIAPIARegs.ENAM0 -> sprites[TIAPIARegs.ISPRITE_MISSILE0].drawingBits = if ((value and 2) == 2) 1 else 0
            TIAPIARegs.ENAM1 -> sprites[TIAPIARegs.ISPRITE_MISSILE1].drawingBits = if ((value and 2) == 2) 1 else 0
            TIAPIARegs.GRP0 -> sprites[TIAPIARegs.ISPRITE_PLAYER0].drawingBits = value and 255
            TIAPIARegs.GRP1 -> sprites[TIAPIARegs.ISPRITE_PLAYER1].drawingBits = value and 255
            // PMG sprite positioning registers
            TIAPIARegs.RESBL -> sprites[TIAPIARegs.ISPRITE_BALL].x = column
            TIAPIARegs.RESP0 -> sprites[TIAPIARegs.ISPRITE_PLAYER0].x = column
            TIAPIARegs.RESP1 -> sprites[TIAPIARegs.ISPRITE_PLAYER1].x = column
            TIAPIARegs.RESM0 -> sprites[TIAPIARegs.ISPRITE_MISSILE0].x = column
            TIAPIARegs.RESM1 -> sprites[TIAPIARegs.ISPRITE_MISSILE1].x = column
            TIAPIARegs.RESMP0 -> sprites[TIAPIARegs.ISPRITE_MISSILE0].x = sprites[TIAPIARegs.ISPRITE_PLAYER0].x
            TIAPIARegs.RESMP1 -> sprites[TIAPIARegs.ISPRITE_MISSILE1].x = sprites[TIAPIARegs.ISPRITE_PLAYER1].x
            // PMG sprite scaling and copies
            TIAPIARegs.NUSIZ0 -> {
                sprites[TIAPIARegs.ISPRITE_PLAYER0].setPlayerScaleCopy(value and 7)
                sprites[TIAPIARegs.ISPRITE_MISSILE0].scale = 1 shl ((value shr 4) and 3)
            }
            TIAPIARegs.NUSIZ1 -> {
                sprites[TIAPIARegs.ISPRITE_PLAYER1].setPlayerScaleCopy(value and 7)
                sprites[TIAPIARegs.ISPRITE_MISSILE1].scale = 1 shl ((value shr 4) and 3)
            }
            // PMG Mirroring
            TIAPIARegs.REFP0 -> sprites[TIAPIARegs.ISPRITE_PLAYER0].mirror = (value and 8) == 8
            TIAPIARegs.REFP1 -> sprites[TIAPIARegs.ISPRITE_PLAYER1].mirror = (value and 8) == 8
            // PMG Horizontal movement
            TIAPIARegs.HMP0 -> sprites[TIAPIARegs.ISPRITE_PLAYER0].deltaX = convertNibbleToSignedInt(value)
            TIAPIARegs.HMP1 -> sprites[TIAPIARegs.ISPRITE_PLAYER1].deltaX = convertNibbleToSignedInt(value)
            TIAPIARegs.HMM0 -> sprites[TIAPIARegs.ISPRITE_MISSILE0].deltaX = convertNibbleToSignedInt(value)
            TIAPIARegs.HMM1 -> sprites[TIAPIARegs.ISPRITE_MISSILE1].deltaX = convertNibbleToSignedInt(value)
            TIAPIARegs.HMBL -> sprites[TIAPIARegs.ISPRITE_BALL].deltaX = convertNibbleToSignedInt(value)
            TIAPIARegs.HMOVE -> {
                for (cntr in 0..4)
                    sprites[cntr].hmove()
            }
            TIAPIARegs.HMCLR -> {
                for (cntr in 0..4)
                    sprites[cntr].deltaX = 0
            }
            // collision handling clear
            TIAPIARegs.CXCLR -> collisonState = 0

            // Unknown or unimplemented registers print warning
            else -> println("TIA register $address not implemented!")
        }
    }

    private fun buildCollisionByte(bit7mask:Int, bit6mask:Int):Int {
        val bit7 = if ((collisonState and bit7mask) == bit7mask) 128 else 0
        val bit6 = if ((collisonState and bit6mask) == bit6mask) 64 else 0
        return bit7 or bit6
    }

    fun readRegister(address:Int):Int {
        return when(address) {
            TIAPIARegs.CXBLPF -> buildCollisionByte(TIAPIARegs.ICOL_PFBL,0)
            TIAPIARegs.CXM0FB -> buildCollisionByte(TIAPIARegs.ICOL_PFM0, TIAPIARegs.ICOL_BLM0)
            TIAPIARegs.CXM1FB -> buildCollisionByte(TIAPIARegs.ICOL_PFM1, TIAPIARegs.ICOL_BLM1)
            TIAPIARegs.CXM0P -> buildCollisionByte(TIAPIARegs.ICOL_P0M0, TIAPIARegs.ICOL_M0P1)
            TIAPIARegs.CXM1P -> buildCollisionByte(TIAPIARegs.ICOL_P0M1, TIAPIARegs.ICOL_P1M1)
            TIAPIARegs.CXP0FB -> buildCollisionByte(TIAPIARegs.ICOL_PFP0, TIAPIARegs.ICOL_BLP0)
            TIAPIARegs.CXP1FB -> buildCollisionByte(TIAPIARegs.ICOL_PFP1, TIAPIARegs.ICOL_BLP1)
            TIAPIARegs.CXPPMM -> buildCollisionByte(TIAPIARegs.ICOL_P0P1, TIAPIARegs.ICOL_M0M1)

        // Unknown or unimplemented registers print warning
            else -> { println("TIA register $address not implemented!"); 0}
        }
    }


    // return true when scanline complete, false otherwise
    fun nextClockTick():Boolean {
        // run current pixel
        val column = colorClock - 68
        if (column >= 0) {
            var pixelColor = backgroundColor
            var collisionMask = 32767

            // render playfield
            val pfCol = if (column < 80) column / 4 else
                if (mirror) (159 - column) / 4 else (column - 80) / 4
            var pfPixelMask = 0x80000 shr pfCol
            val shouldDrawPlayfield =(playfieldBits and pfPixelMask) > 0
            val pfColor = if (scoreMode) {if (column < 80) playerMissile0Color else playerMissile1Color } else playfieldBallColor
            if ( ! shouldDrawPlayfield) collisionMask = collisionMask and 31668
            if ((shouldDrawPlayfield) and ( ! priorityPlayfield )) pixelColor = pfColor

            pixelColor = if (sprites[0].isPixelDrawn(column)) playfieldBallColor else {
                collisionMask = collisionMask and sprites[0].collisionMask
                pixelColor}
            pixelColor = if (sprites[1].isPixelDrawn(column)) playerMissile1Color else {
                collisionMask = collisionMask and sprites[1].collisionMask
                pixelColor}
            pixelColor = if (sprites[2].isPixelDrawn(column)) playerMissile1Color else {
                collisionMask = collisionMask and sprites[2].collisionMask
                pixelColor}
            pixelColor = if (sprites[3].isPixelDrawn(column)) playerMissile0Color else {
                collisionMask = collisionMask and sprites[3].collisionMask
                pixelColor}
            pixelColor = if (sprites[4].isPixelDrawn(column)) playerMissile0Color else {
                collisionMask = collisionMask and sprites[4].collisionMask
                pixelColor}

            if ((shouldDrawPlayfield) and (priorityPlayfield)) pixelColor = pfColor

            collisonState = collisonState or collisionMask

            rasterLine[column] = pixelColor
        }
        ++colorClock
         return if (colorClock >= 228) {
             colorClock = 0
             true
         } else false
    }

    fun renderScanline() {
        while (!nextClockTick()) /* run clock until scanline finished */;
    }

    fun runToClock(targetColorClock:Int) {
        assert((targetColorClock >= 0) and (targetColorClock < 228))
        while (colorClock != targetColorClock)
            nextClockTick()
    }
}