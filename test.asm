; Bank directive test
.BANK 0 $1000 1024
	JSR bank1
	JSR bank2
	BRK
.bank 1 1024 $2000
bank1:	LDA #1
	RTS
.BANK 2 1024 $2000
bank2:
	LDA #2
	RTS

	
; org test
.BANK 0 $1000
	JMP ten
.ORG 4106
ten: LDA #10
	BRK
	
; equ directive tests
.EQU ONE 1
.EQU TWO $2
.EQU THREE %11
	LDA #ONE
	LDA #TWO
	STA THREE
	BRK
					
					
; .high .low directive test
.BANK 0 $1000 1024
.EQU hilow $1234
start: LDA #.HIGH hilow
	LDA #.LOW highlow
	LDA #.HIGH start
	LDA #.LOW start
	BRK
	

; .byte .word directive tests
.BANK 0 $1000 1024
.EQU two 2
.EQU fivesix $0605
	LDA bytes	; make sure labels to .byte data works
	LDA words	; make sure labels to .word data works
bytes: .BYTE 1 two 3 4
words: .WORD fivesix $0807 $0A09
	BRK	; make sure can continue assembly generation after data
 

; Macro Test code
.MSTART DELAY 255
	LDX #P0
delayLoop:
	DEX
	BNE delayLoop
.MEND

.MSTART LONGDELAY 2 255
	LDY #P0
longDelayLoop:
	.MACRO DELAY P1
	DEY
	BNE longDelayLoop
.MEND

.MACRO DELAY 1
.MACRO DELAY 3
.MACRO LONGDELAY 10 100



; *** flag tests
; SEC test makes sure Carry is set (1)
	CLC
	SEC
	BRK
; Expect C = 1


; CLC test makes sure Carry is clear (0)
	SEC
	CLC
	BRK
; Expect C = 0


; SED test makes sure Decimal mode is set (1)
	CLD
	SED
	BRK
; Expect D = 1

; CLD test makes sure Decimal mode is clear (0)
	SED
	CLD
	BRK
; Expect D = 0


; CLI test
	SEI
	CLI
	BRK
; Expect I = 0

; SEI should clear then interupts
	CLI
	SEI
	BRK
; Expect I = 1

; CLO test - write better test when have ADC implemented!
	CLV
	BRK
; Expect V = 0


; ****************************
; *** LOAD REGISTERS TESTS ***
; ****************************

; LDA Immediate - zero test
	LDA #0
	BRK
	; expect a=0; z=1; n=0

; LDX Immediate - negative test
	LDX #$FF
	BRK
	; expect x=255; z=0; n=1

; LDY Immediate - positive test
	LDY #127
	BRK
	; expect x=255; z=0; n=0
	

; LDA Zero Page   
	LDA 5
	BRK
.ORG 5
.BYTE 42
	; expect a=42, z=0, n=0

; LDX Zero Page 0
	LDX 200
	BRK
.ORG 200
.BYTE 0
	; expect x=0, z=1, n=0

; LDY Zero Page negative
	LDY 100
	BRK
.ORG 100
.BYTE 128
	; expect y=128, z=0, n=1


; LDA Zero Page, x
	LDX #10
	LDA 5,X
	BRK
.ORG 15
.BYTE 99
	; expect a=99
	
; LDY Zero Page, x
	LDX #5
	LDY 5,X
	BRK
.ORG 10
.BYTE 12
	; expect y=12

; LDX Zero Page, y
	LDY #15
	LDX 10,X
	BRK
.ORG 25
.BYTE 52
	; expect x=52
	
; LDA, LDX, LDY Absolute
	LDA 1024
	LDX 1025
	LDY 1026
	BRK
.ORG 1024
.BYTE 1 2 3
	; expect a = 1, x=2, y=3
	

 
// Absolute,X  BD *
//  Absolute,Y B9 *
//  (Indirect,X) A1
//  (Indirect),Y B1 *
 