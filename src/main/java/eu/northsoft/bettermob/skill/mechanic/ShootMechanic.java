package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillStep;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class ShootMechanic implements Mechanic, Listener {
    private record PendingShot(LivingEntity shooter, List<SkillStep> onHit, List<SkillStep> onEnd, double damage, Runnable stopTicker) {}

    private static final Map<String, Class<? extends Projectile>> PROJECTILES = Map.of(
            "arrow", Arrow.class,
            "spectralarrow", SpectralArrow.class,
            "trident", Trident.class,
            "snowball", Snowball.class,
            "egg", Egg.class,
            "fireball", LargeFireball.class,
            "smallfireball", SmallFireball.class);

    private final SkillEngine engine;
    private final Map<UUID, PendingShot> shots = new ConcurrentHashMap<>();

    public ShootMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @EventHandler
    public void onProjectileRemove(EntityRemoveEvent event) {
        PendingShot shot = shots.remove(event.getEntity().getUniqueId());
        if (shot != null) shot.stopTicker().run();
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        PendingShot shot = shots.remove(event.getEntity().getUniqueId());
        if (shot == null) return;
        shot.stopTicker().run();
        if (event.getHitEntity() instanceof LivingEntity hit && !hit.equals(shot.shooter())) {
            if (shot.damage() > 0) engine.state().applyDamage(hit, shot.damage(), shot.shooter());
            if (shot.onHit() != null) engine.runSteps(shot.onHit(), new SkillContext(shot.shooter(), hit, null).withTrigger(hit));
        }
        if (shot.onEnd() != null) engine.runSteps(shot.onEnd(), SkillContext.of(shot.shooter()).withOrigin(event.getEntity().getLocation()));
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        LivingEntity caster = call.context().caster();
        LivingEntity aim = call.context().trigger();
        if (aim == null && caster instanceof Mob mob) aim = mob.getTarget();
        if (aim == null) return;

        String typeName = p.getOrDefault("type", "arrow").toLowerCase(Locale.ROOT).replace("_", "");
        Class<? extends Projectile> type = PROJECTILES.get(typeName);
        if (type == null) {
            engine.plugin().messages().warn("skill.shootUnknownProjectile", "type", p.get("type"));
            type = Arrow.class;
        }

        double speed = Math.max(0.1, parseFloat(p.get("velocity"), 1f) * 2);
        double damage = parseFloat(p.get("damage"), 2f);
        if (engine.debug().verbose()) engine.debug().verbose("shoot " + typeName + " at " + aim.getName() + ", speed " + speed + ", damage " + damage, engine.subject(caster));
        double spread = Math.toRadians(parseFloat(p.get("spread"), 0f));
        boolean gravity = !"false".equalsIgnoreCase(p.get("gravity"));
        Vector direction = aim.getEyeLocation().toVector().subtract(caster.getEyeLocation().toVector()).normalize();
        if (spread > 0) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            direction.add(new Vector(random.nextDouble(-spread, spread), random.nextDouble(-spread, spread), random.nextDouble(-spread, spread))).normalize();
        }
        Vector heading = direction.clone();
        String onHit = p.get("oh");
        String onEnd = p.get("oe");
        String onTick = p.get("ot");
        long interval = Math.max(1, parseInt(firstParam(p, "i", "interval"), 5));

        caster.launchProjectile(type, direction.multiply(speed), projectile -> {
            double extraDamage = damage;
            if (projectile instanceof AbstractArrow arrow) {
                arrow.setDamage(damage / speed);
                arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                extraDamage = 0;
            }
            if (projectile instanceof Fireball fireball) {
                fireball.setDirection(heading);
                fireball.setYield(0);
                fireball.setIsIncendiary(false);
            }
            if (!gravity) projectile.setGravity(false);

            Runnable[] stop = {() -> { }};
            if (onTick != null) {
                List<SkillStep> tickSteps = engine.inline(onTick);
                stop[0] = Tasks.runTimer(engine.plugin(), projectile, interval, interval,
                        () -> engine.runSteps(tickSteps, SkillContext.of(caster).withOrigin(projectile.getLocation())));
            }
            if (onHit != null || onEnd != null || extraDamage > 0 || onTick != null) {
                shots.put(projectile.getUniqueId(), new PendingShot(caster,
                        onHit == null ? null : engine.inline(onHit),
                        onEnd == null ? null : engine.inline(onEnd),
                        extraDamage, () -> stop[0].run()));
                Tasks.runLater(engine.plugin(), projectile, 400L, () -> {
                    shots.remove(projectile.getUniqueId());
                    stop[0].run();
                });
            }
        });
    }
}
