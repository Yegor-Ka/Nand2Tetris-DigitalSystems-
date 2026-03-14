import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class CompilationEngine {

    private VMWriter vmWriter;
    private JackTokenizer tokenizer;
    private SymbolTable symbolTable;
    private String currentClass;
    private String currentSubroutine;
    private int labelIndex;
    /**
     * Constructor for compiler engine
     * @param jackFile jack file to compile
     * @param vmFile fileName.xml
     */
    public CompilationEngine(File jackFile, File vmFile) {

        tokenizer = new JackTokenizer(jackFile);
        vmWriter = new VMWriter(vmFile);
        symbolTable = new SymbolTable();

        labelIndex = 0;
    }

    private String currentFunc(){
        if(currentClass.length() != 0 && currentSubroutine.length() != 0){
                return  currentClass + "." + currentSubroutine;
        }
        return "";
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
    private String compileType() {
        tokenizer.advance();
        JackTokenizer.TYPE type = tokenizer.tokenType();

        // Check for primitive types directly
        if (type == JackTokenizer.TYPE.KEYWORD) {
            JackTokenizer.KEYWORD keyword = tokenizer.keyWord();
            if (keyword == JackTokenizer.KEYWORD.INT ||
                    keyword == JackTokenizer.KEYWORD.CHAR ||
                    keyword == JackTokenizer.KEYWORD.BOOLEAN) {
                return tokenizer.getCurrentToken();
            }
        }

        // Check for class name (identifier)
        if (type == JackTokenizer.TYPE.IDENTIFIER) {
            return tokenizer.identifier();
        }

        // Error handling
        error("Expected 'int', 'char', 'boolean', or a class name.");
        return ""; // Will never reach this line because error() throws an exception
    }

    /**
     * Compiles a complete class.
     * class: 'class' className '{' classVarDec* subroutineDec* '}'
     */
    public void compileClass() {
        tokenizer.advance();

        // 'class' keyword
        if (tokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD || tokenizer.keyWord() != JackTokenizer.KEYWORD.CLASS){
            System.out.println(tokenizer.getCurrentToken());
            error("class");
        }

        // className (identifier)
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
            error("className");
        }
        currentClass = tokenizer.identifier();

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
        vmWriter.close();
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
        // Determine if there is a class variable declaration; next token can be '}' or a subroutine declaration
        tokenizer.advance();

        // If next token is '}', rollback and return (no class variable declaration)
        if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.pointerBack();
            return;
        }

        // If next token is not a keyword, report an error
        if (tokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD) {
            error("Keywords");
        }

        JackTokenizer.KEYWORD keyWord = tokenizer.keyWord();

        // If it's the start of a subroutine declaration, rollback and return
        if (keyWord == JackTokenizer.KEYWORD.CONSTRUCTOR ||
                keyWord == JackTokenizer.KEYWORD.FUNCTION ||
                keyWord == JackTokenizer.KEYWORD.METHOD) {
            tokenizer.pointerBack();
            return;
        }

        // If it's not 'static' or 'field', report an error
        if (keyWord != JackTokenizer.KEYWORD.STATIC && keyWord != JackTokenizer.KEYWORD.FIELD) {
            error("static or field");
        }

        // Determine kind (STATIC or FIELD)
        Symbol.KIND kind = (keyWord == JackTokenizer.KEYWORD.STATIC) ? Symbol.KIND.STATIC : Symbol.KIND.FIELD;

        // Get type
        String type = compileType();

        // Process at least one varName
        while (true) {
            // Expect varName
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
                error("identifier");
            }

            String name = tokenizer.identifier();
            symbolTable.define(name, type, kind);

            // Expect ',' or ';'
            tokenizer.advance();
            char currentSymbol = tokenizer.symbol();
            if (tokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (currentSymbol != ',' && currentSymbol != ';')) {
                error("',' or ';'");
            }

            // If it's ';', break the loop (end of declaration)
            if (currentSymbol == ';') {
                break;
            }
        }

        // Recursively process the next class variable declaration
        compileClassVarDec();
    }



    private void compileSubroutine() {
        // Determine whether there is a subroutine; next can be '}'
        tokenizer.advance();

        // If next token is '}', rollback and return (no subroutine to compile)
        if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.pointerBack();
            return;
        }

        // Ensure subroutine starts correctly with a valid keyword
        if (tokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD ||
                (tokenizer.keyWord() != JackTokenizer.KEYWORD.CONSTRUCTOR &&
                        tokenizer.keyWord() != JackTokenizer.KEYWORD.FUNCTION &&
                        tokenizer.keyWord() != JackTokenizer.KEYWORD.METHOD)) {
            error("constructor|function|method");
        }

        // Store the subroutine type
        JackTokenizer.KEYWORD keyword = tokenizer.keyWord();
        symbolTable.startSubroutine();

        // If it's a method, define "this" as an argument
        if (keyword == JackTokenizer.KEYWORD.METHOD) {
            symbolTable.define("this", currentClass, Symbol.KIND.ARG);
        }

        // Determine return type ('void' or another type)
        tokenizer.advance();
        String type = (tokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && tokenizer.keyWord() == JackTokenizer.KEYWORD.VOID)
                ? "void"
                : compileTypeWithRollback();

        // Ensure the subroutine name is an identifier
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
            error("subroutineName");
        }

        currentSubroutine = tokenizer.identifier();

        requireSymbol('(');
        compileParameterList();
        requireSymbol(')');

        compileSubroutineBody(keyword);

        // Recursively process the next subroutine
        compileSubroutine();
    }

    // Helper method to handle type compilation correctly
    private String compileTypeWithRollback() {
        tokenizer.pointerBack();
        return compileType();
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
    private void compileSubroutineBody(JackTokenizer.KEYWORD keyword){
        //'{'
        requireSymbol('{');
        //varDec*
        compileVarDec();
        //write VM function declaration
        writeFunctionDec(keyword);
        //statements
        compileStatement();
        //'}'
        requireSymbol('}');
    }


    private void writeFunctionDec(JackTokenizer.KEYWORD keyword){

        vmWriter.writeFunction(currentFunc(),symbolTable.varCount(Symbol.KIND.VAR));

        //METHOD and CONSTRUCTOR need to load this pointer
        if (keyword == JackTokenizer.KEYWORD.METHOD){
            //A Jack method with k arguments is compiled into a VM function that operates on k + 1 arguments.
            // The first argument (argument number 0) always refers to the this object.
            vmWriter.writePush(VMWriter.SEGMENT.ARG, 0);
            vmWriter.writePop(VMWriter.SEGMENT.POINTER,0);

        }else if (keyword == JackTokenizer.KEYWORD.CONSTRUCTOR){
            //A Jack function or constructor with k arguments is compiled into a VM function that operates on k arguments.
            vmWriter.writePush(VMWriter.SEGMENT.CONST,symbolTable.varCount(Symbol.KIND.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(VMWriter.SEGMENT.POINTER,0);
        }
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
        // Determine if there is a statement; next can be '}'
        tokenizer.advance();

        // If next token is '}', rollback and return (no more statements to compile)
        if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && tokenizer.symbol() == '}') {
            tokenizer.pointerBack();
            return;
        }

        // Ensure the next token is a valid statement keyword
        if (tokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD) {
            error("keyword");
        }

        // Process the statement based on the keyword type
        switch (tokenizer.keyWord()) {
            case LET -> compileLet();
            case IF -> compileIf();
            case WHILE -> compilesWhile();
            case DO -> compileDo();
            case RETURN -> compileReturn();
            default -> error("'let'|'if'|'while'|'do'|'return'");
        }

        // Recursively process the next statement
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
        // Check if there is a parameter list; if next token is ')', rollback and return
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && tokenizer.symbol() == ')') {
            tokenizer.pointerBack();
            return;
        }

        // There is at least one parameter
        tokenizer.pointerBack();

        while (true) {
            // Parse type
            String type = compileType();

            // Parse varName
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
                error("identifier");
            }

            symbolTable.define(tokenizer.identifier(), type, Symbol.KIND.ARG);

            // Expect ',' or ')'
            tokenizer.advance();
            char currentSymbol = tokenizer.symbol();
            if (tokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (currentSymbol != ',' && currentSymbol != ')')) {
                error("',' or ')'");
            }

            // If ')', rollback and exit
            if (currentSymbol == ')') {
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
        // Determine if there is a variable declaration
        tokenizer.advance();

        // If the next token is not 'var', rollback and return
        if (tokenizer.tokenType() != JackTokenizer.TYPE.KEYWORD || tokenizer.keyWord() != JackTokenizer.KEYWORD.VAR) {
            tokenizer.pointerBack();
            return;
        }

        // Parse type
        String type = compileType();

        while (true) {
            // Parse varName
            tokenizer.advance();
            if (tokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
                error("identifier");
            }

            symbolTable.define(tokenizer.identifier(), type, Symbol.KIND.VAR);

            // Expect ',' or ';'
            tokenizer.advance();
            char currentSymbol = tokenizer.symbol();
            if (tokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (currentSymbol != ',' && currentSymbol != ';')) {
                error("',' or ';'");
            }

            // If ';', end declaration
            if (currentSymbol == ';') {
                break;
            }
        }

        // Recursively process the next variable declaration
        compileVarDec();
    }


    /**
     * Compiles a do statement.
     * Format: 'do' subroutineCall ';'
     */
    private void compileDo() {
        // subroutineCall
        compileSubroutineCall();
        // ';' symbol
        requireSymbol(';');

        vmWriter.writePop(VMWriter.SEGMENT.TEMP, 0);
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
        // Parse varName
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
            error("varName");
        }

        String varName = tokenizer.identifier();

        // Expect '[' or '='
        tokenizer.advance();
        char currentSymbol = tokenizer.symbol();
        if (tokenizer.tokenType() != JackTokenizer.TYPE.SYMBOL || (currentSymbol != '[' && currentSymbol != '=')) {
            error("'['|'='");
        }

        boolean isArray = (currentSymbol == '[');

        // Handle array assignment if '[' is found
        if (isArray) {
            // Push base address onto the stack
            vmWriter.writePush(getSegment(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));

            // Compute offset
            compileExpression();
            requireSymbol(']');

            // Compute base + offset
            vmWriter.writeArithmetic(VMWriter.COMMAND.ADD);
            tokenizer.advance();
        }

        // Parse expression
        compileExpression();
        requireSymbol(';');

        // Store result in variable or array
        if (isArray) {
            // *(base + offset) = expression
            vmWriter.writePop(VMWriter.SEGMENT.TEMP, 0);
            vmWriter.writePop(VMWriter.SEGMENT.POINTER, 1);
            vmWriter.writePush(VMWriter.SEGMENT.TEMP, 0);
            vmWriter.writePop(VMWriter.SEGMENT.THAT, 0);
        } else {
            // Direct assignment
            vmWriter.writePop(getSegment(symbolTable.kindOf(varName)), symbolTable.indexOf(varName));
        }
    }


    /**
     * returns Segment
     * @param kind
     */
    private VMWriter.SEGMENT getSegment(Symbol.KIND kind) {
        return switch (kind) {
            case VAR -> VMWriter.SEGMENT.LOCAL;
            case ARG -> VMWriter.SEGMENT.ARG;
            case STATIC -> VMWriter.SEGMENT.STATIC;
            case FIELD -> VMWriter.SEGMENT.THIS;
            default -> VMWriter.SEGMENT.NONE;
        };
    }

    private String mLabel(){
        return "LABEL_" + (labelIndex++);
    }


    /**
     * Compiles a while statement.
     * Format: 'while' '(' expression ')' '{' statements '}'l
     */
    private void compilesWhile() {
        // Generate labels for while loop
        String continueLabel = mLabel();
        String topLabel = mLabel();

        // Top label for the while loop
        vmWriter.writeLabel(topLabel);

        // Parse '(' and condition expression
        requireSymbol('(');
        compileExpression();
        requireSymbol(')');

        // If ~(condition), jump to continue label (exit loop)
        vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);
        vmWriter.writeIf(continueLabel);

        // Parse '{' and loop body
        requireSymbol('{');
        compileStatement();
        requireSymbol('}');

        // Jump back to the top of the loop
        vmWriter.writeGoto(topLabel);

        // Continue label (exit point of the loop)
        vmWriter.writeLabel(continueLabel);
    }


    /**
     * Compiles a return statement.
     * Format: 'return' expression? ';'
     */
    private void compileReturn() {

        // Check for an optional expression
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && tokenizer.symbol() == ';') {
            // No expression, just a ';'
            vmWriter.writePush(VMWriter.SEGMENT.CONST, 0);
        }else {

            // There is an expression
            tokenizer.pointerBack();
            compileExpression();

            // ';' symbol
            requireSymbol(';');
        }
        vmWriter.writeReturn();
    }

    /**
     * Compiles an if statement, possibly with a trailing else clause.
     * Format: 'if' '(' expression ')' '{' statements '}' ('else' '{' statements '}')?
     */
    private void compileIf() {
        // Generate labels for else and end of if block
        String elseLabel = mLabel();
        String endLabel = mLabel();

        // Parse '(' and condition expression
        requireSymbol('(');
        compileExpression();
        requireSymbol(')');

        // If ~(condition), jump to else label
        vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);
        vmWriter.writeIf(elseLabel);

        // Parse '{' and execute statements inside the if-block
        requireSymbol('{');
        compileStatement();
        requireSymbol('}');

        // Jump to end label after if-block execution
        vmWriter.writeGoto(endLabel);

        // Else label (executes if the condition was false)
        vmWriter.writeLabel(elseLabel);

        // Check if there is an 'else' block
        tokenizer.advance();
        if (tokenizer.tokenType() == JackTokenizer.TYPE.KEYWORD && tokenizer.keyWord() == JackTokenizer.KEYWORD.ELSE) {
            requireSymbol('{');
            compileStatement();
            requireSymbol('}');
        } else {
            tokenizer.pointerBack();
        }

        // End label (exit point of the if-else structure)
        vmWriter.writeLabel(endLabel);
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
        tokenizer.advance();

        if (tokenizer.tokenType() == JackTokenizer.TYPE.IDENTIFIER) {
            // varName | varName '[' expression ']' | subroutineCall
            String tempId = tokenizer.identifier();
            tokenizer.advance();

            if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL) {
                char currentSymbol = tokenizer.symbol();

                if (currentSymbol == '[') {
                    // Array entry: varName '[' expression ']'
                    vmWriter.writePush(getSegment(symbolTable.kindOf(tempId)), symbolTable.indexOf(tempId));

                    compileExpression();
                    requireSymbol(']');

                    // Compute base + offset
                    vmWriter.writeArithmetic(VMWriter.COMMAND.ADD);

                    // Pop into 'that' pointer and push *(base+index) onto stack
                    vmWriter.writePop(VMWriter.SEGMENT.POINTER, 1);
                    vmWriter.writePush(VMWriter.SEGMENT.THAT, 0);
                } else if (currentSymbol == '(' || currentSymbol == '.') {
                    // Subroutine call
                    tokenizer.pointerBack();
                    tokenizer.pointerBack();
                    compileSubroutineCall();
                } else {
                    tokenizer.pointerBack();
                    vmWriter.writePush(getSegment(symbolTable.kindOf(tempId)), symbolTable.indexOf(tempId));
                }
            } else {
                tokenizer.pointerBack();
                vmWriter.writePush(getSegment(symbolTable.kindOf(tempId)), symbolTable.indexOf(tempId));
            }
        } else {
            // Handle integerConstant, stringConstant, keywordConstant, '(' expression ')', or unaryOp term
            switch (tokenizer.tokenType()) {
                case INT_CONST -> vmWriter.writePush(VMWriter.SEGMENT.CONST, tokenizer.intVal());

                case STRING_CONST -> {
                    // Create a new string and append every character to it
                    String str = tokenizer.stringVal();
                    vmWriter.writePush(VMWriter.SEGMENT.CONST, str.length());
                    vmWriter.writeCall("String.new", 1);

                    for (char c : str.toCharArray()) {
                        vmWriter.writePush(VMWriter.SEGMENT.CONST, (int) c);
                        vmWriter.writeCall("String.appendChar", 2);
                    }
                }

                case KEYWORD -> {
                    JackTokenizer.KEYWORD keyword = tokenizer.keyWord();
                    switch (keyword) {
                        case TRUE -> {
                            vmWriter.writePush(VMWriter.SEGMENT.CONST, 0);
                            vmWriter.writeArithmetic(VMWriter.COMMAND.NOT);
                        }
                        case FALSE, NULL -> vmWriter.writePush(VMWriter.SEGMENT.CONST, 0);
                        case THIS -> vmWriter.writePush(VMWriter.SEGMENT.POINTER, 0);
                        default -> error("integerConstant|stringConstant|keywordConstant|'(' expression ')'|unaryOp term");
                    }
                }

                case SYMBOL -> {
                    char symbol = tokenizer.symbol();
                    switch (symbol) {
                        case '(' -> {
                            compileExpression();
                            requireSymbol(')');
                        }
                        case '-', '~' -> {
                            compileTerm();
                            vmWriter.writeArithmetic(symbol == '-' ? VMWriter.COMMAND.NEG : VMWriter.COMMAND.NOT);
                        }
                        default -> error("integerConstant|stringConstant|keywordConstant|'(' expression ')'|unaryOp term");
                    }
                }

                default -> error("integerConstant|stringConstant|keywordConstant|'(' expression ')'|unaryOp term");
            }
        }
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
        // Parse initial identifier (className, varName, or subroutineName)
        tokenizer.advance();
        if (tokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
            error("identifier");
        }

        String name = tokenizer.identifier();
        int nArgs = 0;

        // Check for '(' or '.'
        tokenizer.advance();
        char currentSymbol = tokenizer.symbol();

        if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL) {
            if (currentSymbol == '(') {
                // Method call: push 'this' pointer
                vmWriter.writePush(VMWriter.SEGMENT.POINTER, 0);

                // Parse expression list
                nArgs = compileExpressionList() + 1;

                requireSymbol(')');
                vmWriter.writeCall(currentClass + '.' + name, nArgs);
            } else if (currentSymbol == '.') {
                // (className | varName) '.' subroutineName '(' expressionList ')'
                String objName = name;

                // Parse subroutineName
                tokenizer.advance();
                if (tokenizer.tokenType() != JackTokenizer.TYPE.IDENTIFIER) {
                    error("identifier");
                }

                name = tokenizer.identifier();
                String type = symbolTable.typeOf(objName);

                if (type.equals("int") || type.equals("boolean") || type.equals("char") || type.equals("void")) {
                    error("no built-in type");
                } else if (type.isEmpty()) {
                    name = objName + "." + name;
                } else {
                    nArgs = 1;
                    // Push variable directly onto stack
                    vmWriter.writePush(getSegment(symbolTable.kindOf(objName)), symbolTable.indexOf(objName));
                    name = type + "." + name;
                }

                requireSymbol('(');
                nArgs += compileExpressionList();
                requireSymbol(')');
                vmWriter.writeCall(name, nArgs);
            } else {
                error("'('|'.'");
            }
        } else {
            error("'('|'.'");
        }
    }


    /**
     * Compiles an expression.
     * Format: term (op term)*
     */
    private void compileExpression() {
        // Parse the first term
        compileTerm();

        // Process (op term)*
        while (true) {
            tokenizer.advance();

            // Check if the token is a valid operator
            if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && tokenizer.isOp()) {
                String opCmd = switch (tokenizer.symbol()) {
                    case '+' -> "add";
                    case '-' -> "sub";
                    case '*' -> "call Math.multiply 2";
                    case '/' -> "call Math.divide 2";
                    case '<' -> "lt";
                    case '>' -> "gt";
                    case '=' -> "eq";
                    case '&' -> "and";
                    case '|' -> "or";
                    default -> throw new IllegalStateException("Unknown operator: " + tokenizer.symbol());
                };

                // Parse the next term
                compileTerm();

                // Write the corresponding VM command
                vmWriter.writeCommand(opCmd);
            } else {
                tokenizer.pointerBack();
                break;
            }
        }
    }


    /**
     * changed to int
     * Compiles a (possibly empty) comma-separated list of expressions.
     * Format: (expression (',' expression)*)?
     */
    private int compileExpressionList() {
        int nArgs = 0;

        tokenizer.advance();

        // Check if there are any expressions; if next token is ')', rollback and return 0
        if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && tokenizer.symbol() == ')') {
            tokenizer.pointerBack();
            return nArgs;
        }

        // At least one expression exists
        nArgs = 1;
        tokenizer.pointerBack();
        compileExpression();

        // Process (',' expression)*
        while (true) {
            tokenizer.advance();
            if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && tokenizer.symbol() == ',') {
                compileExpression();
                nArgs++;
            } else {
                tokenizer.pointerBack();
                break;
            }
        }

        return nArgs;
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

        if (tokenizer.tokenType() == JackTokenizer.TYPE.SYMBOL && tokenizer.symbol() == expectedSymbol) {
        } else {
            error("'" + expectedSymbol + "'");
        }
    }
}