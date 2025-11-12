// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/4/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)
// The algorithm is based on repetitive addition.

(START)
//at the start set R[2] = 0
    @R2
    M=0
(LOOP)
    //if R[1] = 0, end loop
    @R1
    D=M
    @END
    D;JLE
    //R[2] = R[2] + R[0]
    @R0
    D=M
    @R2
    M=D+M
    //R[1] = R[1] - 1
    @R1
    M=M-1
    //do it untill R[1] = 0, then go to first line and end loop
    @LOOP
    0;JMP
(END)
//ends loop
    @END
    0;JMP