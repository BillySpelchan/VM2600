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
	
; LDA and LDY Absolute,X 
	LDX #10
	LDA 512,X
	LDY 513,X
	BRK
.ORG 522
.BYTE 1 2
	; expect a = 1, y=2


; LDA and LDX Absolute,Y
	LDY #10
	LDA 512,Y
	LDX 513,Y
	BRK
.ORG 522
.BYTE 1 2
	; expect a = 1, x=2


; LDA (Indirect,X)
	LDX #200
	LDA (54,X)
	BRK
.ORG 254
.WORD 1024
.ORG 1024
.BYTE 88
	; expect A=88
	
	
; LDA (Indirect),Y 
	LDY #10
	LDA (254),Y
	BRK
.ORG 254
.WORD 1024
.ORG 1034
.BYTE 88
	; expect A=88

	
; *******************************
; *** STORING REGISTERS TESTS ***
; *******************************

; STA tests (all seven modes)
	LDA #123
	LDX #$10
	LDY #5
	STA $AB	; zero page
	STA $AB,X
	STA $250
	STA $250,X
	STA $250,Y
	STA ($50,X)
	STA ($60),Y
	BRK
.ORG $60
.WORD $600
;MAB, MBB, M250, M260, M255, M600, M605 = 123

; STX Tests (all three)
	LDX #22
	LDY #5
	STX $50 	;  Zero Page
	STX $50,Y	;  Zero Page,Y
	STX $250	;  Absolute
	BRK
;M50, M55, M250 = 22

; STY Tests (all three)
	LDX #5
	LDY #33
	STY $50 	;  Zero Page
	STY $50,X	;  Zero Page,X
	STY $250	;  Absolute
	BRK
;M50, M55, M250 = 33


; ********************************************
; *** TRANSFERRING BETWEEN REGISTERS TESTS ***
; ********************************************

; TXA TAY test
	LDX #179
	TXA
	TAY
	BRK
; Acc, Y=79, N=1, Z=0

; TYA TAX test
	LDY #24
	TYA
	TAX
	BRK
; Acc, X=24, N=0, Z=0

; Transfer zero test
	LDA #0
	LDX #23
	TAX
	BRK
; Acc, X=0, N=0, Z=1
	

; ********************
; *** STACK TEESTS ***
; ********************
	
; TSX and TXS
	LDX #128
	TXS
	LDX #0
	TSX
	BRK
; X, SP = 128

; Stack Pushing test
	LDX #255
	TXS
	LDA #11
	SED
	LDY #0	; set zero flag so flag register should now be 42
	PHP
	PHA
; sp = 253, M1FE=11 M1FF=42	

; Stack popping test
	PLA
	PLP
	BRK
.ORG $1FE
.BYTE 11 42
; sp = 253, M1FE=11 M1FF=42	


; *************************************
; *** INCREMENT and DECREMENT TESTS ***
; *************************************

; INC, INX, INY tests
	LDX #$10
	LDY #255
	INC $50
	INC $41,X
	INC $110
	INC $101,X
	INX
	INY
	BRK
.ORG $50
.BYTE 1 2
.ORG $110
.BYTE 3 4
; expect M50=2, M51=3, M110=4, M111=5, X=17, Y=0, Z=1

; DEC, DEX, DEY tests
	LDX #$10
	LDY #0
	DEC $50
	DEC $41,X
	DEC $110
	DEC $101,X
	DEX
	DEY
	BRK
.ORG $50
.BYTE 1 2
.ORG $110
.BYTE 3 4
; expect M50=0, M51=1, M110=2, M111=3, X=15, Y=255, N=1


; ************************************
; *** BASIC BRANCHING INSTRUCTIONS ***
; ************************************

; BEQ test 
LDY #1
LDX #1
BEQ done
INY		; Make y 2 if reached which should be
DEX
BEQ done
DEY		; Make y 1 (or 0) if reached which fails test!
done: BRK
; expect y = 2


; BNE test 5 + 5 using iny and looping
	LDX #5
	LDY #5
add: INY
	DEX
	BNE add
	BRK
	; expect y=10

; BMI -> ABS(-16) the hard way
	LDX #$F0	; two's complement value for -16
	LDY #0
add: INY
	INX
	BMI add		; repeat while X is negative
	BRK
	; expect y=16

	
