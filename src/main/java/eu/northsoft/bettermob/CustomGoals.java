package eu.northsoft.bettermob;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * KI-Verhalten, das Vanilla nicht mitbringt, aber MythicMobs-Packs per Namen anfordern
 * (lookAtTarget, monsters). Paper erlaubt eigene Goals - diese hier sind bewusst klein.
 */
final class CustomGoals {
    private CustomGoals() {}

    /** Dreht den Kopf zum Ziel, solange es im Radius ist - noetig, wenn die Waffen-Goals nach "clear" fehlen. */
    static Goal<Mob> lookAtTarget(Plugin plugin, Mob mob, double radius) {
        GoalKey<Mob> key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "look_at_target"));
        return new Goal<>() {
            @Override
            public boolean shouldActivate() {
                LivingEntity target = mob.getTarget();
                return target != null && target.getWorld().equals(mob.getWorld())
                        && target.getLocation().distanceSquared(mob.getLocation()) <= radius * radius;
            }

            @Override
            public void tick() {
                LivingEntity target = mob.getTarget();
                if (target != null) mob.lookAt(target);
            }

            @Override
            public GoalKey<Mob> getKey() {
                return key;
            }

            @Override
            public EnumSet<GoalType> getTypes() {
                return EnumSet.of(GoalType.LOOK);
            }
        };
    }

    /**
     * Greift das naechste feindliche Monster an. Ohne Fraktionen gelten alle Mobs, die BetterMob selbst
     * verwaltet, als Freunde (sonst wuerden sich z.B. zwei Haustier-Skelette gegenseitig jagen).
     */
    static Goal<Mob> nearestMonster(Plugin plugin, Mob mob, Predicate<Entity> managed) {
        GoalKey<Mob> key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "nearest_monster"));
        return new Goal<>() {
            private LivingEntity found;
            private int ticks;

            @Override
            public boolean shouldActivate() {
                if (mob.getTarget() != null) return false;
                // Das Umfeld nur alle 10 Ticks absuchen, nicht in jedem Tick.
                if (++ticks % 10 != 0) return false;
                var follow = mob.getAttribute(Attribute.FOLLOW_RANGE);
                double radius = follow == null ? 16 : follow.getValue();
                found = null;
                double best = Double.MAX_VALUE;
                for (Entity entity : mob.getNearbyEntities(radius, radius, radius)) {
                    if (!(entity instanceof Monster monster) || monster.isDead() || managed.test(monster)) continue;
                    double distance = monster.getLocation().distanceSquared(mob.getLocation());
                    if (distance < best) {
                        best = distance;
                        found = monster;
                    }
                }
                return found != null;
            }

            @Override
            public boolean shouldStayActive() {
                return false;
            }

            @Override
            public void start() {
                if (found != null && found.isValid()) mob.setTarget(found);
            }

            @Override
            public GoalKey<Mob> getKey() {
                return key;
            }

            @Override
            public EnumSet<GoalType> getTypes() {
                return EnumSet.of(GoalType.TARGET);
            }
        };
    }
}
