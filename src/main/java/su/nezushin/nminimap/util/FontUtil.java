package su.nezushin.nminimap.util;

public class FontUtil {

    public static int unicodeEscapeSequenceToInt(String sequence) {
        return Integer.parseInt(sequence.substring(2), 16);
    }

    public static String unicodeToEscapeSequence(String unicodeStr){
        StringBuilder str = new StringBuilder();
        for(var i : unicodeStr.toCharArray())
            str.append(charToUnicodeEscapeSequence(i));
        return str.toString();
    }


    public static String intToUnicodeEscapeSequence(int codepoint) {
        return String.format("\\u%04X", codepoint);
    }

    public static String charToUnicodeEscapeSequence(char c) {
        return intToUnicodeEscapeSequence(c);
    }
}
