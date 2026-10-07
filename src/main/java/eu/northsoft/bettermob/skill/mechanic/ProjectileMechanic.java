package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillStep;
import eu.northsoft.bettermob.util.RegionEntities;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;
import static eu.northsoft.bettermob.skill.SkillTags.HELPER_TAG;

public final class ProjectileMechanic implements Mechanic {
    private static final double MAX_RANGE = 200;
    private static final double TURN_PER_TICK = Math.toRadians(8);
    private static final double MIN_REACH_DIVISOR = 8;

    private final SkillEngine engine;

    public ProjectileMechanic(SkillEngine engine) {
        this.engine = engine;
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
        if (aim != null && !Bukkit.isOwnedByCurrentRegion(aim)) aim = null;

        Location start = caster.getEyeLocation();
        Vector direction = aim != null ? aim.getEyeLocation().toVector().subtract(start.toVector()) : start.getDirection();
        if (direction.lengthSquared() < 1.0e-9) direction = start.getDirection();
        direction.normalize();

        double perTick = Math.max(0.05, Math.min(5, parseFloat(p.get("speed"), 20f) / 20.0));
        double range = Math.max(1, Math.min(MAX_RANGE, parseFloat(p.get("range"), 30f)));
        double radius = Math.max(0.1, Math.min(8, parseFloat(p.get("radius"), 1f)));
        double damage = Math.max(0, parseFloat(p.get("damage"), 0f));
        int bounces = Math.max(0, Math.min(20, parseInt(p.get("bounce"), 0)));
        boolean homing = "true".equalsIgnoreCase(p.get("homing"));
        String modelId = p.get("model");
        List<SkillStep> onHit = lines(firstParam(p, "oh", "onhit"));
        List<SkillStep> onEnd = lines(firstParam(p, "oe", "onend"));
        List<SkillStep> onTick = lines(firstParam(p, "ot", "ontick"));
        long interval = Math.max(1, parseInt(firstParam(p, "i", "interval"), 5));
        final LivingEntity homingTarget = homing ? aim : null;
        final Vector heading = direction;
        Location origin = start.clone().add(direction.clone().multiply(0.5));
        if (engine.debug().verbose()) engine.debug().verbose("projectile model " + modelId + ", speed " + perTick * 20 + "/s, range " + range + ", homing " + homing + ", bounce " + bounces, engine.subject(caster));

        Tasks.runOwnedAt(engine.plugin(), origin, () -> {
            ArmorStand body = origin.getWorld().spawn(origin, ArmorStand.class, stand -> {
                stand.setInvisible(true);
                stand.setSmall(true);
                stand.setMarker(true);
                stand.setGravity(false);
                stand.setSilent(true);
                stand.setPersistent(false);
                stand.addScoreboardTag(HELPER_TAG);
            });
            Object betterTracker = modelId == null ? null : engine.betterModel().attachIfPresent(body, modelId);
            Object engineTracker = modelId == null || betterTracker != null ? null : engine.modelEngine().attachIfPresent(body, modelId);
            new Flight(caster, body, heading, perTick, range, radius, damage, bounces, homingTarget, onHit, onEnd, onTick, interval, betterTracker, engineTracker).start();
        });
    }

    private List<SkillStep> lines(String raw) {
        return raw == null ? null : engine.inline(raw);
    }

    private final class Flight {
        private final LivingEntity caster;
        private final ArmorStand body;
        private final double perTick;
        private final double range;
        private final double radius;
        private final double damage;
        private final LivingEntity homingTarget;
        private final List<SkillStep> onHit;
        private final List<SkillStep> onEnd;
        private final List<SkillStep> onTick;
        private final long interval;
        private final Object betterTracker;
        private final Object engineTracker;
        private Vector direction;
        private int bouncesLeft;
        private double travelled;
        private long ticks;
        private boolean finished;
        private boolean crossedBorder;
        private Runnable cancel = () -> { };