; BPL - Count to 28 the hard way
	LDX #100
	LDY #0
count: INY
	INX
	BPL count	; will count from 100 to 127, quit when 128 hit
	BRK
	; exect y=28
	
; ***************************
; *** ADDING INSTRUCTIONS ***
; ***************************

; #immediate with overflow checked
	CLD
	LDA #64
	CLC
	ADC #64
	BRK
	;expect A=128, V=1, N=1 C=0, Z=0

	
; zero page using BCD
	LDA #$10
	SED
	CLC
	ADC 25
	BRK
.ORG 25
.BYTE $15
	;expect A=$25, N=0 C=0, Z=0	
	
	
; Zero page,x with BCD and carry no carry result
	LDA #$10
	LDX #0
	SED
	SEC			; force carry
	ADC 25,X
	BRK
.ORG 25
.BYTE $15
	;expect A=$26, N=0 C=0, Z=0	

	
; absolute with carry resulting in carry result and zero
	CLD
	LDA #128
	SEC
	ADC 512
	BRK
.ORG 512	
.BYTE 128
	;expect A=0 V=1, C=1, Z=1, N=0

	
; absolute,x overflow resulting in zero
	CLD
	LDX #2
	LDA #128
	CLC
	ADC 512,X
	BRK
.ORG 512	
.BYTE 0 0 128
	; A=0, Z=1, C=1

; absolute,y normal add
	CLD
	LDY #0
	LDA #50
	CLC
	ADC 512,Y
	BRK
.ORG 512	
.BYTE 25
	; A = 75, V=0, C=0

	
; (indirect, x) negative but normal add
	CLD
	LDX #1
	LDA #255
	CLC
	ADC (25,X)
	BRK
.ORG 26
.WORD 512
.ORG 512
.BYTE 254
	; A=253 V=0 C=1

	
; (indirect),y negative + positive (carry but no overflow)
	CLD
	LDY #1
	LDA #255
	CLC
	ADC (25),1
	BRK
.ORG 25
.WORD 512
.ORG 512
.BYTE 0 127
	; a=126 v=0 c=1

	
; ********************************
; *** SUBTRACTION INSTRUCTIONS ***
; ********************************

; SBC immediate with overflow
	CLD
	LDA #64
	SEC
	SBC #191
	BRK
	; Expect A=129, V=1, N=1, C=0, Z=0

	
; SBC zero page using BCD
	SED
	LDA #$15
	SEC
	SBC 25
	BRK
.ORG 25
.BYTE $10
	; expect A=$05, N=0, C=1, Z=0
	

;SVC Zero page,x with BCD and underflow
	SED
	LDA #$10
	LDX #0
	SEC
	SBC 25,X
	BRK
.ORG 25
.BYTE $95
	; expect A=$15, N=0, C=0, Z=0

	
; sbc bsolute with no carry res zero
	CLD
	LDA #64
	CLC
	SBC 512
	BRK
.ORG 512
.BYTE 63
	; expect A=0, V=0, N=0, C=1,Z=1
	
	
; absolute,x overflow (-128-1 = 127?)
	CLD
	LDX #2
	LDA #128
	SEC
	SBC 512,X
	BRK
.ORG 512
.BYTE 0 0 1
	; A=127, V= 1, N= 0, C=1, Z=0


; sbc absolute,y normal
	CLD
	LDY #0
	LDA #50
	SEC
	SBC 512,Y
	BRK
.ORG 512
.BYTE 25
	; A=25, V=0, N=0, C=1, Z= 0
	

; sbc (indirect, x) negative but normal
	CLD
	LDX #1
	LDA #254
	SEC
	SBC (25,X)
	BRK
.ORG 26
.WORD 512
.ORG 512
.BYTE 255
	; A=255, V=0, N=1, C=0, Z=0
	

; sbc (indirect),y negative - positive
    CLD
	LDY #1
	LDA #255
	SEC
	SBC (25),Y
	BRK
.ORG 25
.WORD 512
.ORG 512
.BYTE 0 2
    ; A=253, V=0, N=1, C=1, Z=0
	

; *******************************************	
; *** Math Branching Instructions testing ***
; *******************************************

; BCC test by counting times can add 10 before wrapping
	CLD
	LDA #0
	TAX
