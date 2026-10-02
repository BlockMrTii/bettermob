package eu.northsoft.bettermob;

import java.util.Locale;

/**
 * Eine Drop-Zeile: "name menge chance", z.B. "BONE 2-4 40%", "EXP 8-20 100%" oder "nm_bison_drops".
 * Menge ist eine Zahl oder ein Bereich, Chance "80%" oder 0.8 - beides ist optional (1 Stueck, 100%).
 */
record DropEntry(String name, int min, int max, double chance) {

    static DropEntry parse(String line) {
        String[] tokens = line.trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty()) return null;
        int[] amount = {1, 1};
        double chance = 1.0;
        try {
            if (tokens.length >= 3) {
                amount = parseAmount(tokens[1]);
                chance = parseChance(tokens[2]);
            } else if (tokens.length == 2) {
                String token = tokens[1];
                // Eine einzelne Zahl ist die Menge - ausser sie sieht nach Chance aus (80% oder 0.25).
                if (token.endsWith("%") || (token.contains(".") && Double.parseDouble(token) < 1)) chance = parseChance(token);
                else amount = parseAmount(token);
            }
        } catch (NumberFormatException exception) {
            return null;
        }
        return new DropEntry(tokens[0], Math.min(amount[0], amount[1]), Math.max(amount[0], amount[1]), chance);
    }

    private static int[] parseAmount(String rawToken) {
        // MythicMobs schreibt Bereiche auch als "1to2".
        String token = rawToken.replaceFirst("(?<=\\d)to(?=\\d)", "-");
        int dash = token.indexOf('-', 1);
        if (dash < 0) {
            int value = (int) Math.round(Double.parseDouble(token));
            return new int[]{value, value};
        }
        return new int[]{Integer.parseInt(token.substring(0, dash)), Integer.parseInt(token.substring(dash + 1))};
    }

    private static double parseChance(String token) {
        double value = token.endsWith("%") ? Double.parseDouble(token.substring(0, token.length() - 1)) / 100 : Double.parseDouble(token);
        return Math.max(0, Math.min(1, value));
    }

    boolean isExp() {
        String key = name.toLowerCase(Locale.ROOT);
        return key.equals("exp") || key.equals("experience") || key.equals("xp");
    }
}
