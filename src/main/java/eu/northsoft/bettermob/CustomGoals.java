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

final class CustomGoals {
    private CustomGoals() {}

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

    static Goal<Mob> nearestMonster(Plugin plugin, Mob mob, Predicate<Entity> managed) {
        GoalKey<Mob> key = GoalKey.of(Mob.class, new NamespacedKey(plugin, "nearest_monster"));
        return new Goal<>() {
            private LivingEntity found;
            private int ticks;

            @Override
            public boolean shouldActivate() {
                if (mob.getTarget() != null) return false;

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
