import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JackTokenizer {

    // Token types
    public static enum TYPE {KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST, NONE}
    public static enum KEYWORD {CLASS, METHOD, FUNCTION,
        CONSTRUCTOR, INT, BOOLEAN,
        CHAR, VOID, VAR,
        STATIC, FIELD, LET,
        DO, IF, ELSE,
        WHILE, RETURN, TRUE,
        FALSE, NULL, THIS}

    private String currentToken;
    private TYPE currentTokenType;
    private int pointer;
    private List<String> tokens;

    private static final Pattern tokenPatterns;
    private static final String keyWordReg;
    private static final String symbolReg = "[\\&\\*\\+\\(\\)\\.\\/\\,\\-\\]\\;\\~\\}\\|\\{\\>\\=\\[\\<]";
    private static final String intReg = "[0-9]+";
    private static final String strReg = "\"[^\"\n]*\"";
    private static final String idReg = "[a-zA-Z_]\\w*";

    private static final EnumMap<KEYWORD, String> keyWordMap = new EnumMap<>(KEYWORD.class);
    private static final Set<Character> opSet = Set.of('+', '-', '*', '/', '&', '|', '<', '>', '=');

    static {
        // Initialize keyword mapping
        for (KEYWORD keyword : KEYWORD.values()) {
            keyWordMap.put(keyword, keyword.name().toLowerCase());
        }

        // Build keyword regex
        keyWordReg = String.join("|", keyWordMap.values());

        // Compile the pattern for token matching
        tokenPatterns = Pattern.compile(idReg + "|" + keyWordReg + "|" + symbolReg + "|" + intReg + "|" + strReg);
    }

    /**
     * Initializes the tokenizer with the input file.
     */
    public JackTokenizer(File inFile) {
        tokens = new ArrayList<>();
        StringBuilder preprocessed = new StringBuilder();

        try (Scanner scanner = new Scanner(inFile)) {
            while (scanner.hasNext()) {
                String line = noComments(scanner.nextLine()).trim();
                if (!line.isEmpty()) {
                    preprocessed.append(line).append("\n");
                }
            }
            String cleanedCode = noBlockComments(preprocessed.toString()).trim();

            Matcher matcher = tokenPatterns.matcher(cleanedCode);
            while (matcher.find()) {
                tokens.add(matcher.group());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + inFile.getPath(), e);
        }

        pointer = 0;
        currentToken = "";
        currentTokenType = TYPE.NONE;
    }

    /**
     * Checks if there are more tokens available.
     */
    public boolean hasMoreTokens() {
        return pointer < tokens.size();
    }

    /**
     * Advances to the next token.
     */
    public void advance() {
        if (!hasMoreTokens()) {
            throw new IllegalStateException("No more tokens available.");
        }

        currentToken = tokens.get(pointer++);
        determineTokenType();
    }

    /**
     * Determines the type of the current token.
     */
    private void determineTokenType() {
        if (currentToken.matches(keyWordReg)) {
            currentTokenType = TYPE.KEYWORD;
        } else if (currentToken.matches(symbolReg)) {
            currentTokenType = TYPE.SYMBOL;
        } else if (currentToken.matches(intReg)) {
            currentTokenType = TYPE.INT_CONST;
        } else if (currentToken.matches(strReg)) {
            currentTokenType = TYPE.STRING_CONST;
        } else if (currentToken.matches(idReg)) {
            currentTokenType = TYPE.IDENTIFIER;
        } else {
            throw new IllegalArgumentException("Unknown token: " + currentToken);
        }
    }

    public String getCurrentToken() {
        return currentToken;
    }

    public TYPE tokenType() {
        return currentTokenType;
    }

    public KEYWORD keyWord() {
        if (currentTokenType != TYPE.KEYWORD) {
            throw new IllegalStateException("Current token is not a keyword.");
        }
        return KEYWORD.valueOf(currentToken.toUpperCase());
    }

    public char symbol() {
        if (currentTokenType != TYPE.SYMBOL) {
            throw new IllegalStateException("Current token is not a symbol.");
        }
        return currentToken.charAt(0);
    }

    public String identifier() {
        if (currentTokenType != TYPE.IDENTIFIER) {
            throw new IllegalStateException("Current token is not an identifier.");
        }
        return currentToken;
    }

    public int intVal() {
        if (currentTokenType != TYPE.INT_CONST) {
            throw new IllegalStateException("Current token is not an integer constant.");
        }
        return Integer.parseInt(currentToken);
    }

    public String stringVal() {
        if (currentTokenType != TYPE.STRING_CONST) {
            throw new IllegalStateException("Current token is not a string constant.");
        }
        return currentToken.substring(1, currentToken.length() - 1); // Remove quotes
    }

    public void pointerBack() {
        if (pointer > 0) {
            currentToken = tokens.get(--pointer);
            determineTokenType();
        }
    }

    public boolean isOp() {
        return currentTokenType == TYPE.SYMBOL && opSet.contains(symbol());
    }

    public static String noComments(String input) {
        int pos = input.indexOf("//");
        return (pos != -1) ? input.substring(0, pos) : input;
    }

    public static String noBlockComments(String input) {
        int start = input.indexOf("/*");
        if (start == -1) return input;

        StringBuilder result = new StringBuilder(input);
        int end = input.indexOf("*/");

        while (start != -1) {
            if (end == -1) {
                return result.substring(0, start);
            }
            result.delete(start, end + 2);
            start = result.indexOf("/*");
            end = result.indexOf("*/");
        }

        return result.toString();
    }
}