count: INX
	CLC
	ADC #10
	BCC count
	BRK
	; expect A = 6, X=26, C=1
	
	
; BCS count how many times can subtract 10 before wrapping
	CLD
	LDA #255
	LDX #0
count:	INX
	SEC
	SBC #10
	BCS count
	BRK;
	; expect A 251, X26, C=0
	
	
; BVC count how many times can add 10 until become negative
	CLD
	LDA #0
	TAX
count:	INX
	CLC
	ADC #10
	BVC count
	BRK;
	; expect A 130, X13, C=0, V=1
	
; BVS test
	CLD
	LDA #64
	LDX #0
	CLC
	ADC #32
	BVS done
	INX
	CLC
	ADC #32
	BVS done
	INX
done: BRK
	; expect A=128, X=1, V=1

	
; ************************************	
; *** COMPARE Instructions testing ***
; ************************************
	
; CMP Immediate 
	CLD
	LDA #0
	TAX
loop:	INX
	CLC
	ADC #2
	CMP #20
	BNE loop
	BRK
	; expect x=10,a=20,z=1
	
	
; CMP Zero Page
	CLD
	LDA #0
	TAX
loop:	INX
	CLC
	ADC #2
	CMP 200
	BMI loop
	BRK
.ORG 200
.BYTE 20
	; expect x=10,a=20,z=1

	
; CMP Zero Page,X
	CLD
	LDA #0
	TAY
	LDX #10
loop:	INY
	CLC
	ADC #2
	CMP 190,X
	BCC loop
	BRK
.ORG 200
.BYTE 20
	; expect x=10,a=20,z=1


; CMP Absolute
	CLD
	LDA #0
	TAX
loop:	INX
	CLC
	ADC #2
	CMP 512
	BMI loop
	BRK
.ORG 512
.BYTE 20
	; expect x=10,a=20,z=1

	
; CMP Absolute,X find 4 index
	LDA #4
	LDX #255
loop: INX
	CMP 512,X
	BNE loop
	BRK
.ORG 512
.BYTE 1 2 3 4 
	; expect A=4, X=3, Z=1, N=0, C=1


; CMP Absolute,Y
	LDA #5
	LDY #255
loop: INY
	CMP 512,Y
	BNE loop
	BRK
.ORG 512
.BYTE 1 2 3 4 5 
	; expect A=5, X=4, Z=1, N=0, C=1


; CMP (Indirect,X)
	LDA #69
	LDX #2
	LDY #0
	CMP (200,X)
	BEQ done
	LDY 512
done: BRK	
.ORG 200
.WORD 0 512
.ORG 512
.BYTE 42
	; Expect A=69,Y=42
	
; CMP (Indirect),Y
	LDA #3
	LDY #255
loop: INY
	CMP (200),Y
	BNE loop
	BRK
.ORG 200
.WORD 512
.ORG 512
.BYTE 1 2 3 4 5 
	; expect A=3, X=2, Z=1, N=0, C=1

	
; CPX Immediate - compute 3x20 the hard way
	CLD
	LDA #0
	TAX
loop:	INX
	CLC
	ADC #3
	CPX #20
	BNE loop
	BRK
	; expect x=20,a=60,z=1
	
	
; CPX Zero Page - compute 5x5 the hard way
	CLD
	LDA #0
	TAX
loop:	INX
	CLC
	ADC #5
	CPX 200
	BCC loop
	BRK
.ORG 200
.BYTE 5
	; expect x=5,a=25,c=1

	
; CPX Absolute - Compute 6x7 the hard way
	CLD
	LDA #0
	TAX
loop:	INX
	CLC
	ADC #7
	CPX 520
	BMI loop
	BRK
.ORG 520
.BYTE 6
	; expect x=6,a=42,n=0
 
 
; CPY Immediate - compute 3x20 the hard way
	CLD
	LDA #0
	TAY
loop:	INY
	CLC
	ADC #3
	CPY #20
	BNE loop
	BRK
	; expect y=20,a=60,z=1
	
	
; CPY Zero Page - compute 5x5 the hard way
	CLD
	LDA #0
	TAY
loop:	INY
	CLC
	ADC #5
	CPY 200
	BCC loop
	BRK
