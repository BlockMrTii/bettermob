package eu.northsoft.bettermob.item;

import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentSupportTest {
    @Test
    void slotNamesAndAliases() {
        assertEquals(EquipmentSlot.HEAD, EquipmentSupport.slotFor("HELMET"));
        assertEquals(EquipmentSlot.OFF_HAND, EquipmentSupport.slotFor("offhand"));
        assertEquals(EquipmentSlot.FEET, EquipmentSupport.slotFor(" boots "));
        assertEquals(EquipmentSlot.HAND, EquipmentSupport.slotFor("hand"));
    }

    @Test
    void unknownSlotsAreRecognised() {
        assertTrue(EquipmentSupport.isKnownSlot("Legs"));
        assertFalse(EquipmentSupport.isKnownSlot("tail"));
    }
}
