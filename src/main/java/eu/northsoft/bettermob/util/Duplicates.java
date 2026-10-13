package eu.northsoft.bettermob.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class Duplicates {
    public interface Reporter {
        void report(String id, String folder, int count);
    }

    private record Seen(String id, String folder, int count) {}

    private final Map<String, Seen> seen = new LinkedHashMap<>();

    public void add(String id, String folder) {
        seen.merge(id.toLowerCase(Locale.ROOT), new Seen(id, folder, 1), (first, again) -> new Seen(first.id(), first.folder(), first.count() + 1));
    }

    public int size() {
        return seen.size();
    }

    public void report(Reporter reporter) {
        for (Seen entry : seen.values()) reporter.report(entry.id(), entry.folder(), entry.count());
    }
}
