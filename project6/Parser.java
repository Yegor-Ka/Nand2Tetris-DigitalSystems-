import java.io.*;


public class Parser {
    private BufferedReader reader;
    private String currentLine;
    private String nextLine;


    /**
     * Constructor: creates a Parser and opens source file.
     *
     * @param source the source file
     * @throws IOException if file cannot be read.
     */
    public Parser(File source) throws IOException {
        if (source == null) {
            throw new NullPointerException("filePath is null");
        }
        if (!source.exists()) {
            throw new FileNotFoundException(source.getAbsolutePath());
        }
        this.reader = new BufferedReader(new FileReader(source));
        this.currentLine = null;
        this.nextLine = reader.readLine();
    }


    /**
     * Checks if there are more lines in the file to process.
     * @return True if more lines are available, false otherwise.
     */
    public boolean hasNextLine(){
        return this.nextLine != null;
    }


    /**
     * Reads the next line of the file, making it the current instruction.
     * @throws IOException If an error occurs during reading.
     */
    public void advance() throws IOException {
        do {
            if (!hasNextLine()) {
                throw new IllegalStateException("No more lines to advance.");
            }
            currentLine = nextLine.trim(); // Move next line to current
            nextLine = reader.readLine(); // Read the next line in advance
            // Remove comments
            if (currentLine.contains("//")) {
                currentLine = currentLine.split("//")[0].trim();
            }
        } while (currentLine.isEmpty()); // Skip blank lines
    }


    /**
     * Determines the type of the current instruction.
     * @return The type of the current instruction: A_INSTRUCTION, C_INSTRUCTION, or L_INSTRUCTION.
     */
    public InstructionType instructionType(){
        if(currentLine.startsWith("@")) {
            return InstructionType.A_INSTRUCTION;
        }else if (currentLine.startsWith("(") && currentLine.endsWith(")")) {
            return InstructionType.L_INSTRUCTION;
        }else{
            return InstructionType.C_INSTRUCTION;
        }
    }


    /**
     * Returns the symbol or decimal value of the current command (@Xxx or (Xxx)).
     * Should be called only when the instruction type is A_INSTRUCTION or L_INSTRUCTION.
     *
     * @return The symbol or value of the instruction.
     * @throws IllegalStateException If called on a C_INSTRUCTION.
     */
    public String symbol(){
        InstructionType type = instructionType();
        if(type == InstructionType.A_INSTRUCTION) {
            return currentLine.substring(1);//removes "@"
        }else if(type == InstructionType.L_INSTRUCTION) {
            return currentLine.substring(1, currentLine.length()-1); //removes "(" and ")"
        }else{
            throw new IllegalStateException("Instruction type is not A_INSTRUCTION or L_INSTRUCTION");
        }
    }


    /**
     * Returns the destination mnemonic of the current C-instruction.
     * Should be called only when the instruction type is C_INSTRUCTION.
     *
     * @return The destination part of the instruction, or null if not present.
     * @throws IllegalStateException If called on a non-C_INSTRUCTION.
     */
    public String dest(){
        if(instructionType() != InstructionType.C_INSTRUCTION) {
            throw new IllegalStateException("Instruction type is not C_INSTRUCTION");
        }
        if(currentLine.contains("=")) {
            return currentLine.split("=")[0].trim(); //extract dest
        }
        return null;//no destination
    }


    /**
     * Returns the computation mnemonic of the current C-instruction.
     * Should be called only when the instruction type is C_INSTRUCTION.
     *
     * @return The computation part of the instruction.
     * @throws IllegalStateException If called on a non-C_INSTRUCTION.
     */
    public String comp(){
        if(instructionType() != InstructionType.C_INSTRUCTION) {
            throw new IllegalStateException("Instruction type is not C_INSTRUCTION");
        }
        String compPart = currentLine;
        if(compPart.contains("=")) {
            compPart = compPart.split("=")[1];//removes dest
        }
        if(compPart.contains(";")) {
            compPart = compPart.split(";")[0];//removes jump
        }
        return compPart.trim();
    }


    /**
     * Returns the jump mnemonic of the current C-instruction.
     * Should be called only when the instruction type is C_INSTRUCTION.
     *
     * @return The jump part of the instruction, or null if not present.
     * @throws IllegalStateException If called on a non-C_INSTRUCTION.
     */
    public String jump(){
        if(instructionType() != InstructionType.C_INSTRUCTION) {
            throw new IllegalStateException("Instruction type is not C_INSTRUCTION");
        }
        if(currentLine.contains(";")) {
            return currentLine.split(";")[1].trim(); //extracts jump
        }
        return null; //no jump
    }

    /**
     * Closes the file reader to release resources.
     *
     * @throws IOException If an error occurs during closing.
     */
    public void close() throws IOException {
        if(reader != null) {
            reader.close();
        }
    }

}

