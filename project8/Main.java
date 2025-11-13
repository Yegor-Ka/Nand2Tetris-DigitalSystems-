import java.io.*;
import java.util.Scanner;


/**
 * Final version of VMtranslator.
 */
public class Main {
    private static CodeWriter codeWriter;

    /**
     * Main entry point for the VM translator.
     *
     * <p>Accepts a single command-line argument specifying a file or directory containing VM code.
     * Depending on the input, the translator processes one or more VM files, translates them into Hack assembly,
     * and writes the output to a corresponding `.asm` file. If the input is a directory, all `.vm` files in
     * the directory (and its subdirectories) are processed.</p>
     *
     * <p>If the input includes a `Sys.vm` file or is `Sys.vm` directly, bootstrap code is generated to initialize
     * the stack and call `Sys.init`.</p>
     *
     * @param args Command-line arguments. The first argument must be the file or directory to process.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Main <fileOrDirectory>");
            return;
        }

        File inputFile = new File(args[0]);
        if (!inputFile.exists()) {
            System.out.println("File not found: " + args[0]);
            return;
        }

        codeWriter = new CodeWriter(inputFile);

        // If we need to write bootstrap code:
        // Write bootstrap code if:
        // 1. Input is a directory containing Sys.vm
        // 2. Input is exactly Sys.vm
        boolean writeInit = false;

        if (inputFile.isDirectory()) {
            // Check if Sys.vm is present in the directory
            File[] files = inputFile.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().equals("Sys.vm")) {
                        writeInit = true;
                        break;
                    }
                }
            }
        } else {
            // If it's a single file and named Sys.vm
            if (inputFile.getName().equals("Sys.vm")) {
                writeInit = true;
            }
        }

        // If we decided to write init, do it now
        if (writeInit) {
            codeWriter.writeInit();
        }

        // Translate files
        if (inputFile.isDirectory()) {
            translateDirectory(inputFile);
        } else {
            translateFile(inputFile);
        }

        codeWriter.close();
    }

    /**
     * Translates all `.vm` files in a given directory to Hack assembly.
     *
     * <p>Recursively processes the directory and its subdirectories, translating
     * any `.vm` files encountered. For each file, {@link #translateFile(File)} is called.</p>
     *
     * @param directory The input directory containing `.vm` files
     */
    private static void translateDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                translateDirectory(file);
            } else {
                if (file.getName().endsWith(".vm")) {
                    translateFile(file);
                }
            }
        }
    }


    /**
     * Translates a single `.vm` file to a corresponding `.asm` file.
     *
     * <p>Parses the input VM file, identifies commands, and generates Hack assembly instructions
     * using the {@link Parser} and {@link CodeWriter} classes. The output file is named
     * after the input file, replacing the `.vm` extension with `.asm`.</p>
     *
     * @param file The input `.vm` file to translate
     */
    private static void translateFile(File file) {
        try (Scanner inputScanner = new Scanner(file)) {
            Parser parser = new Parser(inputScanner);
            String asmFileName = file.getName().replace(".vm", ".asm");
            codeWriter.setFileName(asmFileName);

            while (parser.hasMoreLines()) {
                parser.advance();
                CommandType ct = parser.commandType();
                switch (ct) {
                    case C_PUSH:
                    case C_POP:
                        codeWriter.writePushPop(ct, parser.arg1(), parser.arg2());
                        break;
                    case C_ARITHMETIC:
                        codeWriter.writeArithmetic(parser.arg1());
                        break;
                    case C_LABEL:
                        codeWriter.writeLabel(parser.arg1());
                        break;
                    case C_GOTO:
                        codeWriter.writeGoto(parser.arg1());
                        break;
                    case C_IF:
                        codeWriter.writeIf(parser.arg1());
                        break;
                    case C_FUNCTION:
                        codeWriter.writeFunction(parser.arg1(), parser.arg2());
                        break;
                    case C_CALL:
                        codeWriter.writeCall(parser.arg1(), parser.arg2());
                        break;
                    case C_RETURN:
                        codeWriter.writeReturn();
                        break;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
