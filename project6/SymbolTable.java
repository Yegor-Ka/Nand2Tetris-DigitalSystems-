import java.util.HashMap;


public class SymbolTable {
    private final HashMap<String, Integer> table;

    /**
     * Constructor: Initializes an empty symbol table and preloads predefined symbols.
     */
    public SymbolTable() {
        table = new HashMap<>();
        //Predefined symbols
        for (int i = 0; i <= 15; i++) {
            table.put("R" + i, i); // R[0] to R[15]
        }
        table.put("SCREEN", 16384);
        table.put("KBD", 24576);
        table.put("SP", 0);
        table.put("LCL", 1);
        table.put("ARG", 2);
        table.put("THIS", 3);
        table.put("THAT", 4);
    }

    /**
     * Adds a symbol-address pair to the table.
     *
     * @param symbol  the symbol to add
     * @param address the address associated with the symbol
     */

    public void addEntry(String symbol, int address) {
        table.put(symbol, address);
    }

    /**
     * Checks if the symbol table contains the given symbol.
     *
     * @param symbol the symbol to check
     * @return true if the symbol exists in the table, false otherwise
     */
    public boolean contains(String symbol) {
        return table.containsKey(symbol);
    }

    /**
     * Returns the address associated with the given symbol.
     *
     * @param symbol the symbol to look up
     * @return the address associated with the symbol
     * @throws IllegalArgumentException if the symbol is not found
     */
    public int getAddress(String symbol) {
        if(!contains(symbol)) {
            throw new IllegalArgumentException("Symbol not found: " + symbol);
        }
        return table.get(symbol);
    }


}
