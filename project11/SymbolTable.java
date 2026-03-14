import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private final Map<String, Symbol> classSymbols; // STATIC, FIELD
    private final Map<String, Symbol> subroutineSymbols; // ARG, VAR
    private final EnumMap<Symbol.KIND, Integer> indices; // Tracks counts

    /**
     * Initializes a new SymbolTable with empty scopes and counters.
     */
    public SymbolTable() {
        classSymbols = new HashMap<>();
        subroutineSymbols = new HashMap<>();
        indices = new EnumMap<>(Symbol.KIND.class);

        resetIndices();
    }

    /**
     * Starts a new subroutine scope by clearing subroutine-specific symbols.
     */
    public void startSubroutine() {
        subroutineSymbols.clear();
        indices.put(Symbol.KIND.ARG, 0);
        indices.put(Symbol.KIND.VAR, 0);
    }

    /**
     * Defines a new identifier with its name, type, and kind.
     * Assigns it a running index based on its scope.
     *
     * @param name Identifier name
     * @param type Identifier type
     * @param kind Identifier kind (STATIC, FIELD, ARG, VAR)
     */
    public void define(String name, String type, Symbol.KIND kind) {
        if (kind == Symbol.KIND.NONE) return; // Ignore invalid kinds

        Map<String, Symbol> targetScope = isClassScope(kind) ? classSymbols : subroutineSymbols;
        int index = indices.getOrDefault(kind, 0);

        targetScope.put(name, new Symbol(type, kind, index));
        indices.put(kind, index + 1);
    }

    /**
     * Returns the number of variables of the given kind in the current scope.
     *
     * @param kind The kind of variable
     * @return Count of variables of the specified kind
     */
    public int varCount(Symbol.KIND kind) {
        return indices.getOrDefault(kind, 0);
    }

    /**
     * Returns the kind of the named identifier in the current scope.
     * If not found, returns NONE.
     *
     * @param name Identifier name
     * @return The kind of the identifier
     */
    public Symbol.KIND kindOf(String name) {
        return lookup(name).map(Symbol::getKind).orElse(Symbol.KIND.NONE);
    }

    /**
     * Returns the type of the named identifier in the current scope.
     *
     * @param name Identifier name
     * @return The type of the identifier, or an empty string if not found
     */
    public String typeOf(String name) {
        return lookup(name).map(Symbol::getType).orElse("");
    }

    /**
     * Returns the index assigned to the named identifier.
     *
     * @param name Identifier name
     * @return The index of the identifier, or -1 if not found
     */
    public int indexOf(String name) {
        return lookup(name).map(Symbol::getIndex).orElse(-1);
    }

    /**
     * Looks up a symbol by name in both class and subroutine scopes.
     *
     * @param name Identifier name
     * @return An Optional containing the found Symbol, or empty if not found
     */
    private java.util.Optional<Symbol> lookup(String name) {
        return java.util.Optional.ofNullable(subroutineSymbols.getOrDefault(name, classSymbols.get(name)));
    }

    /**
     * Determines if the given kind belongs to the class scope.
     *
     * @param kind The kind to check
     * @return True if it is a STATIC or FIELD variable, false otherwise
     */
    private boolean isClassScope(Symbol.KIND kind) {
        return kind == Symbol.KIND.STATIC || kind == Symbol.KIND.FIELD;
    }

    /**
     * Resets the index counters for all symbol kinds.
     */
    private void resetIndices() {
        for (Symbol.KIND kind : Symbol.KIND.values()) {
            indices.put(kind, 0);
        }
    }
}
