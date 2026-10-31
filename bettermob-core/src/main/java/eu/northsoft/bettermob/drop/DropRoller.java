package eu.northsoft.bettermob.drop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class DropRoller {
    private DropRoller() {}

    public record RolledDrop(String itemKey, int amount) {}
    public record RollResult(List<RolledDrop> items, int exp) {}

    public static RollResult roll(DropTable table) {
        return roll(table, id -> null);
    }

    public static RollResult roll(DropTable table, Function<String, DropTable> tableLookup) {
        List<RolledDrop> drops = new ArrayList<>();
        int exp = rollInternal(table, tableLookup, drops, 0);
        return new RollResult(drops, exp);
    }

    private static int rollInternal(DropTable table, Function<String, DropTable> tableLookup, List<RolledDrop> out, int depth) {
        if (table == null || depth >= 5) return 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int exp = 0;
        List<DropEntry> hit = new ArrayList<>();
        List<DropEntry> missed = new ArrayList<>();

        for (DropEntry entry : table.entries()) {
            boolean passed = random.nextDouble() < entry.chance();
            if (entry.isExp()) {
                if (passed) exp += amount(entry);
            } else {
                (passed ? hit : missed).add(entry);
            }
        }

        Collections.shuffle(hit, random);
        while (hit.size() > table.maxItems()) hit.remove(hit.size() - 1);

        Collections.shuffle(missed, random);
        while (hit.size() < table.minItems() && !missed.isEmpty()) {
            hit.add(missed.remove(missed.size() - 1));
        }

        for (DropEntry entry : hit) {
            int amt = amount(entry);
            DropTable nested = tableLookup != null ? tableLookup.apply(entry.name()) : null;
            if (nested != null) {
                for (int i = 0; i < Math.max(1, amt); i++) {
                    exp += rollInternal(nested, tableLookup, out, depth + 1);
                }
            } else {
                out.add(new RolledDrop(entry.name(), amt));
            }
        }
        return exp;
    }

    private static int amount(DropEntry entry) {
        return entry.min() >= entry.max() ? entry.min() : ThreadLocalRandom.current().nextInt(entry.min(), entry.max() + 1);
    }
}