.ORG 200
.BYTE 5
	; expect Y=5,a=25,c=1

	
; CPY Absolute - Compute 6x7 the hard way
	CLD
	LDA #0
	TAY
loop:	INY
	CLC
	ADC #7
	CPY 520
	BMI loop
	BRK
.ORG 520
.BYTE 6
	; expect y=6,a=42,n=0

	
	
; ***************************************	
; *** Jumping and subroutines testing ***
; ***************************************

; JMP (both direct and indirect)
	JMP 512
	BRK
.ORG 512
	JMP (768)
	BRK
.ORG 768
	.WORD 1970
.ORG 1970
	LDA #42
	BRK
	; expect 

	
; JSR - Multipy 4x4 the hard and ineffiecent way
	CLD
	LDA #0
	TAY
loop:	INY
	JSR add4
	CPY 520
	BMI loop
	BRK
.ORG 520
.BYTE 4
add4: CLC
	ADC #4
	RTS
	; expect a 16
	
	
	
; **********************************	
; *** Logical operations testing ***
; **********************************

; AND Immediate and Zero Page 
	LDA #$F0
	AND #$33 ; $30 result
	TAX
	AND 200 ; 30 and 03 = 0 to test Z
	BRK
.ORG 200
.BYTE 3
	; expect X=$30, A =0, Z=1, N=0

	
; AND Absolute and Zero Page,X 
	LDA $F0
	AND 512 ; $90 result
	TAY
	LDX #11
	AND 189,X ; 90 and 88 = 8 to test Z
	BRK
.ORG 200
.BYTE 88 
.ORG 512
.BYTE $99 
	; expect Y=$90, A =128, Z=0, N=1

	
; AND Absolute,X and (Indirect,X)
	LDA $0F
	LDX #11
	AND (189,X) ; $09 result
	TAY
	AND 502,X ; 9 and 88 = 8
	BRK
.ORG 200
.WORD 512 
.ORG 512
.BYTE $99 $88 
	; expect Y=$9, A =8, Z=0, N=0

	
; AND Absolute,Y  and (Indirect,Y) 
	LDA $0F
	LDY #11
	AND (200),Y ; $09 result
	TAX
	AND 502,Y ; 9 and 88 = 8
	BRK
.ORG 200
.WORD 501 
.ORG 512
.BYTE $99 $88 
	; expect Y=$9, A =8, Z=0, N=0

	
	
; EOR Immediate and Zero Page 
	LDA #$F0
	EOR #$33 ; $C3 result
	TAX
	EOR 200 ; C3 xor C3 = 0 to test Z
	BRK
.ORG 200
.BYTE $C3
	; expect X=$C3, A =0, Z=1, N=0

	
; EOR Absolute and Zero Page,X 
	LDA $F0
	EOR 512 ; $FF result
	TAY
	LDX #11
	EOR 189,X ; FF and 88 = 77
	BRK
.ORG 200
.BYTE 88 
.ORG 512
.BYTE $0F 
	; expect Y=$FF, A =$77, Z=0, N=0

	
; EOR Absolute,X and (Indirect,X)
	LDA $0F
	LDX #11
	EOR (189,X) ; $96 result
	TAY
	EOR 502,X ; 9 and 77 = E1
	BRK
.ORG 200
.WORD 512 
.ORG 512
.BYTE $99 $77 
	; expect Y=$96, A =$E1, Z=0, N=1

	
; EOR Absolute,Y  and (Indirect,Y) 
	LDA $0F
	LDY #11
	EOR (200),Y ; $96 result
	TAX
	EOR 502,Y ; 9 and 88 = 1E
	BRK
.ORG 200
.WORD 501 
.ORG 512
.BYTE $99 $88 
	; expect Y=$96, A =$1E, Z=0, N=0

	
; ORA Immediate and Zero Page 
	LDA #$F0
	ORA #$33 ; $30 result
	TAX
	LDA #0
	ORA 200 ; 0 and 0 = 0 to test Z
	BRK
.ORG 200
.BYTE 0
	; expect X=$F3, A =0, Z=1, N=0

	
; ORA Absolute and Zero Page,X 
	LDA $F0
	ORA 512 ; $F9 result
	TAY
	LDX #11
	ORA 189,X ; F9 and 88 = F9 
	BRK
