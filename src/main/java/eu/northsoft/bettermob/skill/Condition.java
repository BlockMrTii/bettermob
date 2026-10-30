package eu.northsoft.bettermob.skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public record Condition(String name, String params, Boolean expected, String action, String actionValue) {
    private static final int CACHE_LIMIT = 4096;
    private static final Condition UNPARSEABLE = new Condition("", null, null, null, null);
    public static final String COMPOSITE = "(composite)";

    public boolean composite() {
        return COMPOSITE.equals(name);
    }

    public static List<List<String>> alternatives(String expression) {
        List<List<String>> any = new ArrayList<>();
        List<String> all = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '{' || c == '[' || c == '(') depth++;
            else if (c == '}' || c == ']' || c == ')') depth--;
            else if (depth == 0 && i + 1 < expression.length() && (c == '|' || c == '&') && expression.charAt(i + 1) == c) {
                all.add(expression.substring(start, i).trim());
                if (c == '|') {
                    any.add(all);
                    all = new ArrayList<>();
                }
                start = i + 2;
                i++;
            }
        }
        all.add(expression.substring(start).trim());
        any.add(all);
        for (List<String> group : any) {
            for (String atom : group) if (atom.isEmpty() || parse(atom) == null) return null;
        }
        return any;
    }

    public boolean mentions(String conditionName) {
        if (!composite()) return name.equals(conditionName);
        List<List<String>> any = alternatives(params);
        if (any == null) return false;
        for (List<String> all : any) {
            for (String atom : all) if (parse(atom).mentions(conditionName)) return true;
        }
        return false;
    }

    public static boolean holds(String expression, Predicate<Condition> met) {
        List<List<String>> any = alternatives(expression);
        if (any == null) return true;
        for (List<String> all : any) {
            boolean ok = true;
            for (String atom : all) {
                if (!met.test(parse(atom))) {
                    ok = false;
                    break;
                }
            }
            if (ok) return true;
        }
        return false;
    }
    private static final Map<String, Condition> CACHE = new ConcurrentHashMap<>();

    public static Condition parse(String raw) {
        Condition cached = CACHE.get(raw);
        if (cached == null) {
            Condition parsed = parseUncached(raw);
            if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();
            CACHE.put(raw, parsed == null ? UNPARSEABLE : parsed);
            return parsed;
        }
        return cached == UNPARSEABLE ? null : cached;
    }

    private static Condition parseUncached(String raw) {
        String text = raw.trim();
        if (text.startsWith("(")) return parseComposite(text);
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
        return withTail(name, params, i < text.length() ? text.substring(i).trim() : "");
    }

    private static Condition parseComposite(String text) {
        int depth = 0;
        int end = -1;
        for (int j = 0; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c == '{' || c == '[' || c == '(') depth++;
            else if (c == '}' || c == ']' || c == ')') {
                depth--;
                if (depth == 0 && c == ')') {
                    end = j;
                    break;
                }
            }
        }
        if (end < 0) return null;
        String rest = text.substring(end + 1).trim();
        if (rest.startsWith("&&") || rest.startsWith("||")) return alternatives(text) == null ? null : new Condition(COMPOSITE, text, null, null, null);
        String inner = text.substring(1, end).trim();
        if (inner.isEmpty() || alternatives(inner) == null) return null;
        return withTail(COMPOSITE, inner, text.substring(end + 1).trim());
    }

    private static Condition withTail(String name, String params, String rest) {
        Boolean expected = null;
        String action = null;
        String value = null;
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
