import java.io.*;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Main <filename.asm>");
            return;
        }
        String inputFilename = args[0];
        String outputFilename = inputFilename.replace(".asm", ".hack");

        try {
            SymbolTable symbolTable = new SymbolTable();
            File sourceFile = new File(inputFilename);

            //First Pass: Build the symbol table
            firstPass(sourceFile, symbolTable);
            //Second Pass: Build the symbol table
            secondPass(sourceFile,outputFilename,symbolTable);

            System.out.println("Assembly completed, output: " + outputFilename);
        }catch (IOException e){
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * First pass: Populate the symbol table with labels.
     */
    private static void firstPass(File sourceFile, SymbolTable symbolTable) throws IOException {
        Parser parser = new Parser(sourceFile);
        int instructionAddress = 0;

        while (parser.hasNextLine()){
            parser.advance();

            InstructionType type = parser.instructionType();
            if(type == InstructionType.L_INSTRUCTION){
                String symbol = parser.symbol();
                if(!symbolTable.contains(symbol)){
                    symbolTable.addEntry(symbol,instructionAddress);
                }
            }else{
                instructionAddress++;
            }
        }
        parser.close();
    }

    /**
     * Second pass: Translate instructions into binary machine code.
     */
    public static void secondPass(File sourceFile, String outputFilename, SymbolTable symbolTable) throws IOException {
        Parser parser = new Parser(sourceFile);
        Code code = new Code();
        StringBuilder output = new StringBuilder();
        int nextAvailableAddress = 16;

        while (parser.hasNextLine()){
            parser.advance();

            InstructionType type = parser.instructionType();
            if(type == InstructionType.A_INSTRUCTION){
                String symbol = parser.symbol();
                int address;

                if(isNumeric(symbol)){
                    address = Integer.parseInt(symbol);
                }else{
                    if(!symbolTable.contains(symbol)){
                        symbolTable.addEntry(symbol,nextAvailableAddress++);
                    }
                    address = symbolTable.getAddress(symbol);
                }

                String binaryInstruction = "0" + code.formatNumberAsBinary(Integer.toString(address));
                output.append(binaryInstruction).append("\n");

            }else if(type == InstructionType.C_INSTRUCTION){
                String dest = Code.destMemo(parser.dest());
                String jump = Code.jumpMemo(parser.jump());
                String comp = Code.compMemo(parser.comp());

                String binaryInstruction = "111" + comp + dest + jump ;
                output.append(binaryInstruction).append("\n");

            }
        }
        parser.close();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename))) {
            writer.write(output.toString().trim());
        }
    }

    private static boolean isNumeric(String str){
        return str.matches("\\d+");
    }
}
