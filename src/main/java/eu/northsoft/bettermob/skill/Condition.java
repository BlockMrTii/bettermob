package eu.northsoft.bettermob.skill;

import java.util.Arrays;
import java.util.Locale;

record Condition(String name, String params, Boolean expected, String action, String actionValue) {
    static Condition parse(String raw) {
        String text = raw.trim();
        int i = 0;
        while (i < text.length() && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_' || text.charAt(i) == ':')) i++;
        if (i == 0) return null;
        String name = text.substring(0, i).toLowerCase(Locale.ROOT);
    
        String params = null;
        if (i < text.length() && text.charAt(i) == '{') {
            int depth = 0;
            int j = i;
            for (; j < text.length(); j++) {
                char c = text.charAt(j);
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') {
                    depth--;
                    if (depth == 0) break;
                }
            }
            params = text.substring(i + 1, Math.min(j, text.length()));
            i = j + 1;
        }
    
        Boolean expected = null;
        String action = null;
        String value = null;
        String rest = i < text.length() ? text.substring(i).trim() : "";
        if (!rest.isEmpty()) {
            String[] tokens = rest.split("\\s+");
            int k = 0;
            if (tokens[0].equalsIgnoreCase("true") || tokens[0].equalsIgnoreCase("false")) {
                expected = Boolean.parseBoolean(tokens[0]);
                k = 1;
            }
            if (k < tokens.length) {
                action = tokens[k].toLowerCase(Locale.ROOT);
                if (k + 1 < tokens.length) value = String.join(" ", Arrays.copyOfRange(tokens, k + 1, tokens.length));
            }
        }
        return new Condition(name, params, expected, action, value);
    }
}
