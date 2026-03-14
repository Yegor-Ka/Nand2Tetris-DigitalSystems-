import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class CompilationEngine {

    private PrintWriter outputWriter;
    private PrintWriter tokenWriter;
    private JackTokenizer tokenizer;

    /**
     * Constructor for compiler engine
     * @param jackFile jack file to compile
     * @param outFile fileName.xml
     * @param outTokenFile fileNameT.xml
     */
    public CompilationEngine(File jackFile, File outFile, File outTokenFile) {
        try{
            tokenizer = new JackTokenizer(jackFile);
            outputWriter = new PrintWriter(outFile);
            tokenWriter = new PrintWriter(outTokenFile);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
    }

    /**
     * Compiles a type in the Jack language.
     * This method handles built-in types (int, char, boolean) as well as custom class names (identifiers).
     *
     * Steps:
     * 1. Advances the tokenizer to the next token.
     * 2. Checks if the token is one of the keyword types (int, char, boolean).
     * 3. If not a keyword type, checks if the token is an identifier (custom class name).
     * 4. If the token is neither a keyword type nor an identifier, throws an error.
     *
     * Errors:
     * Throws an error if the token is not a valid type (int, char, boolean, or a class name).
     */
    private void compileType() {
        tokenizer.advance();

        boolean isType = false;

        // Check for keyword types: int, char, boolean
        if (tokenizer.tokenType() == JackTokenizer.KEYWORD) {
            int keyword = tokenizer.keyWord();
            if (keyword == JackTokenizer.INT || keyword == JackTokenizer.CHAR || keyword == JackTokenizer.BOOLEAN) {
                writeToken("keyword", tokenizer.getCurrentToken());
                isType = true;
            }
        }

        // Check for className (identifier)
        if (!isType && tokenizer.tokenType() == JackTokenizer.IDENTIFIER) {
            writeToken("identifier", tokenizer.identifier());
            isType = true;
        }

        // Error if no valid type found
        if (!isType) {
            error("int|char|boolean|className");
        }
    }

    /**
     * Helper method to write token to both output writers.
     *
     * @param type  The type of the token (e.g., keyword, identifier).
     * @param value The value of the token.
     */
    private void writeToken(String type, String value) {
        outputWriter.print("<" + type + "> " + value + " </" + type + ">\n");
        tokenWriter.print("<" + type + "> " + value + " </" + type + ">\n");
    }

    /**
     * Compiles a complete class.
     * class: 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() {
        tokenizer.advance();

        // 'class' keyword
        if (tokenizer.tokenType() != JackTokenizer.KEYWORD || tokenizer.keyWord() != JackTokenizer.CLASS) {
            error("class");
        }
        outputWriter.print("<class>\n");
        tokenWriter.print("<tokens>\n");
        writeToken("keyword", "class");

        // className (identifier)
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.IDENTIFIER) {
            error("className");
        }
        writeToken("identifier", tokenizer.identifier());

        // '{' symbol
        requireSymbol('{');

        // classVarDec* and subroutineDec*
        compileClassVarDec();
        compileSubroutine();

        // '}' symbol
        requireSymbol('}');

        // Ensure no unexpected tokens remain
        if (tokenizer.hasMoreTokens()) {
            throw new IllegalStateException("Unexpected tokens");
        }

        // Finalize and close the class and tokens
        closeTokens();
    }

    /**
     * Helper method to close tokens and finalize the class structure.
     */
    private void closeTokens() {
        tokenWriter.print("</tokens>\n");
        outputWriter.print("</class>\n");
        outputWriter.close();
        tokenWriter.close();
    }

    /**
     * Compiles a class variable declaration in the Jack language.
     * This includes static and field variable declarations in a class.
     *
     * Format: ('static' | 'field') type varName (',' varName)* ';'
     *
     * Steps:
     * 1. Advances the tokenizer to the next token.
     * 2. Checks if the token indicates the end of the class ('}') or the start of a subroutine.
     * 3. If it's a valid class variable declaration, processes the 'static' or 'field' keyword.
     * 4. Compiles the type of the variable (e.g., int, char, boolean, or a class name).
     * 5. Processes at least one variable name (varName), followed by optional ','-separated varNames, and ends with ';'.
     * 6. Recursively checks for additional class variable declarations.
     *
     * Errors:
     * Throws an error if invalid keywords, symbols, or identifiers are encountered.
     */
    private void compileClassVarDec() {
        tokenizer.advance();

        // Check if next token is '}' (end of class)
        if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.pointerBack();
            return;
        }

        // Check if next token is a keyword
        if (tokenizer.tokenType() != JackTokenizer.KEYWORD) {
            error("Keywords");
        }

        // Check if next token starts a subroutine declaration
        if (tokenizer.keyWord() == JackTokenizer.CONSTRUCTOR ||
                tokenizer.keyWord() == JackTokenizer.FUNCTION ||
                tokenizer.keyWord() == JackTokenizer.METHOD) {
            tokenizer.pointerBack();
            return;
        }

        // Start classVarDec
        writeStartTag("classVarDec");

        // Ensure the keyword is either 'static' or 'field'
        if (tokenizer.keyWord() != JackTokenizer.STATIC && tokenizer.keyWord() != JackTokenizer.FIELD) {
            error("static or field");
        }
        writeToken("keyword", tokenizer.getCurrentToken());

        // Compile the type (int, char, boolean, or className)
        compileType();

        // Handle at least one varName, followed by ',' or ';'
        while (true) {
            // varName
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.IDENTIFIER) {
                error("identifier");
            }
            writeToken("identifier", tokenizer.identifier());

            // ',' or ';'
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.SYMBOL ||
                    (tokenizer.symbol() != ',' && tokenizer.symbol() != ';')) {
                error("',' or ';'");
            }

            // Write the symbol and decide whether to continue or end
            writeToken("symbol", String.valueOf(tokenizer.symbol()));
            if (tokenizer.symbol() == ';') {
                break;
            }
        }

        // End classVarDec
        writeEndTag("classVarDec");

        // Check for subsequent classVarDec declarations
        compileClassVarDec();
    }

    /**
     * Writes the opening tag for a section.
     *
     * @param tag The tag to write.
     */
    private void writeStartTag(String tag) {
        outputWriter.print("<" + tag + ">\n");
    }

    /**
     * Writes the closing tag for a section.
     *
     * @param tag The tag to write.
     */
    private void writeEndTag(String tag) {
        outputWriter.print("</" + tag + ">\n");
    }

    /**
     * Compiles a subroutine declaration (constructor, function, or method) in the Jack language.
     *
     * Format: ('constructor' | 'function' | 'method') ('void' | type) subroutineName '(' parameterList ')' subroutineBody
     *
     * Steps:
     * 1. Advances to the next token and checks if it is the end of the class ('}').
     * 2. Verifies that the token is a valid subroutine keyword ('constructor', 'function', 'method').
     * 3. Compiles the return type ('void' or type).
     * 4. Compiles the subroutine name (identifier).
     * 5. Compiles the parameter list enclosed in parentheses.
     * 6. Processes the subroutine body, which contains local variable declarations and statements.
     * 7. Recursively checks for additional subroutine declarations.
     *
     * Errors:
     * Throws an error if invalid tokens are encountered at any step.
     */
    private void compileSubroutine() {
        tokenizer.advance();

        // If next is '}', return
        if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.pointerBack();
            return;
        }

        // Start of a subroutine
        if (tokenizer.tokenType() != JackTokenizer.KEYWORD ||
                (tokenizer.keyWord() != JackTokenizer.CONSTRUCTOR &&
                        tokenizer.keyWord() != JackTokenizer.FUNCTION &&
                        tokenizer.keyWord() != JackTokenizer.METHOD)) {
            error("constructor|function|method");
        }

        writeStartTag("subroutineDec");
        writeToken("keyword", tokenizer.getCurrentToken());

        // 'void' or type
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.KEYWORD && tokenizer.keyWord() == JackTokenizer.VOID) {
            writeToken("keyword", "void");
        } else {
            tokenizer.pointerBack();
            compileType();
        }

        // subroutineName (identifier)
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.IDENTIFIER) {
            error("subroutineName");
        }
        writeToken("identifier", tokenizer.identifier());

        // '(' and parameterList
        requireSymbol('(');
        writeStartTag("parameterList");
        compileParameterList();
        writeEndTag("parameterList");
        requireSymbol(')');

        // subroutineBody
        compileSubroutineBody();
        writeEndTag("subroutineDec");

        // Recursively compile next subroutine
        compileSubroutine();
    }

    /**
     * Compiles the body of a subroutine in the Jack language.
     *
     * Format: '{' varDec* statements '}'
     *
     * Steps:
     * 1. Writes the opening tag for the subroutine body.
     * 2. Ensures the subroutine starts with an opening brace ('{').
     * 3. Compiles zero or more variable declarations (varDec*).
     * 4. Compiles statements enclosed in the subroutine body.
     * 5. Ensures the subroutine body ends with a closing brace ('}').
     * 6. Writes the closing tag for the subroutine body.
     *
     * Errors:
     * Throws an error if invalid tokens are encountered (e.g., missing '{' or '}' or invalid statements).
     */
    private void compileSubroutineBody() {
        writeStartTag("subroutineBody");

        // '{'
        requireSymbol('{');

        // varDec*
        compileVarDec();

        // statements
        writeStartTag("statements");
        compileStatement();
        writeEndTag("statements");

        // '}'
        requireSymbol('}');

        writeEndTag("subroutineBody");
    }

    /**
     * Compiles a single statement in the Jack language.
     *
     * Statements can be one of the following:
     * - letStatement: 'let' varName ('[' expression ']')? '=' expression ';'
     * - ifStatement: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
     * - whileStatement: 'while' '(' expression ')' '{' statements '}'
     * - doStatement: 'do' subroutineCall ';'
     * - returnStatement: 'return' expression? ';'
     *
     * Steps:
     * 1. Advances the tokenizer to the next token.
     * 2. Checks if the token is '}' (end of statements block); if so, stops further processing.
     * 3. Verifies that the token is a valid keyword indicating the start of a statement.
     * 4. Depending on the type of statement, calls the corresponding compilation function:
     *    - `compileLet()`, `compileIf()`, `compilesWhile()`, `compileDo()`, or `compileReturn()`.
     * 5. Recursively compiles the next statement in the block, if any.
     *
     * Errors:
     * Throws an error if the token is not a recognized statement keyword.
     */
    private void compileStatement() {
        tokenizer.advance();

        // If next is '}', return
        if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.pointerBack();
            return;
        }

        // Check for valid statement keywords
        if (tokenizer.tokenType() != JackTokenizer.KEYWORD) {
            error("keyword");
        }

        switch (tokenizer.keyWord()) {
            case JackTokenizer.LET -> compileLet();
            case JackTokenizer.IF -> compileIf();
            case JackTokenizer.WHILE -> compilesWhile();
            case JackTokenizer.DO -> compileDo();
            case JackTokenizer.RETURN -> compileReturn();
            default -> error("'let'|'if'|'while'|'do'|'return'");
        }

        // Recursively compile next statement
        compileStatement();
    }

    /**
     * Compiles a (possibly empty) parameter list, excluding the enclosing "()".
     * The format of a parameter list is:
     * (type varName) (',' type varName)*
     *
     * Steps:
     * 1. Check if the parameter list is empty (indicated by a ')' token).
     * 2. If not empty, process each parameter:
     *    - Compile the parameter type (e.g., int, char, boolean, or className).
     *    - Compile the parameter name (varName).
     *    - Handle ',' between parameters or ')' at the end of the list.
     * 3. Stops processing when ')' is encountered.
     *
     * Errors:
     * Throws an error if an expected type or identifier is missing or if a ',' or ')' is misplaced.
     */
    private void compileParameterList() {
        tokenizer.advance();

        // Check if the parameter list is empty (next token is ')')
        if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == ')') {
            tokenizer.pointerBack();
            return;
        }

        // Process at least one parameter
        tokenizer.pointerBack();
        while (true) {
            // Type
            compileType();

            // varName
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.IDENTIFIER) {
                error("identifier");
            }
            writeToken("identifier", tokenizer.identifier());

            // ',' or ')'
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.SYMBOL || (tokenizer.symbol() != ',' && tokenizer.symbol() != ')')) {
                error("',' or ')'");
            }

            if (tokenizer.symbol() == ',') {
                writeToken("symbol", ",");
            } else {
                tokenizer.pointerBack();
                break;
            }
        }
    }

    /**
     * Compiles a variable declaration in the format:
     * 'var' type varName (',' varName)* ';'
     *
     * Steps:
     * 1. Check if the current token is the 'var' keyword.
     *    - If not, return to stop processing further declarations.
     * 2. Compile the variable declaration:
     *    - Write the 'var' keyword.
     *    - Compile the type of the variable (e.g., int, char, boolean, or className).
     *    - Process at least one variable name (varName), separated by ','.
     *    - End the declaration with a ';'.
     * 3. Recursively handle subsequent variable declarations.
     *
     * Errors:
     * Throws an error if an expected 'var' keyword, type, identifier, or symbol (',' or ';') is missing.
     */
    private void compileVarDec() {
        tokenizer.advance();

        // If no 'var', go back
        if (tokenizer.tokenType() != JackTokenizer.KEYWORD || tokenizer.keyWord() != JackTokenizer.VAR) {
            tokenizer.pointerBack();
            return;
        }

        writeStartTag("varDec");
        writeToken("keyword", "var");

        // Type
        compileType();

        // Process at least one varName
        while (true) {
            // varName
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.IDENTIFIER) {
                error("identifier");
            }
            writeToken("identifier", tokenizer.identifier());

            // ',' or ';'
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.SYMBOL || (tokenizer.symbol() != ',' && tokenizer.symbol() != ';')) {
                error("',' or ';'");
            }

            writeToken("symbol", String.valueOf(tokenizer.symbol()));

            // Break if ';' is encountered
            if (tokenizer.symbol() == ';') {
                break;
            }
        }

        writeEndTag("varDec");

        // Recursively compile subsequent var declarations
        compileVarDec();
    }

    /**
     * Compiles a do statement.
     * Format: 'do' subroutineCall ';'
     */
    private void compileDo() {
        writeStartTag("doStatement");

        // 'do' keyword
        writeToken("keyword", "do");

        // subroutineCall
        compileSubroutineCall();

        // ';' symbol
        requireSymbol(';');

        writeEndTag("doStatement");
    }

    /**
     * Compiles a let statement in the format:
     * 'let' varName ('[' expression ']')? '=' expression ';'
     *
     * Steps:
     * 1. Write the opening XML tag for the let statement.
     * 2. Verify and write the 'let' keyword.
     * 3. Compile the variable name (varName).
     * 4. Check for an optional array index ('[' expression ']'):
     *    - If '[' is encountered, compile the enclosed expression and ensure it ends with ']'.
     *    - Otherwise, rewind to reprocess the next token as '='.
     * 5. Verify and write the '=' symbol.
     * 6. Compile the main expression following '='.
     * 7. Ensure the statement ends with a ';' symbol.
     * 8. Write the closing XML tag for the let statement.
     *
     * Errors:
     * Throws an error if any expected token (e.g., 'let', varName, '[', '=', ';') is missing or misplaced.
     */
    private void compileLet() {
        writeStartTag("letStatement");

        // 'let' keyword
        writeToken("keyword", "let");

        // varName
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.IDENTIFIER) {
            error("varName");
        }
        writeToken("identifier", tokenizer.identifier());

        // '[' or '='
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.SYMBOL || (tokenizer.symbol() != '[' && tokenizer.symbol() != '=')) {
            error("'['|'='");
        }

        // '[' expression ']'
        if (tokenizer.symbol() == '[') {
            writeToken("symbol", "[");
            compileExpression();
            requireSymbol(']');
        } else {
            tokenizer.pointerBack();
        }

        // '=' symbol
        requireSymbol('=');

        // expression
        compileExpression();

        // ';' symbol
        requireSymbol(';');

        writeEndTag("letStatement");
    }

    /**
     * Compiles a while statement.
     * Format: 'while' '(' expression ')' '{' statements '}'
     */
    private void compilesWhile() {
        writeStartTag("whileStatement");

        // 'while' keyword
        writeToken("keyword", "while");

        // '(' symbol
        requireSymbol('(');

        // expression
        compileExpression();

        // ')' symbol
        requireSymbol(')');

        // '{' symbol
        requireSymbol('{');

        // statements
        writeStartTag("statements");
        compileStatement();
        writeEndTag("statements");

        // '}' symbol
        requireSymbol('}');

        writeEndTag("whileStatement");
    }

    /**
     * Compiles a return statement.
     * Format: 'return' expression? ';'
     */
    private void compileReturn() {
        writeStartTag("returnStatement");

        // 'return' keyword
        writeToken("keyword", "return");

        // Check for an optional expression
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == ';') {
            // No expression, just a ';'
            writeToken("symbol", ";");
            writeEndTag("returnStatement");
            return;
        }

        // There is an expression
        tokenizer.pointerBack();
        compileExpression();

        // ';' symbol
        requireSymbol(';');

        writeEndTag("returnStatement");
    }

    /**
     * Compiles an if statement, possibly with a trailing else clause.
     * Format: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
     */
    private void compileIf() {
        writeStartTag("ifStatement");

        // 'if' keyword
        writeToken("keyword", "if");

        // '(' expression ')'
        requireSymbol('(');
        compileExpression();
        requireSymbol(')');

        // '{' statements '}'
        requireSymbol('{');
        writeStartTag("statements");
        compileStatement();
        writeEndTag("statements");
        requireSymbol('}');

        // Check for optional 'else' clause
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.KEYWORD && tokenizer.keyWord() == JackTokenizer.ELSE) {
            writeToken("keyword", "else");

            // '{' statements '}'
            requireSymbol('{');
            writeStartTag("statements");
            compileStatement();
            writeEndTag("statements");
            requireSymbol('}');
        } else {
            tokenizer.pointerBack();
        }

        writeEndTag("ifStatement");
    }

    /**
     * Compiles a single term, which can be one of the following:
     * - integerConstant
     * - stringConstant
     * - keywordConstant (true, false, null, this)
     * - varName
     * - varName '[' expression ']'
     * - subroutineCall
     * - '(' expression ')'
     * - unaryOp term
     *
     * Steps:
     * 1. Start the XML tag for the term.
     * 2. Determine the type of term based on the current token:
     *    - Identifier: Could be varName, varName '[' expression ']', or subroutineCall.
     *    - Integer constant, string constant, or keyword constant.
     *    - Symbol: Could be '(' expression ')' or unary operator with a term.
     * 3. Write the appropriate tokens and recursively handle nested expressions or terms.
     * 4. Close the XML tag for the term.
     *
     * Errors:
     * Throws an error if the token does not match any valid term type.
     */
    private void compileTerm() {
        writeStartTag("term");

        tokenizer.advance();

        if (tokenizer.tokenType() == JackTokenizer.IDENTIFIER) {
            // Handle varName, varName '[' expression ']', or subroutineCall
            String tempId = tokenizer.identifier();

            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == '[') {
                // Array entry: varName '[' expression ']'
                writeToken("identifier", tempId);
                writeToken("symbol", "[");
                compileExpression();
                requireSymbol(']');
            } else if (tokenizer.tokenType() == JackTokenizer.SYMBOL && (tokenizer.symbol() == '(' || tokenizer.symbol() == '.')) {
                // Subroutine call
                tokenizer.pointerBack();
                tokenizer.pointerBack();
                compileSubroutineCall();
            } else {
                // varName
                writeToken("identifier", tempId);
                tokenizer.pointerBack();
            }
        } else {
            // Handle other term types
            switch (tokenizer.tokenType()) {
                case JackTokenizer.INT_CONST -> writeToken("integerConstant", String.valueOf(tokenizer.intVal()));
                case JackTokenizer.STRING_CONST -> writeToken("stringConstant", tokenizer.stringVal());
                case JackTokenizer.KEYWORD -> {
                    if (tokenizer.keyWord() == JackTokenizer.TRUE ||
                            tokenizer.keyWord() == JackTokenizer.FALSE ||
                            tokenizer.keyWord() == JackTokenizer.NULL ||
                            tokenizer.keyWord() == JackTokenizer.THIS) {
                        writeToken("keyword", tokenizer.getCurrentToken());
                    } else {
                        error("Expected keyword constant");
                    }
                }
                case JackTokenizer.SYMBOL -> {
                    if (tokenizer.symbol() == '(') {
                        // '(' expression ')'
                        writeToken("symbol", "(");
                        compileExpression();
                        requireSymbol(')');
                    } else if (tokenizer.symbol() == '-' || tokenizer.symbol() == '~') {
                        // unaryOp term
                        writeToken("symbol", String.valueOf(tokenizer.symbol()));
                        compileTerm();
                    } else {
                        error("Expected '(' expression ')' or unaryOp term");
                    }
                }
                default -> error("Expected term");
            }
        }

        writeEndTag("term");
    }

    /**
     * Compiles a subroutine call, which can be one of the following:
     * - subroutineName '(' expressionList ')'
     * - (className | varName) '.' subroutineName '(' expressionList ')'
     *
     * Steps:
     * 1. Parse the initial identifier, which could be subroutineName, className, or varName.
     * 2. Determine the type of subroutine call based on the next symbol:
     *    - If '(' follows, it's a direct subroutine call with an expression list.
     *    - If '.' follows, it's a method call or a class-based call, requiring another identifier.
     * 3. Handle the corresponding expression list enclosed by '(' and ')'.
     *
     * Errors:
     * Throws an error if the expected token sequence for a valid subroutine call is not found.
     */
    private void compileSubroutineCall() {
        tokenizer.advance();

        // subroutineName | className | varName
        if (tokenizer.tokenType() != JackTokenizer.IDENTIFIER) {
            error("identifier");
        }
        writeToken("identifier", tokenizer.identifier());

        tokenizer.advance();

        if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == '(') {
            // '(' expressionList ')'
            writeToken("symbol", "(");
            writeStartTag("expressionList");
            compileExpressionList();
            writeEndTag("expressionList");
            requireSymbol(')');
        } else if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == '.') {
            // '.' subroutineName '(' expressionList ')'
            writeToken("symbol", ".");
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.IDENTIFIER) {
                error("identifier");
            }
            writeToken("identifier", tokenizer.identifier());
            requireSymbol('(');
            writeStartTag("expressionList");
            compileExpressionList();
            writeEndTag("expressionList");
            requireSymbol(')');
        } else {
            error("'('|'.'");
        }
    }

    /**
     * Compiles an expression.
     * Format: term (op term)*
     */
    private void compileExpression() {
        writeStartTag("expression");

        // Compile the first term
        compileTerm();

        // Compile (op term)*
        while (true) {
            tokenizer.advance();

            // Check if the token is an operator
            if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.isOp()) {
                // Handle special characters for XML
                String symbol = switch (tokenizer.symbol()) {
                    case '>' -> "&gt;";
                    case '<' -> "&lt;";
                    case '&' -> "&amp;";
                    default -> String.valueOf(tokenizer.symbol());
                };
                writeToken("symbol", symbol);

                // Compile the next term
                compileTerm();
            } else {
                tokenizer.pointerBack();
                break;
            }
        }

        writeEndTag("expression");
    }

    /**
     * Compiles a (possibly empty) comma-separated list of expressions.
     * Format: (expression (',' expression)*)?
     */
    private void compileExpressionList() {
        tokenizer.advance();

        // Check if there are no expressions (next token is ')')
        if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == ')') {
            tokenizer.pointerBack();
            return;
        }

        tokenizer.pointerBack();

        // Compile the first expression
        compileExpression();

        // Compile additional expressions separated by ','
        while (true) {
            tokenizer.advance();

            if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == ',') {
                writeToken("symbol", ",");
                compileExpression();
            } else {
                tokenizer.pointerBack();
                break;
            }
        }
    }
    /**
     * Throws an exception to report errors.
     * @param expected The expected token description.
     */
    private void error(String expected) {
        throw new IllegalStateException(
                "Expected token: " + expected + ", but found: " + tokenizer.getCurrentToken()
        );
    }

    /**
     * Requires a specific symbol and writes it to the output if present.
     * Throws an error if the expected symbol is not found.
     * @param expectedSymbol The symbol that must be present.
     */
    private void requireSymbol(char expectedSymbol) {
        tokenizer.advance();

        if (tokenizer.tokenType() == JackTokenizer.SYMBOL && tokenizer.symbol() == expectedSymbol) {
            writeToken("symbol", String.valueOf(expectedSymbol));
        } else {
            error("'" + expectedSymbol + "'");
        }
    }
}