package eu.northsoft.bettermob.drop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropRollerTest {

    @Test
    void rollsGuaranteedItemsAndExp() {
        DropEntry item1 = new DropEntry("minecraft:diamond", 2, 2, 1.0);
        DropEntry item2 = new DropEntry("minecraft:iron_ingot", 5, 5, 1.0);
        DropEntry exp = new DropEntry("exp", 10, 10, 1.0);

        DropTable table = new DropTable("loot", 0, 10, List.of(item1, item2, exp));
        DropRoller.RollResult result = DropRoller.roll(table);

        assertEquals(2, result.items().size());
        assertEquals(10, result.exp());
        assertTrue(result.items().stream().anyMatch(d -> d.itemKey().equals("minecraft:diamond") && d.amount() == 2));
        assertTrue(result.items().stream().anyMatch(d -> d.itemKey().equals("minecraft:iron_ingot") && d.amount() == 5));
    }

    @Test
    void respectsMaxItemsConstraint() {
        DropEntry item1 = new DropEntry("item1", 1, 1, 1.0);
        DropEntry item2 = new DropEntry("item2", 1, 1, 1.0);
        DropEntry item3 = new DropEntry("item3", 1, 1, 1.0);

        DropTable table = new DropTable("limited", 0, 1, List.of(item1, item2, item3));
        DropRoller.RollResult result = DropRoller.roll(table);

        assertEquals(1, result.items().size());
    }

    @Test
    void respectsMinItemsConstraint() {
        DropEntry item1 = new DropEntry("rare1", 1, 1, 0.0);
        DropEntry item2 = new DropEntry("rare2", 1, 1, 0.0);

        DropTable table = new DropTable("guaranteed_min", 1, 2, List.of(item1, item2));
        DropRoller.RollResult result = DropRoller.roll(table);

        assertEquals(1, result.items().size());
    }
}
