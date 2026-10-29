package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Particle;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParticleDataTest {
    @Test
    void legacyNamesMapToCurrentParticles() {
        assertEquals(Particle.BLOCK, ParticleSupport.parse(null, "block_crack"));
        assertEquals(Particle.DUST, ParticleSupport.parse(null, "reddust"));
        assertEquals(Particle.FLAME, ParticleSupport.parse(null, "flame"));
    }

    @Test
    void dustUsesColorAndSize() {
        Particle.DustOptions dust = (Particle.DustOptions) ParticleSupport.data(Particle.DUST, Map.of("color", "#608eb6", "size", "2"));
        assertEquals(0x608eb6, dust.getColor().asRGB());
        assertEquals(2f, dust.getSize());
    }

    @Test
    void spellUsesColorAndPower() {
        Particle.Spell spell = (Particle.Spell) ParticleSupport.data(Particle.EFFECT, Map.of("color", "255,0,0", "power", "2"));
        assertEquals(0xFF0000, spell.getColor().asRGB());
        assertEquals(2f, spell.getPower());
    }

    @Test
    void plainParticleHasNoData() {
        assertNull(ParticleSupport.data(Particle.FLAME, Map.of()));
    }
}
