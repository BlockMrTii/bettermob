package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.model.ModelParts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelEffectMechanicTest {
    @Test
    void colorsAreHexOrRgbAndFallBackWhenInvalid() {
        assertEquals(0xFBFF00, ModelEffectMechanic.parseColor("fbff00", 1));
        assertEquals(0x87FF00, ModelEffectMechanic.parseColor("\"#87ff00\"", 1));
        assertEquals(0xFF0080, ModelEffectMechanic.parseColor("255,0,128", 1));
        assertEquals(0xFF0000, ModelEffectMechanic.parseColor("300,-1,0", 1));
        assertEquals(1, ModelEffectMechanic.parseColor("red", 1));
        assertEquals(1, ModelEffectMechanic.parseColor(null, 1));
    }

    @Test
    void partsMatchExactlyOrByContainedName() {
        assertTrue(ModelParts.matches("spark", "Spark", true));
        assertFalse(ModelParts.matches("spark_left", "spark", true));
        assertTrue(ModelParts.matches("spark_left", "spark", false));
        assertTrue(ModelParts.matches("anything", null, true));
        assertFalse(ModelParts.matches(null, "spark", false));
    }
}
