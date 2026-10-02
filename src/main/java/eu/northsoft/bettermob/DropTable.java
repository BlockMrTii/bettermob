package eu.northsoft.bettermob;

import java.util.List;

record DropTable(String id, int minItems, int maxItems, List<DropEntry> entries) {
    static DropTable anonymous(List<DropEntry> entries) {
        return new DropTable("", 0, Integer.MAX_VALUE, entries);
    }
}
