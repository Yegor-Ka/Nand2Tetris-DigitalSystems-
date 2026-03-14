import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JackTokenizer {

    // Token types
    public static final int KEYWORD = 1, SYMBOL = 2, IDENTIFIER = 3, INT_CONST = 4, STRING_CONST = 5;

    // Keywords
    public static final int CLASS = 10, METHOD = 11, FUNCTION = 12, CONSTRUCTOR = 13, INT = 14, BOOLEAN = 15, CHAR = 16,
            VOID = 17, VAR = 18, STATIC = 19, FIELD = 20, LET = 21, DO = 22, IF = 23, ELSE = 24, WHILE = 25, RETURN = 26,
            TRUE = 27, FALSE = 28, NULL = 29, THIS = 30;

    private Scanner scanner;
    private String currentToken;
    private int currentTokenType;
    private int pointer;
    private ArrayList<String> tokens;

    private static Pattern tokenPatterns;
    private static String keyWordReg;
    private static String symbolReg;
    private static String intReg;
    private static String strReg;
    private static String idReg;

    private static final Map<String, Integer> keyWordMap = new HashMap<>();
    private static final Set<Character> opSet = new HashSet<>();

    static {
        // Initialize keyword map
        keyWordMap.put("class", CLASS);
        keyWordMap.put("constructor", CONSTRUCTOR);
        keyWordMap.put("function", FUNCTION);
        keyWordMap.put("method", METHOD);
        keyWordMap.put("field", FIELD);
        keyWordMap.put("static", STATIC);
        keyWordMap.put("var", VAR);
        keyWordMap.put("int", INT);
        keyWordMap.put("char", CHAR);
        keyWordMap.put("boolean", BOOLEAN);
        keyWordMap.put("void", VOID);
        keyWordMap.put("true", TRUE);
        keyWordMap.put("false", FALSE);
        keyWordMap.put("null", NULL);
        keyWordMap.put("this", THIS);
        keyWordMap.put("let", LET);
        keyWordMap.put("do", DO);
        keyWordMap.put("if", IF);
        keyWordMap.put("else", ELSE);
        keyWordMap.put("while", WHILE);
        keyWordMap.put("return", RETURN);

        // Initialize operator set
        opSet.add('+');
        opSet.add('-');
        opSet.add('*');
        opSet.add('/');
        opSet.add('&');
        opSet.add('|');
        opSet.add('<');
        opSet.add('>');
        opSet.add('=');
    }

    /**
     * Initializes the tokenizer with the given file.
     * Removes comments and whitespace, then tokenizes the content.
     *
     * @param inFile the input Jack file
     */
    public JackTokenizer(File inFile) {
        try {
            scanner = new Scanner(inFile);
            StringBuilder preprocessed = new StringBuilder();
            // Process each line: remove single-line comments and trim whitespace
            while (scanner.hasNext()) {
                String line = noComments(scanner.nextLine().trim());
                if (!line.isEmpty()) {
                    preprocessed.append(line).append("\n");
                }
            }
            // Remove block comments and trim the result
            String content = noBlockComments(preprocessed.toString()).trim();

            // Initialize regex patterns and tokenize the content
            initRegexPatterns();
            tokenizeContent(content);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes regular expression patterns for tokenization.
     */
    private void initRegexPatterns() {
        keyWordReg = String.join("|", keyWordMap.keySet());
        symbolReg = "[\\&\\*\\+\\(\\)\\.\\/\\,\\-\\]\\;\\~\\}\\|\\{\\>\\=\\[\\<]";
        intReg = "\\d+";
        strReg = "\"[^\"\n]*\"";
        idReg = "[\\w_][\\w\\d_]*";

        tokenPatterns = Pattern.compile(keyWordReg + "|" + symbolReg + "|" + intReg + "|" + strReg + "|" + idReg);
    }

    /**
     * Tokenizes the preprocessed content using regex patterns.
     *
     * @param content the content to tokenize
     */
    private void tokenizeContent(String content) {
        Matcher matcher = tokenPatterns.matcher(content);
        tokens = new ArrayList<>();
        pointer = 0;
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        currentToken = "";
        currentTokenType = -1;
    }

    /**
     * Checks if there are more tokens to process.
     *
     * @return true if there are more tokens, false otherwise
     */
    public boolean hasMoreTokens() {
        return pointer < tokens.size();
    }

    /**
     * Advances to the next token and sets its type.
     * Throws an exception if no more tokens are available.
     */
    public void advance() {
        if (hasMoreTokens()) {
            currentToken = tokens.get(pointer++);
            setTokenType();
        } else {
            throw new IllegalStateException("No more tokens available.");
        }
    }

    /**
     * Returns the current token.
     *
     * @return the current token as a string
     */
    public String getCurrentToken() {
        return currentToken;
    }

    /**
     * Returns the type of the current token.
     *
     * @return the type of the current token
     */
    public int tokenType() {
        return currentTokenType;
    }

    /**
     * Returns the keyword type of the current token.
     * Throws an exception if the current token is not a keyword.
     *
     * @return the keyword type as an integer
     */
    public int keyWord() {
        if (currentTokenType != KEYWORD) {
            throw new IllegalStateException("Current token is not a keyword.");
        }
        return keyWordMap.get(currentToken);
    }

    /**
     * Returns the symbol represented by the current token.
     * Throws an exception if the current token is not a symbol.
     *
     * @return the current symbol as a character
     */
    public char symbol() {
        if (currentTokenType != SYMBOL) {
            throw new IllegalStateException("Current token is not a symbol.");
        }
        return currentToken.charAt(0);
    }

    /**
     * Returns the identifier represented by the current token.
     * Throws an exception if the current token is not an identifier.
     *
     * @return the current identifier as a string
     */
    public String identifier() {
        if (currentTokenType != IDENTIFIER) {
            throw new IllegalStateException("Current token is not an identifier.");
        }
        return currentToken;
    }

    /**
     * Returns the integer value of the current token.
     * Throws an exception if the current token is not an integer constant.
     *
     * @return the integer value of the current token
     */
    public int intVal() {
        if (currentTokenType != INT_CONST) {
            throw new IllegalStateException("Current token is not an integer constant.");
        }
        return Integer.parseInt(currentToken);
    }

    /**
     * Returns the string value of the current token, without surrounding quotes.
     * Throws an exception if the current token is not a string constant.
     *
     * @return the string value of the current token
     */
    public String stringVal() {
        if (currentTokenType != STRING_CONST) {
            throw new IllegalStateException("Current token is not a string constant.");
        }
        return currentToken.substring(1, currentToken.length() - 1);
    }

    /**
     * Moves the pointer back by one token, if possible.
     */
    public void pointerBack() {
        if (pointer > 0) {
            pointer--;
        }
    }

    /**
     * Checks if the current symbol is a valid operator.
     *
     * @return true if the current symbol is an operator, false otherwise
     */
    public boolean isOp() {
        return opSet.contains(symbol());
    }

    /**
     * Removes single-line comments (//) from a string.
     *
     * @param line the input string
     * @return the string with comments removed
     */
    public static String noComments(String line) {
        int index = line.indexOf("//");
        return index >= 0 ? line.substring(0, index) : line;
    }

    /**
     * Removes multi-line block comments
     * @param content the input content
     * @return the content with block comments removed
     */
    public static String noBlockComments(String content) {
        int startIndex = content.indexOf("/*");
        while (startIndex != -1) {
            int endIndex = content.indexOf("*/", startIndex + 2);
            if (endIndex == -1) {
                return content.substring(0, startIndex).trim();
            }
            content = content.substring(0, startIndex) + content.substring(endIndex + 2);
            startIndex = content.indexOf("/*");
        }
        return content;
    }

    /**
     * Sets the type of the current token based on its value.
     */
    private void setTokenType() {
        if (keyWordMap.containsKey(currentToken)) {
            currentTokenType = KEYWORD;
        } else if (currentToken.matches(symbolReg)) {
            currentTokenType = SYMBOL;
        } else if (currentToken.matches(intReg)) {
            currentTokenType = INT_CONST;
        } else if (currentToken.matches(strReg)) {
            currentTokenType = STRING_CONST;
        } else if (currentToken.matches(idReg)) {
            currentTokenType = IDENTIFIER;
        } else {
            throw new IllegalArgumentException("Unknown token: " + currentToken);
        }
    }
}