.ORG 200
.BYTE 88 
.ORG 512
.BYTE $99 
	; expect Y=$F9, A =$F9, Z=0, N=1

	
; ORA Absolute,X and (Indirect,X)
	LDA $0F
	LDX #11
	ORA (189,X) ; $9F result
	TAY
	ORA 502,X ; 9F and 77 = FF
	BRK
.ORG 200
.WORD 512 
.ORG 512
.BYTE $99 $77 
	; expect Y=$9F, A =$FF, Z=0, N=0

	
; ORA Absolute,Y  and (Indirect,Y) 
	LDA $0F
	LDY #11
	ORA (200),Y ; $1F result
	TAX
	ORA 502,Y ; 1F and 22 = 3F
	BRK
.ORG 200
.WORD 501 
.ORG 512
.BYTE $11 $22 
	; expect X=$1F, A =3F, Z=0, N=0

	
; BIT Zero Page
	LDA #$0F
	BIT 200
	BRK
.ORG 200
.BYTE $FF
	; A=$0F, Z=0, N=1, V=1
	
	
; BIT Absolute
	LDA #$F0
	BIT 512
	BRK
.ORG 512
.BYTE $0F
	; A=$F0, Z=1, N=0, V=0


; *************************************	
; *** Shifting and Rotating testing ***
; *************************************

; ROL accumulator 
	CLC
	LDA #128
	ROL A
	BRK
	; expect A=0, Z=1, C=1, N=0


; ROL with memory	
	LDX #1
	ROL 254
	ROL 254,X
	ROL 256
	ROL 256,X
	BRK
.ORG 254
.BYTE 128 127 $77 $77
	;expect MFE=0 MFF=255 M100=$EE M101=$EE N=1, Z=0, C=0

	
; ASL accumulator 
	LDA #128
	SEC
	ASL A
	BRK
	; expect A=0, Z=1, C=1, N=0


; ASL with memory	
	LDX #1
	ASL 254
	ASL 254,X
	ASL 256
	ASL 256,X
	BRK
.ORG 254
.BYTE 128 127 $F7 $77
	;expect MFE=0 MFF=254 M100=$EE M101=$EE N=1, Z=0, C=0


; ROR accumulator 
	CLC
	LDA #1
	ROR A
	BRK
	; expect A=0, Z=1, C=1, N=0


; ROR with memory	
	LDX #1
	ROR 254
	ROR 254,X
	ROR 256
	ROR 256,X
	BRK
.ORG 254
.BYTE 1 254 $EF 254
	;expect MFE=0 MFF=255 M100=$77 M101=$FF N=1, Z=0, C=0


; LSR accumulator 
	LDA #1
	SEC
	LSR A
	BRK
	; expect A=0, Z=1, C=1, N=0


; LSR with memory	
	LDX #1
	LSR 254
	LSR 254,X
	LSR 256
	LSR 256,X
	BRK
.ORG 254
.BYTE 128 127 $EE $76
	;expect MFE=64 MFF=63 M100=$77 M101=$3B N=1, Z=0, C=0

	
; ***************************	
; *** Interrupt handling  ***
; ***************************

; RTI
	LDX $FC	; for test, set up stack
	TXS		; the interrupt data alreay in there (see org)
	CLC		; the carry flag is set in the stack
	RTI		; return using stack data
	BRK
.ORG $1FD
.BYTE  33 8 2
.ORG 520
	BCC done	; the carry flag is set in the stack!
	LDX #42
done: BRK
	; expect X=42

	
; BRK  -> requires IRQ set to 512, cycles = 71
	LDX #255 ; 2
	TXS		 ; 4
loop: BRK		; 11, 33, 55, END
	; start here with 22, 44, 66
	INX 	; 24, 46, 68
	JMP loop	;27, 49, 71
.ORG 512
	; reach here at 11, 33, 55
	TAY	; 13, 35, 57
	TXA	; 15, 37, 59
	RTI ; 22, 44, 66
	;x=2, y = 0, A = 1

	
; NOP -> run for 32 cycles
	LDX #0		;2
	loop: NOP	; 4, 15, 26
	NOP			; 6, 17, 28
	NOP			; 8, 19, 30
	INX			; 10, 21, 32
	BNE loop	;13, 24
	; expect at 32 cycles for x=3
	