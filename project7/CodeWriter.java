import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CodeWriter {
    private PrintWriter pw;
    private String fileName;

    private int labelCount = 0;

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

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

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
                    pw.println("@R" + (3 + index));
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
            switch (segment) {
                case "local":
                    storeTargetAddress("LCL", index);
                    break;
                case "argument":
                    storeTargetAddress("ARG", index);
                    break;
                case "this":
                    storeTargetAddress("THIS", index);
                    break;
                case "that":
                    storeTargetAddress("THAT", index);
                    break;
                case "pointer":
                    pw.println("@R" + (3 + index));
                    pw.println("D=A");
                    break;
                case "temp":
                    pw.println("@R" + (5 + index));
                    pw.println("D=A");
                    break;
                case "static":
                    pw.println("@" + fileName.replace(".vm", "") + index);
                    pw.println("D=A");
                    break;
            }
            pw.println("@R13");
            pw.println("M=D");
            popStackToD();
            pw.println("@R13");
            pw.println("A=M");
            pw.println("M=D");
        }
    }

    public void close() {
        if (pw != null) {
            pw.close();
        }
    }

    private void incrementStackPointer() {
        pw.println("@SP");
        pw.println("M=M+1");
    }

    private void decrementStackPointer() {
        pw.println("@SP");
        pw.println("M=M-1");
    }

    private void popStackToD() {
        decrementStackPointer();
        pw.println("A=M");
        pw.println("D=M");
    }

    private void pushDToStack() {
        loadStackPointerToA();
        pw.println("M=D");
        incrementStackPointer();
    }

    private void loadStackPointerToA() {
        pw.println("@SP");
        pw.println("A=M");
    }

    private void loadSegment(String segment, int index, String operation) {
        pw.println("@" + segment);
        pw.println("D=M");
        pw.println("@" + index);
        pw.println("A=D+A");
        pw.println(operation);
    }

    private void storeTargetAddress(String segment, int index) {
        pw.println("@" + segment);
        pw.println("D=M");
        pw.println("@" + index);
        pw.println("D=D+A");
    }

    private void unaryOperation(String operation) {
        decrementStackPointer();
        loadStackPointerToA();
        pw.println(operation);
        incrementStackPointer();
    }

    private void binaryOperation(String operation) {
        popStackToD();
        decrementStackPointer();
        loadStackPointerToA();
        pw.println(operation);
        incrementStackPointer();
    }

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
}
