package eu.northsoft.bettermob.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuplicatesTest {
    @Test
    void repeatsOfOneIdAreCountedAndReportedOnceWithTheFirstFolder() {
        Duplicates duplicates = new Duplicates();
        duplicates.add("Reset", "packs/a/skills");
        duplicates.add("reset", "packs/b/skills");
        duplicates.add("RESET", "packs/a/skills");
        duplicates.add("other", "packs/b/skills");
        List<String> reports = new ArrayList<>();
        duplicates.report((id, folder, count) -> reports.add(id + "|" + folder + "|" + count));
        assertEquals(List.of("Reset|packs/a/skills|3", "other|packs/b/skills|1"), reports);
        assertEquals(2, duplicates.size());
    }
}
