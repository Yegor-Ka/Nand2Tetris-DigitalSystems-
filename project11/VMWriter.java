import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class VMWriter {

    public enum SEGMENT {CONST, ARG, LOCAL, STATIC, THIS, THAT, POINTER, TEMP, NONE}
    public enum COMMAND {ADD, SUB, NEG, EQ, GT, LT, AND, OR, NOT}

    private static final Map<SEGMENT, String> segmentStringMap = Map.ofEntries(
            Map.entry(SEGMENT.CONST, "constant"),
            Map.entry(SEGMENT.ARG, "argument"),
            Map.entry(SEGMENT.LOCAL, "local"),
            Map.entry(SEGMENT.STATIC, "static"),
            Map.entry(SEGMENT.THIS, "this"),
            Map.entry(SEGMENT.THAT, "that"),
            Map.entry(SEGMENT.POINTER, "pointer"),
            Map.entry(SEGMENT.TEMP, "temp")
    );

    private static final Map<COMMAND, String> commandStringMap = Map.ofEntries(
            Map.entry(COMMAND.ADD, "add"),
            Map.entry(COMMAND.SUB, "sub"),
            Map.entry(COMMAND.NEG, "neg"),
            Map.entry(COMMAND.EQ, "eq"),
            Map.entry(COMMAND.GT, "gt"),
            Map.entry(COMMAND.LT, "lt"),
            Map.entry(COMMAND.AND, "and"),
            Map.entry(COMMAND.OR, "or"),
            Map.entry(COMMAND.NOT, "not")
    );

    private PrintWriter printWriter;

    /**
     * Creates a new file and prepares it for writing.
     */
    public VMWriter(File fOut) {
        try {
            printWriter = new PrintWriter(fOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /** Writes a VM push command */
    public void writePush(SEGMENT segment, int index) {
        writeCommand("push", segmentStringMap.get(segment), String.valueOf(index));
    }

    /** Writes a VM pop command */
    public void writePop(SEGMENT segment, int index) {
        writeCommand("pop", segmentStringMap.get(segment), String.valueOf(index));
    }

    /** Writes a VM arithmetic command */
    public void writeArithmetic(COMMAND command) {
        writeCommand(commandStringMap.get(command));
    }

    /** Writes a VM label command */
    public void writeLabel(String label) {
        writeCommand("label", label);
    }

    /** Writes a VM goto command */
    public void writeGoto(String label) {
        writeCommand("goto", label);
    }

    /** Writes a VM if-goto command */
    public void writeIf(String label) {
        writeCommand("if-goto", label);
    }

    /** Writes a VM call command */
    public void writeCall(String name, int nArgs) {
        writeCommand("call", name, String.valueOf(nArgs));
    }

    /** Writes a VM function command */
    public void writeFunction(String name, int nLocals) {
        writeCommand("function", name, String.valueOf(nLocals));
    }

    /** Writes a VM return command */
    public void writeReturn() {
        writeCommand("return");
    }

    /** Writes a generic VM command */
    public void writeCommand(String cmd) {
        printWriter.println(cmd);
    }

    private void writeCommand(String cmd, String arg1) {
        printWriter.println(cmd + " " + arg1);
    }

    private void writeCommand(String cmd, String arg1, String arg2) {
        printWriter.println(cmd + " " + arg1 + " " + arg2);
    }

    /** Closes the output file */
    public void close() {
        printWriter.close();
    }
}
