// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/4/Fill.asm

// Runs an infinite loop that listens to the keyboard input. 
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel. When no key is pressed, 
// the screen should be cleared.
// Initialize Variables
(START) 
//set keyboard adress for input of keyboard
    @KBD
    D=A
    @key
    M=D
(KEY_PRESSED)
    // curr = last pixel map
    @24575
    D=A
    @curr_pixel
    M=D
    //if key is pressed draw black pixel
    @key
    A=M
    D=M
    @fillsc
    M=-1
    @DRAW
    D;JNE
    //else
    @fillsc
    M=0
(DRAW)
//fill or clear screen
    @fillsc
    D=M
    @DRAW
    A=M
    M=D
    //if last pixel on screen is a first pixel on screem back to key pressed check
    @curr_pixel
    D=M
    @SCREEN
    D=D-A
    @KEY_PRESSED
    D;JLE
    //decrement pixel map
    @curr_pixel
    M=M-1
    //continue drawing
    @DRAW
    0;JMP