        Flight(LivingEntity caster, ArmorStand body, Vector direction, double perTick, double range, double radius, double damage,
               int bounces, LivingEntity homingTarget, List<SkillStep> onHit, List<SkillStep> onEnd, List<SkillStep> onTick,
               long interval, Object betterTracker, Object engineTracker) {
            this.caster = caster;
            this.body = body;
            this.direction = direction;
            this.perTick = perTick;
            this.range = range;
            this.radius = radius;
            this.damage = damage;
            this.bouncesLeft = bounces;
            this.homingTarget = homingTarget;
            this.onHit = onHit;
            this.onEnd = onEnd;
            this.onTick = onTick;
            this.interval = interval;
            this.betterTracker = betterTracker;
            this.engineTracker = engineTracker;
        }

        void start() {
            cancel = Tasks.runTimer(engine.plugin(), body, 1, 1, this::tick);
        }

        private void tick() {
            if (finished) return;
            if (!body.isValid() || Bukkit.isOwnedByCurrentRegion(caster) && (!caster.isValid() || caster.isDead())) {
                finish(null, false);
                return;
            }
            Location position = body.getLocation();
            if (crossedBorder) {
                crossedBorder = false;
                if (strike(position)) return;
            }
            if (homingTarget != null && homingTarget.isValid() && homingTarget.getWorld().equals(position.getWorld())) {
                direction = ProjectileMotion.steer(direction, homingTarget.getEyeLocation().toVector().subtract(position.toVector()), TURN_PER_TICK);
            }
            double ownedReach = perTick;
            while (ownedReach > perTick / MIN_REACH_DIVISOR && !Bukkit.isOwnedByCurrentRegion(position.clone().add(direction.clone().multiply(ownedReach)))) ownedReach /= 2;
            boolean ownedAhead = Bukkit.isOwnedByCurrentRegion(position.clone().add(direction.clone().multiply(ownedReach)));
            RayTraceResult block = ownedAhead ? position.getWorld().rayTraceBlocks(position, direction, ownedReach, org.bukkit.FluidCollisionMode.NEVER, true) : null;
            double step = block == null ? ownedReach : Math.max(0, block.getHitPosition().distance(position.toVector()));
            Location next = position.clone().add(direction.clone().multiply(step));
            next.setDirection(direction);
            body.teleportAsync(next);
            travelled += step;

            if (strike(next)) return;
            crossedBorder = !Bukkit.isOwnedByCurrentRegion(next);
            if (block != null && block.getHitBlockFace() != null) {
                if (bouncesLeft > 0) {
                    bouncesLeft--;
                    direction = ProjectileMotion.reflect(direction, block.getHitBlockFace().getDirection());
                } else {
                    finish(null, true);
                    return;
                }
            }
            if (travelled >= range) {
                finish(null, true);
                return;
            }
            ticks++;
            if (onTick != null && ticks % interval == 0) {
                Location at = next.clone();
                Tasks.runOwned(engine.plugin(), caster, () -> engine.runSteps(onTick, SkillContext.of(caster).withOrigin(at)));
            }
        }

        private boolean strike(Location at) {
            for (Entity entity : RegionEntities.near(at, radius)) {
                if (!(entity instanceof LivingEntity candidate) || candidate.equals(caster) || candidate.getScoreboardTags().contains(HELPER_TAG) || candidate instanceof ArmorStand) continue;
                if (damage > 0) engine.state().applyDamage(candidate, damage, caster);
                finish(candidate, true);
                return true;
            }
            return false;
        }

        private void finish(LivingEntity hit, boolean runSkills) {
            if (finished) return;
            finished = true;
            cancel.run();
            Location end = body.getLocation();
            engine.betterModel().close(betterTracker);
            engine.modelEngine().close(engineTracker);
            Tasks.runOn(engine.plugin(), body, body::remove);
            if (!runSkills) return;
            if (hit != null && onHit != null) {
                Tasks.runOwned(engine.plugin(), caster, () -> engine.runSteps(onHit, new SkillContext(caster, hit, null).withTrigger(hit).withOrigin(end)));
            }
            if (onEnd != null) {
                Tasks.runOwned(engine.plugin(), caster, () -> engine.runSteps(onEnd, SkillContext.of(caster).withOrigin(end)));
            }
        }
    }
}
