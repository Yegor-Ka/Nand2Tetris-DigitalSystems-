import java.io.*;
import java.util.Objects;
import java.util.Scanner;

public class Main {
    private static CodeWriter codeWriter;

    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Usage: java Main <file>");
        }

        File inputFile = new File(args[0]);
        if(!inputFile.exists()) {
            System.out.println("File not found: " + args[0]);
        }

        codeWriter = new CodeWriter(inputFile);
        if (inputFile.isDirectory()) {
            iterateFiles(inputFile.listFiles());
        } else {
            translate(inputFile);
            System.out.println("Translated file: " + inputFile);
        }
        codeWriter.close();
    }

    private static void iterateFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                iterateFiles(Objects.requireNonNull(file.listFiles()));
            } else {
                if (file.getName().endsWith(".vm")) {
                    translate(file);
                }
            }
        }
    }

    private static void translate( File file) {
        File outputFile = new File(file.getName().split(".vm")[0] + ".asm");


        Scanner inputScanner = null;
        try {
            inputScanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Parser parser = new Parser(inputScanner);
        codeWriter.setFileName(outputFile.getName());

        while (parser.hasMoreLines()) {
            parser.advance();
            switch (parser.commandType()) {
                case C_PUSH:
                case C_POP:
                    codeWriter.writePushPop(parser.commandType(), parser.arg1(), parser.arg2());
                    break;
                case C_ARITHMETIC:
                    codeWriter.writeArithmetic(parser.arg1());
                    break;
            }
        }
    }
}
