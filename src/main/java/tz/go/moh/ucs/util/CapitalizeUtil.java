package tz.go.moh.ucs.util;

public class CapitalizeUtil {

    /**
     * Removes all double quotes from the input string, then capitalizes the first letter of each word.
     *
     * @param input the string to be processed, e.g. "Kombo ""A""" - Nkoaranga
     * @return a new string formatted as "Kombo A - Nkoaranga"
     */
    public static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Remove all double quotes from the input string.
        input = input.replace("\"", "");

        // Split the string into words based on whitespace.
        String[] words = input.split("\\s+");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (!word.isEmpty()) {
                if (word.length() <= 2) {
                    formatted.append(word.toUpperCase()).append(" ");
                } else {
                    String capWord = word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
                    formatted.append(capWord).append(" ");
                }
            }
        }

        // Remove the trailing space and return.
        return formatted.toString().trim();
    }
}

