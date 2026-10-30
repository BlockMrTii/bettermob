package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.item.ItemNbt;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HeldItemMechanicTest {
    @Test
    void loreIsAQuotedListOrSplitAtBars() {
        assertEquals(List.of("&DRight-Click to Set", "&DStart Point"), HeldItemMechanic.loreLines("\"&DRight-Click to Set\",\"&DStart Point\""));
        assertEquals(List.of("one", "two"), HeldItemMechanic.loreLines("one|two"));
        assertEquals(List.of(), HeldItemMechanic.loreLines(null));
    }

    @Test
    void nbtKeysAreNamespacedAndCleaned() {
        assertEquals("bettermob:nbt_track_name", ItemNbt.key("Track Name").toString());
        assertNull(ItemNbt.key(" "));
    }

    @Test
    void jsonInsertsAreEscaped() {
        assertEquals("a\\\"b\\\\c\\u000a", JsonMessageMechanic.jsonString("a\"b\\c\n"));
    }
}
