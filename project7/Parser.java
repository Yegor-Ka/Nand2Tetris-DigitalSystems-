import java.util.Scanner;

public class Parser {
    private final Scanner scanner;
    private String currentCommand;

    public Parser(Scanner scanner) {
        this.scanner = scanner;
    }

    public boolean hasMoreLines() {
        return scanner.hasNextLine();
    }

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

    public CommandType commandType() {
        String command = currentCommand.split(" ")[0];
        return switch (command) {
            case "push" -> CommandType.C_PUSH;
            case "pop" -> CommandType.C_POP;
            default -> CommandType.C_ARITHMETIC;
        };
    }

    public String arg1() {
        if(commandType() == CommandType.C_RETURN) {
            return null;
        }
        return commandType() == CommandType.C_ARITHMETIC
                ? currentCommand
                : currentCommand.split(" ")[1];
    }

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
