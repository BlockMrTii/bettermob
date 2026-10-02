package eu.northsoft.bettermob.drop;

import java.util.List;

public record DropTable(String id, int minItems, int maxItems, List<DropEntry> entries) {
    public static DropTable anonymous(List<DropEntry> entries) {
        return new DropTable("", 0, Integer.MAX_VALUE, entries);
    }
}
