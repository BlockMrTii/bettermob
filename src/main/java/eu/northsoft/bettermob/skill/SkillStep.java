package eu.northsoft.bettermob.skill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public sealed interface SkillStep {
    record Delay(int ticks) implements SkillStep {}

    record Mechanic(String name, Map<String, String> params, String targeter, Map<String, String> targeterParams,
                     String inlineCondition, boolean negated) implements SkillStep {}

    static SkillStep parse(String line) {
        String trimmed = line.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("delay ")) {
            try {
                return new Delay(Integer.parseInt(trimmed.substring(6).trim()));
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        Cursor cursor = new Cursor(trimmed);
        String name = cursor.readWord();
        if (name.isEmpty()) return null;
        Map<String, String> params = cursor.peek() == '{' ? parseParams(cursor.readBraced()) : Map.of();

        cursor.skipWhitespace();

        String targeter = "";
        Map<String, String> targeterParams = Map.<String, String>of();
        if (cursor.peek() == '@') {
            cursor.advance();
            targeter = cursor.readWord().toLowerCase(Locale.ROOT);
            if (cursor.peek() == '{') targeterParams = parseParams(cursor.readBraced());
        }

        cursor.skipWhitespace();
        String inlineCondition = null;
        boolean negated = false;
        if (cursor.peek() == '?') {
            cursor.advance();
            if (cursor.peek() == '!') {
                negated = true;
                cursor.advance();
            }
            StringBuilder raw = new StringBuilder(cursor.readWord());
            if (cursor.peek() == '{') raw.append('{').append(cursor.readBraced()).append('}');
            inlineCondition = raw.toString();
        }

        return new Mechanic(name.toLowerCase(Locale.ROOT), params, targeter, targeterParams, inlineCondition, negated);
    }

    static Map<String, String> parseParams(String raw) {
        Map<String, String> params = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return params;
        List<String> pairs = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
            else if (c == ';' && depth == 0) {
                pairs.add(raw.substring(start, i));
                start = i + 1;
            }
        }
        pairs.add(raw.substring(start));
        for (String pair : pairs) {
            int index = pair.indexOf('=');
            if (index < 0) continue;
            params.put(pair.substring(0, index).trim().toLowerCase(Locale.ROOT), pair.substring(index + 1).trim());
        }
        return params;
    }

    final class Cursor {
        private final String s;
        private int pos;

        public Cursor(String s) {
            this.s = s;
        }

        public char peek() {
            return pos < s.length() ? s.charAt(pos) : '\0';
        }

        public void advance() {
            pos++;
        }

        public void skipWhitespace() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        public String readWord() {
            int start = pos;
            while (pos < s.length() && (Character.isLetterOrDigit(s.charAt(pos)) || s.charAt(pos) == '_' || s.charAt(pos) == ':')) pos++;
            return s.substring(start, pos);
        }

        public String readBraced() {
            int start = pos + 1;
            int depth = 0;
            int i = pos;
            for (; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') {
                    depth--;
                    if (depth == 0) break;
                }
            }
            String inner = s.substring(start, Math.min(i, s.length()));
            pos = i + 1;
            return inner;
        }
    }
}
