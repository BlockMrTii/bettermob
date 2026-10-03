package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class TotemMechanic implements Mechanic, Listener {
    private record TotemBody(LivingEntity caster, String lines, SkillContext at) {}

    private final SkillEngine engine;
    private final Map<UUID, TotemBody> totemBodies = new ConcurrentHashMap<>();

    public TotemMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        SkillContext context = call.context();
        Location origin = call.target().location().clone().add(0, parseFloat(firstParam(p, "yo", "yoffset"), 0f), 0);
        SkillContext at = context.withOrigin(origin);
        if (engine.debug().verbose()) engine.debug().verbose("totem at " + origin.getBlockX() + " " + origin.getBlockY() + " " + origin.getBlockZ() + ", params " + p.keySet(), engine.subject(context.caster()));
        runLines(firstParam(p, "os", "onstart"), at);

        int duration = parseInt(firstParam(p, "md", "maxduration"), 0);
        String onTick = firstParam(p, "ot", "ontick");
        String onEnd = firstParam(p, "oe", "onend");
        String onHit = firstParam(p, "oh", "onhit");
        if (onHit != null) spawnBody(context, at, onHit, duration > 0 ? duration : 100);
        if (duration <= 0 || (onTick == null && onEnd == null)) return;

        long interval = Math.max(1, parseInt(firstParam(p, "i", "interval"), 20));
        long[] elapsed = {0};
        Runnable[] cancel = {() -> { }};
        cancel[0] = Tasks.runTimer(engine.plugin(), context.caster(), interval, interval, () -> {
            if (context.caster().isDead()) {
                cancel[0].run();
                return;
            }
            elapsed[0] += interval;
            runLines(onTick, at);
            if (elapsed[0] >= duration) {
                cancel[0].run();
                runLines(onEnd, at);
            }
        });
    }

    private void spawnBody(SkillContext context, SkillContext at, String lines, int duration) {
        Location origin = at.origin();
        ArmorStand body = origin.getWorld().spawn(origin, ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setSilent(true);
            stand.setPersistent(false);
        });
        totemBodies.put(body.getUniqueId(), new TotemBody(context.caster(), lines, at));
        Tasks.runLater(engine.plugin(), body, duration, () -> {
            totemBodies.remove(body.getUniqueId());
            body.remove();
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onTotemHit(EntityDamageByEntityEvent event) {
        TotemBody totem = totemBodies.get(event.getEntity().getUniqueId());
        if (totem == null) return;
        event.setCancelled(true);
        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) damager = shooter;
        if (damager instanceof LivingEntity attacker && !attacker.equals(totem.caster())) {
            engine.runSteps(engine.inline(totem.lines()),
                    new SkillContext(totem.caster(), attacker, null, totem.at().origin(), true));
        }
    }

    private void runLines(String lines, SkillContext context) {
        if (lines != null) engine.runInline(lines, context);
    }
}
