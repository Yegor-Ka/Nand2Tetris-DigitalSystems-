import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CodeWriter {
    private PrintWriter pw;
    private String fileName;
    private int callCount = 0;
    private String currentFunctionName = "";

    private int labelCount = 0;

    /**
     * Constructs a CodeWriter for translating VM code to Hack assembly.
     *
     * <p>If the input {@code file} is a directory, creates an output file named
     * {@code <directory>.asm} inside it. If it's a single VM file, generates an
     * assembly file with the same name but a {@code .asm} extension in the same directory.
     *
     * <p>Initializes the {@code PrintWriter} for output and sets the {@code fileName}
     * for handling static variables. Prints an error message if file creation fails.
     *
     * @param file the input {@link File}, either a directory containing VM files or a single VM file
     */
    public CodeWriter(File file) {
        try {
            File outputFile;
            if(file.isDirectory()){
                outputFile = new File(file, file.getName() + ".asm");
            }else{
                outputFile = new File(file.getParent(), file.getName().replace(".vm", ".asm"));
            }

            pw = new PrintWriter(new FileWriter(outputFile));
            fileName = outputFile.getName();
        } catch (IOException e) {
            System.err.println("Error creating output file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets the name of the current file being translated.
     * This is used for generating static variable references.
     *
     * @param fileName the name of the current file being translated
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    /**
     * Writes the Hack assembly code for the specified arithmetic or logical command.
     *
     * <p>Generates assembly code for commands like {@code add}, {@code sub}, {@code neg},
     * {@code eq}, {@code gt}, {@code lt}, {@code and}, {@code or}, and {@code not}.
     *
     * @param command the arithmetic or logical command to translate
     */
    public void writeArithmetic(String command) {
        pw.println("// " + command);

        switch (command) {
            case "add":
                binaryOperation("M=D+M");
                break;
            case "sub":
                binaryOperation("M=M-D");
                break;
            case "neg":
                unaryOperation("M=-M");
                break;
            case "and":
                binaryOperation("M=D&M");
                break;
            case "or":
                binaryOperation("M=D|M");
                break;
            case "not":
                unaryOperation("M=!M");
                break;
            case "eq":
                writeCompareLogic("JEQ");
                break;
            case "gt":
                writeCompareLogic("JGT");
                break;
            case "lt":
                writeCompareLogic("JLT");
                break;
        }
    }


    /**
     * Writes the Hack assembly code for a push or pop command.
     *
     * <p>For {@code push}, loads the specified value or segment into the stack.
     * For {@code pop}, stores the top value of the stack into the specified segment.
     *
     * @param commandType the type of the command, either {@code C_PUSH} or {@code C_POP}
     * @param segment     the memory segment (e.g., {@code constant}, {@code local}, {@code argument})
     * @param index       the index within the memory segment
     */
    public void writePushPop(CommandType commandType, String segment, int index) {
        pw.println("// " + (commandType == CommandType.C_PUSH ? "push " : "pop ") + segment + " " + index);

        if (commandType == CommandType.C_PUSH) {
            switch (segment) {
                case "constant":
                    pw.println("@" + index);
                    pw.println("D=A");
                    break;
                case "local":
                    loadSegment("LCL", index, "D=M");
                    break;
                case "argument":
                    loadSegment("ARG", index, "D=M");
                    break;
                case "this":
                    loadSegment("THIS", index, "D=M");
                    break;
                case "that":
                    loadSegment("THAT", index, "D=M");
                    break;
                case "pointer":
                    // pointer 0 => THIS, pointer 1 => THAT
                    if (index == 0) {
                        pw.println("@THIS");
                    } else {
                        pw.println("@THAT");
                    }
                    pw.println("D=M");
                    break;
                case "temp":
                    pw.println("@R" + (5 + index));
                    pw.println("D=M");
                    break;
                case "static":
                    pw.println("@" + fileName.replace(".vm", "") + index);
                    pw.println("D=M");
                    break;
            }
            pushDToStack();
        } else if (commandType == CommandType.C_POP) {
            if (segment.equals("pointer")) {
                // pop pointer i => THIS/THAT = top of stack
                popStackToD();
                if (index == 0) {
                    pw.println("@THIS");
                } else {
                    pw.println("@THAT");
                }
                pw.println("M=D");
                return; // Important to return here so we don't do the other logic
            } else if (segment.equals("temp")) {
                // pop temp i
                pw.println("@" + (5 + index));
                pw.println("D=A");
            } else if (segment.equals("static")) {
                // pop static i
                pw.println("@" + fileName.replace(".vm", "") + index);
                pw.println("D=A");
            } else {
                // local, argument, this, that
                storeTargetAddress(
                        switch (segment) {
                            case "local" -> "LCL";
                            case "argument" -> "ARG";
                            case "this" -> "THIS";
                            case "that" -> "THAT";
                            default -> throw new RuntimeException("Invalid segment: " + segment);
                        },
                        index
                );
            }

            // For non-pointer segments:
            pw.println("@R13");
            pw.println("M=D");
            popStackToD();
            pw.println("@R13");
            pw.println("A=M");
            pw.println("M=D");
        }
    }

    /**
     * Closes the output file stream.
     * Ensures that all written data is flushed and the file is properly closed.
     */
    public void close() {
        if (pw != null) {
            pw.close();
        }
    }

    /**
     * Increments the stack pointer (SP).
     * Updates SP to point to the next available stack location.
     */
    private void incrementStackPointer() {
        pw.println("@SP");
        pw.println("M=M+1");
    }

    /**
     * Decrements the stack pointer (SP).
     * Updates SP to point to the previous stack location.
     */
    private void decrementStackPointer() {
        pw.println("@SP");
        pw.println("M=M-1");
    }

    /**
     * Pops the top stack value into the D register.
     * Decrements SP and stores the value at the top of the stack into D.
     */
    private void popStackToD() {
        decrementStackPointer();
        pw.println("A=M");
        pw.println("D=M");
    }

    /**
     * Pushes the value in the D register onto the stack.
     * Stores D at the top of the stack and increments SP.
     */
    private void pushDToStack() {
        loadStackPointerToA();
        pw.println("M=D");
        incrementStackPointer();
    }

    /**
     * Loads the stack pointer's address into A.
     * Points A to the current top of the stack.
     */
    private void loadStackPointerToA() {
        pw.println("@SP");
        pw.println("A=M");
    }


    /**
     * Loads a segment address with an offset into A and performs an operation.
     *
     * @param segment   the base address of the segment (e.g., LCL, ARG)
     * @param index     the offset within the segment
     * @param operation the operation to perform after loading the address
     */
    private void loadSegment(String segment, int index, String operation) {
        pw.println("@" + segment);
        pw.println("D=M");
        pw.println("@" + index);
        pw.println("A=D+A");
        pw.println(operation);
    }

    /**
     * Stores the address of a target location into D.
     *
     * @param segment the base address of the segment
     * @param index   the offset within the segment
     */
    private void storeTargetAddress(String segment, int index) {
        pw.println("@" + segment);
        pw.println("D=M");
        pw.println("@" + index);
        pw.println("D=D+A");
    }

    /**
     * Performs a unary operation (e.g., negation or logical NOT) on the top of the stack.
     *
     * @param operation the assembly operation to execute
     */
    private void unaryOperation(String operation) {
        decrementStackPointer();
        loadStackPointerToA();
        pw.println(operation);
        incrementStackPointer();
    }

    /**
     * Performs a binary operation (e.g., addition or subtraction) on the top two stack values.
     *
     * @param operation the assembly operation to execute
     */
    private void binaryOperation(String operation) {
        popStackToD();
        decrementStackPointer();
        loadStackPointerToA();
        pw.println(operation);
        incrementStackPointer();
    }

    /**
     * Writes comparison logic (e.g., eq, gt, lt).
     *
     * @param jmpCommand the jump command for the comparison (e.g., JEQ, JGT, JLT)
     */
    private void writeCompareLogic(String jmpCommand) {
        popStackToD();
        decrementStackPointer();
        loadStackPointerToA();
        pw.println("D=M-D");
        pw.println("@LABEL" + labelCount);
        pw.println("D;" + jmpCommand);
        loadStackPointerToA();
        pw.println("M=0");
        pw.println("@ENDLABEL" + labelCount);
        pw.println("0;JMP");
        pw.println("(LABEL" + labelCount + ")");
        loadStackPointerToA();
        pw.println("M=-1");
        pw.println("(ENDLABEL" + labelCount + ")");
        incrementStackPointer();
        labelCount++;
    }

    /**
     * Writes a label for the current function.
     *
     * @param label the label name
     */
    public void writeLabel(String label) {
        pw.println("// label " + label);
        String fullLabel = currentFunctionName.isEmpty() ? label : (currentFunctionName + "$" + label);
        pw.println("(" + fullLabel + ")");
    }

    /**
     * Writes a goto command.
     *
     * @param label the label to jump to
     */
    public void writeGoto(String label) {
        pw.println("// goto " + label);
        String fullLabel = currentFunctionName.isEmpty() ? label : (currentFunctionName + "$" + label);
        pw.println("@" + fullLabel);
        pw.println("0;JMP");
    }

    /**
     * Writes an if-goto command.
     *
     * @param label the label to conditionally jump to
     */
    public void writeIf(String label) {
        pw.println("// if-goto " + label);
        popStackToD();
        String fullLabel = currentFunctionName.isEmpty() ? label : (currentFunctionName + "$" + label);
        pw.println("@" + fullLabel);
        pw.println("D;JNE");
    }

    /**
     * Writes a function declaration with local variable initialization.
     *
     * @param functionName the name of the function
     * @param nVars        the number of local variables to initialize
     */
    public void writeFunction(String functionName, int nVars) {
        pw.println("// function " + functionName + " " + nVars);
        currentFunctionName = functionName;
        pw.println("(" + functionName + ")");
        // Initialize local vars to 0
        for (int i = 0; i < nVars; i++) {
            pw.println("@SP");
            pw.println("A=M");
            pw.println("M=0");
            incrementStackPointer();
        }
    }

    /**
     * Writes the Hack assembly code for the return command.
     */
    public void writeReturn() {
        pw.println("// return");
        // FRAME = LCL
        pw.println("@LCL");
        pw.println("D=M");
        pw.println("@R13");
        pw.println("M=D");

        // RET = *(FRAME-5)
        pw.println("@5");
        pw.println("A=D-A");
        pw.println("D=M");
        pw.println("@R14");
        pw.println("M=D");

        // *ARG = pop()
        popStackToD();
        pw.println("@ARG");
        pw.println("A=M");
        pw.println("M=D");

        // SP = ARG+1
        pw.println("@ARG");
        pw.println("D=M+1");
        pw.println("@SP");
        pw.println("M=D");

        // THAT = *(FRAME-1)
        pw.println("@R13");
        pw.println("AM=M-1");
        pw.println("D=M");
        pw.println("@THAT");
        pw.println("M=D");

        // THIS = *(FRAME-2)
        pw.println("@R13");
        pw.println("AM=M-1");
        pw.println("D=M");
        pw.println("@THIS");
        pw.println("M=D");

        // ARG = *(FRAME-3)
        pw.println("@R13");
        pw.println("AM=M-1");
        pw.println("D=M");
        pw.println("@ARG");
        pw.println("M=D");

        // LCL = *(FRAME-4)
        pw.println("@R13");
        pw.println("AM=M-1");
        pw.println("D=M");
        pw.println("@LCL");
        pw.println("M=D");

        // goto RET
        pw.println("@R14");
        pw.println("A=M");
        pw.println("0;JMP");
    }

    /**
     * Writes a call command for a function.
     *
     * @param functionName the name of the function to call
     * @param nArgs        the number of arguments passed to the function
     */
    public void writeCall(String functionName, int nArgs) {
            pw.println("// call " + functionName + " " + nArgs);
            String returnLabel = "RET_ADDRESS_" + functionName + callCount++;

            // push return-address
            pw.println("@" + returnLabel);
            pw.println("D=A");
            pushDToStack();

            // push LCL
            pw.println("@LCL");
            pw.println("D=M");
            pushDToStack();

            // push ARG
            pw.println("@ARG");
            pw.println("D=M");
            pushDToStack();

            // push THIS
            pw.println("@THIS");
            pw.println("D=M");
            pushDToStack();

            // push THAT
            pw.println("@THAT");
            pw.println("D=M");
            pushDToStack();

            // ARG = SP - nArgs - 5
            pw.println("@SP");
            pw.println("D=M");
            pw.println("@" + (nArgs + 5));
            pw.println("D=D-A");
            pw.println("@ARG");
            pw.println("M=D");

            // LCL = SP
            pw.println("@SP");
            pw.println("D=M");
            pw.println("@LCL");
            pw.println("M=D");

            // goto functionName
            pw.println("@" + functionName);
            pw.println("0;JMP");

            // (returnLabel)
            pw.println("(" + returnLabel + ")");
    }

    /**
     * Writes the bootstrap code for the VM, initializing SP and calling Sys.init.
     */
    public void writeInit() {
        // Bootstrap code: SP=256, call Sys.init
        pw.println("// bootstrap code");
        pw.println("@256");
        pw.println("D=A");
        pw.println("@SP");
        pw.println("M=D");
        writeCall("Sys.init", 0);
    }
 }

