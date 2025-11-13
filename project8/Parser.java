import java.util.Scanner;

/**
 * The Parser class reads and processes VM commands, determining their type
 * and extracting arguments as needed for translation to Hack assembly.
 */
public class Parser {
    private final Scanner scanner;
    private String currentCommand;

    /**
     * Constructs a Parser instance to process VM commands from the given input.
     *
     * @param scanner a {@link Scanner} object initialized with the input VM code
     */
    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Checks if there are more lines to process in the input.
     *
     * @return {@code true} if there are more lines to process; {@code false} otherwise
     */
    public boolean hasMoreLines() {
        return scanner.hasNextLine();
    }

    /**
     * Advances to the next command in the input.
     *
     * <p>Skips over empty lines and comments. If a valid command is found, it is stored
     * in {@code currentCommand}. This method assumes the input follows the VM command
     * syntax, including comments (starting with `//`) and valid command structure.</p>
     */
    public void advance() {
        while (hasMoreLines()) {
            currentCommand = scanner.nextLine();

            // Remove comments
            int commentIndex = currentCommand.indexOf('/');
            if (commentIndex >= 0) {
                currentCommand = currentCommand.substring(0, commentIndex);
            }

            // Trim the command
            currentCommand = currentCommand.trim();

            // Skip empty commands
            if (!currentCommand.isEmpty()) {
                break;
            }
        }
    }

    /**
     * Determines the type of the current VM command.
     *
     * <p>The command type is one of:
     * <ul>
     *   <li>{@link CommandType#C_PUSH} for "push" commands</li>
     *   <li>{@link CommandType#C_POP} for "pop" commands</li>
     *   <li>{@link CommandType#C_LABEL} for "label" commands</li>
     *   <li>{@link CommandType#C_GOTO} for "goto" commands</li>
     *   <li>{@link CommandType#C_IF} for "if-goto" commands</li>
     *   <li>{@link CommandType#C_FUNCTION} for "function" commands</li>
     *   <li>{@link CommandType#C_CALL} for "call" commands</li>
     *   <li>{@link CommandType#C_RETURN} for "return" commands</li>
     *   <li>{@link CommandType#C_ARITHMETIC} for all arithmetic and logical commands
     *       (e.g., "add", "sub", "eq")</li>
     * </ul>
     *
     * @return the {@link CommandType} of the current command
     */
    public CommandType commandType() {
        String command = currentCommand.split(" ")[0];
        return switch (command) {
            case "push" -> CommandType.C_PUSH;
            case "pop" -> CommandType.C_POP;
            case "label" -> CommandType.C_LABEL;
            case "goto" -> CommandType.C_GOTO;
            case "if-goto" -> CommandType.C_IF;
            case "function" -> CommandType.C_FUNCTION;
            case "call" -> CommandType.C_CALL;
            case "return" -> CommandType.C_RETURN;
            default -> CommandType.C_ARITHMETIC;
        };
    }

    /**
     * Extracts the first argument of the current command.
     *
     * <p>The argument extracted depends on the command type:
     * <ul>
     *   <li>For arithmetic commands, returns the command itself (e.g., "add", "sub").</li>
     *   <li>For other commands, returns the first argument (e.g., the segment for "push").</li>
     *   <li>For {@code return} commands, returns {@code null} as they have no arguments.</li>
     * </ul>
     *
     * @return the first argument of the current command, or {@code null} for "return" commands
     */
    public String arg1() {
        if(commandType() == CommandType.C_RETURN) {
            return null;
        }
        return commandType() == CommandType.C_ARITHMETIC
                ? currentCommand
                : currentCommand.split(" ")[1];
    }

    /**
     * Extracts the second argument of the current command, if applicable.
     *
     * <p>This method is valid for commands that have two arguments, such as:
     * <ul>
     *   <li>{@code push} and {@code pop}: the second argument is the index.</li>
     *   <li>{@code function} and {@code call}: the second argument is the number of variables or arguments.</li>
     * </ul>
     * Throws an exception if called for a command that does not have a second argument.
     *
     * @return the second argument of the current command
     * @throws UnsupportedOperationException if the current command type does not support a second argument
     */
    public int arg2() {
        if (commandType() == CommandType.C_PUSH ||
                commandType() == CommandType.C_POP ||
                commandType() == CommandType.C_FUNCTION ||
                commandType() == CommandType.C_CALL){
            return Integer.parseInt(currentCommand.split(" ")[2]);
        }
        throw new UnsupportedOperationException("arg2 is not available for this command type.");
    }
}
