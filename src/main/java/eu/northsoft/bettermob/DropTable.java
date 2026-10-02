package eu.northsoft.bettermob;

import java.util.List;

/** Eine Drop-Table aus droptables/*.yml: Drop-Zeilen plus MinItems/MaxItems (Anzahl der Item-Eintraege). */
record DropTable(String id, int minItems, int maxItems, List<DropEntry> entries) {
    /** Name der Tabelle gibt es nur bei benannten Tabellen - die Drops: Liste eines Mobs ist eine namenlose ohne Grenzen. */
    static DropTable anonymous(List<DropEntry> entries) {
        return new DropTable("", 0, Integer.MAX_VALUE, entries);
    }
}
