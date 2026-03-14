import java.io.File;
import java.util.ArrayList;

public class Main {

    /**
     * Retrieves all `.jack` files from the specified directory.
     *
     * @param directory The directory to search for `.jack` files.
     * @return A list of `.jack` files.
     */
    private static ArrayList<File> getJackFilesFromDirectory(File directory) {
        File[] files = directory.listFiles();
        ArrayList<File> jackFiles = new ArrayList<>();

        if (files == null) return jackFiles;

        for (File file : files) {
            if (file.getName().endsWith(".jack")) {
                jackFiles.add(file);
            }
        }

        return jackFiles;
    }

    /**
     * Processes a single `.jack` file by initializing the CompilationEngine and compiling the class.
     *
     * @param jackFile The `.jack` file to process.
     */
    private static void processJackFile(File jackFile) {
        try {
            String basePath = jackFile.getAbsolutePath().substring(0, jackFile.getAbsolutePath().lastIndexOf("."));
            File outputFile = new File(basePath + ".vm");  // Generate VM code instead of XML

            // Initialize the CompilationEngine and compile the class
            CompilationEngine compiler = new CompilationEngine(jackFile, outputFile);
            compiler.compileClass();  // This should now generate VM code

            // Output success message
            System.out.println("Compiled VM File: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error processing file: " + jackFile.getAbsolutePath());
            e.printStackTrace();
        }
    }

    /**
     * Main entry point for the application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Main <filename or directory>");
            return;
        }

        String inputPath = args[0];
        File inputFile = new File(inputPath);

        if (!inputFile.exists()) {
            System.out.println("Error: File or directory does not exist.");
            return;
        }

        ArrayList<File> jackFiles = new ArrayList<>();

        if (inputFile.isFile()) {
            // Process a single `.jack` file
            if (!inputPath.endsWith(".jack")) {
                System.out.println("Error: The specified file is not a .jack file.");
                return;
            }
            jackFiles.add(inputFile);
        } else if (inputFile.isDirectory()) {
            // Process all `.jack` files in the directory
            jackFiles = getJackFilesFromDirectory(inputFile);

            if (jackFiles.isEmpty()) {
                System.out.println("Error: No .jack files found in the directory.");
                return;
            }
        }

        // Process each `.jack` file
        for (File jackFile : jackFiles) {
            processJackFile(jackFile);
        }
    }
}
