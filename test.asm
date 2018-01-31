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
