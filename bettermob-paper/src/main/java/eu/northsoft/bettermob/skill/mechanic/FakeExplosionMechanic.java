package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

public final class FakeExplosionMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Location at = call.target().location();
        if (at == null || at.getWorld() == null) return;
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 1);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
    }
}
