package a2600Dragons.tia

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

enum class TIARegisters(val address:Int) {
    VSYNC  ( 0x00 ),    // vertical sync - set to 2 to start sync and to 0 to stop
    VBLANK ( 0x01 ),    // vertical blank
    WSYNC  ( 0x02 ),    // wait for sync
    RSYNC  ( 0x03 ),    // Reset sync - for testing
    NUSIZ0 ( 0x04 ),    // number-size player-missile 0  -> xxMMxPPP (Missile size, Player size/copies)
    NUSIZ1 ( 0x05 ),    // number-size player-missile 1  -> xxMMxPPP (Missile size, Player size/copies)
    COLUP0 ( 0x06 ),    // color-luminosity player 0
    COLUP1 ( 0x07 ),    // color-luminosity player 2
    COLUPF ( 0x08 ),    // color-luminosity playfield
    COLUBK ( 0x09 ),    // color-luminosity background
    CTRLPF ( 0x0A ),    // control playfield ball size & collisions
    REFP0  ( 0x0B ),    // reflect player 0
    REFP1  ( 0x0C ),    // reflect player 1
    PF0    ( 0x0D ),    // Playfield register 0 (upper nibble display order 4..7)
    PF1    ( 0x0E ),    // Playfield register 1 (display order 7..0)
    PF2    ( 0x0F ),    // Playfield register 2 (display order 0..7)
    RESP0  ( 0x10 ),    // reset player 0
    RESP1  ( 0x11 ),    // reset player 1
    RESM0  ( 0x12 ),    // reset missile 0
    RESM1  ( 0x13 ),    // reset missile 1
    RESBL  ( 0x14 ),    // reset Ball
    AUDC0  ( 0x15 ),    // audio control 0
    AUDC1  ( 0x16 ),    // audio control 1
    AUDF0  ( 0x17 ),    // audio frequency 0
    AUDF1  ( 0x18 ),    // audio frequency 1
    AUDV0  ( 0x19 ),    // audio volume 0
    AUDV1  ( 0x1A ),    // audio volume 1
    GRP0   ( 0x1B ),    // graphics player 0 (bit pattern)
    GRP1   ( 0x1C ),    // graphics player 1
    ENAM0  ( 0x1D ),    // graphics (enable) missile 0
    ENAM1  ( 0x1E ),    // graphics (enable) missile 1
    ENABL  ( 0x1F ),    // graphics (enable) ball
    HMP0   ( 0x20 ),    // horizontal motion player 0
    HMP1   ( 0x21 ),    // horizontal motion player 1
    HMM0   ( 0x22 ),    // horizontal motion missile 0
    HMM1   ( 0x23 ),    // horizontal motion missile 1
    HMBL   ( 0x24 ),    // horizontal motion ball
    VDELP0 ( 0x25 ),    // vertical delay player 0
    VDELP1 ( 0x26 ),    // vertical delay player 1
    VDELBL ( 0x27 ),    // vertical delay ball
    RESMP0 ( 0x28 ),    // Reset missile 0 to player 0
    RESMP1 ( 0x29 ),    // Reset missile 1 to player 1
    HMOVE  ( 0x2A ),    // Apply horizontal motion
    HMCLR  ( 0x2B ),    // clear horizontal motion registers
    CXCLR  ( 0x2C ),    // clear collision latches

    // Read registers
    CXM0P  ( 0x30 ),    // read collision (bit 7) MO with P1 (Bit 6) M0 with P0
    CXM1P  ( 0x31 ),    // read collision (bit 7) M1 with P0 (Bit 6) M1 with P1
    CXP0FB ( 0x32 ),    // read collision (bit 7) PO with PF (Bit 6) P0 with BL
    CXP1FB ( 0x33 ),    // read collision (bit 7) P1 with PF (Bit 6) P1 with BL
    CXM0FB ( 0x34 ),    // read collision (bit 7) MO with PF (Bit 6) M0 with BL
    CXM1FB ( 0x35 ),    // read collision (bit 7) M1 with PF (Bit 6) M1 with BL
    CXBLPF ( 0x36 ),    // read collision (bit 7) BL with PF (Bit 6) unused
    CXPPMM ( 0x37 ),    // read collision (bit 7) P0 with P1 (Bit 6) M0 with M1
    INPT0  ( 0x38 ),    // Dumped (Paddle) input port 0
    INPT1  ( 0x39 ),    // Dumped (Paddle) input port 1
    INPT2  ( 0x3A ),    // Dumped (Paddle) input port 2
    INPT3  ( 0x3B ),    // Dumped (Paddle) input port 3
    INPT4  ( 0x3C ),    // Latched (Joystick) input port 4
    INPT5  ( 0x3D ),    // Latched (Joystick) input port 5

    SWCHA  ( 0x280 ),
    SWACNT ( 0x281 ),
    SWCHB  ( 0x282 ),
    SWBCNT ( 0x283 ),
    INTIM  ( 0x284 ),
    TIMINT ( 0x285 ),

    TIM1T  ( 0x294 ),
    TIM8T  ( 0x295 ),
    TIM64T ( 0x296 ),
    T1024T ( 0x297 ),

    TIM1I  ( 0x29c ),
    TIM8I  ( 0x29d ),
    TIM64I ( 0x29e ),
    T1024I ( 0x29f )
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

@Suppress("MemberVisibilityCanPrivate", "CanBeVal")
class TIA ( ) {
    var colorClock:Int = 0
    var backgroundColor:Int = 0
    var rasterLine:Array<Int> = Array<Int>(160, {_->0})

    fun writeRegister(address:Int, value:Int) {
        when (address) {
            TIARegisters.COLUBK.address -> backgroundColor = value
            else -> println("TIA register $address not implemented!")
        }
    }

/*
    fun readRegister(address:Int) {

    }
*/

    // return true when scanline complete, false otherwise
    fun nextClockTick():Boolean {
        // run current pixel
        val column = colorClock - 68
        if (column >= 0) {
            var pixelColor = backgroundColor

            // TODO render playfield
            // TODO render player-missile graphics and set collisions

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
}