import java.util.Hashtable;

public class Code {
    private Hashtable<String, String> destMemo;
    private Hashtable<String, String> compMemo;
    private Hashtable<String, String> jumpMemo;

    public Code() {

    }


    /**
     * Returns the binary representation of the `dest` mnemonic.
     *
     * @param dest the destination field (e.g., "M", "D", "MD").
     * @return The binary string representation (e.g., "001", "011").
     */
    public static String destMemo(String dest) {
        if (dest == null || dest.isEmpty()) {
            return "000";
        }
        return switch (dest){
            case "M" -> "001";
            case "D" -> "010";
            case "MD" -> "011";
            case "A" -> "100";
            case "AM" -> "101";
            case "AD" -> "110";
            case "AMD" -> "111";
            default -> throw new IllegalArgumentException("Invalid destination: " + dest);
        };
    }


    /**
     * Returns the binary representation of the `comp` mnemonic.
     *
     * @param comp the computation field (e.g., "D+1", "M").
     * @return The binary string representation (e.g., "0111111", "1110000").
     */
    public static String compMemo(String comp) {
        return switch (comp){
            case "0" -> "0101010";
            case "1" -> "0111111";
            case "-1" -> "0111010";
            case "D" -> "0001100";
            case "A" -> "0110000";
            case "M" -> "1110000";
            case "!D" -> "0001101";
            case "!A" -> "0110001";
            case "!M" -> "1110001";
            case "-D" -> "0001111";
            case "-A" -> "0110011";
            case "-M" -> "1110011";
            case "D+1" -> "0011111";
            case "A+1" -> "0110111";
            case "M+1" -> "1110111";
            case "D-1" -> "0001110";
            case "A-1" -> "0110010";
            case "M-1" -> "1110010";
            case "D+A" -> "0000010";
            case "D+M" -> "1000010";
            case "D-A" -> "0010011";
            case "D-M" -> "1010011";
            case "A-D" -> "0000111";
            case "M-D" -> "1000111";
            case "D&A" -> "0000000";
            case "D&M" -> "1000000";
            case "D|A" -> "0010101";
            case "D|M" -> "1010101";
            default -> throw new IllegalArgumentException("Invalid comp mnemonic" + comp);
        };
    }


    /**
     * Returns the binary representation of the `jump` mnemonic.
     *
     * @param jump the jump field (e.g., "JGT", "JLE").
     * @return The binary string representation (e.g., "001", "110").
     */
    public static String jumpMemo(String jump) {
        if(jump == null || jump.isEmpty()){
            return "000";
        }
        return switch (jump){
          case "JGT" -> "001";
          case "JEQ" -> "010";
          case "JGE" -> "011";
          case "JLT" -> "100";
          case "JNE" -> "101";
          case "JLE" -> "110";
          case "JMP" -> "111";
          default -> "000";
        };
    }


    /**
     * Formats a number as a 15-bit, zero-padded binary string.
     *
     * @param number the numeric string to convert (e.g., "16").
     * @return A 15-bit binary string representation (e.g., "0000000000010000").
     * @throws NumberFormatException if the input is not a valid number.
     */
    public String formatNumberAsBinary(String number) {
        int value = Integer.parseInt(number);
        String binaryNum = Integer.toBinaryString(value);
        // Pad to exactly 15 bits
        if (binaryNum.length() > 15) {
            throw new IllegalArgumentException("Number exceeds 15 bits: " + number);
        }
        return String.format("%15s", binaryNum).replace(' ', '0');
    }

}
