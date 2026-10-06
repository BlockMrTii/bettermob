package eu.northsoft.bettermob.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import eu.northsoft.bettermob.mob.Behaviour;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.function.Supplier;

public final class BehaviourGoals {
    public static final int GUARD_PRIORITY = 1;
    public static final int PATROL_PRIORITY = 6;
    private static final double REACHED_SQUARED = 2.25;
    private static final int REPATH_TICKS = 20;

    private BehaviourGoals() {}

    public static GoalKey<Mob> patrolKey(Plugin plugin) {
        return GoalKey.of(Mob.class, new NamespacedKey(plugin, "patrol"));
    }

    public static GoalKey<Mob> guardKey(Plugin plugin) {
        return GoalKey.of(Mob.class, new NamespacedKey(plugin, "guard"));
    }

    public static void apply(Plugin plugin, Mob mob, Behaviour behaviour, Supplier<Location> home) {
        var goals = Bukkit.getMobGoals();
        goals.removeGoal(mob, patrolKey(plugin));
        goals.removeGoal(mob, guardKey(plugin));
        if (behaviour == null) return;
        if (behaviour.hasGuard()) goals.addGoal(mob, GUARD_PRIORITY, guard(plugin, mob, behaviour.guardRadius(), home));
        if (behaviour.hasPatrol()) goals.addGoal(mob, PATROL_PRIORITY, patrol(plugin, mob, behaviour));
    }

    static int nextIndex(int index, int size, boolean loop) {
        if (size == 0) return 0;
        if (index + 1 < size) return index + 1;
        return loop ? 0 : size;
    }

    static Goal<Mob> patrol(Plugin plugin, Mob mob, Behaviour behaviour) {
        GoalKey<Mob> key = patrolKey(plugin);
        int[] state = {0, 0, 0};
        return new Goal<>() {
            @Override
            public boolean shouldActivate() {
                return mob.getTarget() == null && state[0] < behaviour.patrol().size();
            }

            @Override
            public boolean shouldStayActive() {
                return shouldActivate();
            }

            @Override
            public void tick() {
                if (state[1] > 0) {
                    state[1]--;
                    return;
                }
                Behaviour.Waypoint point = behaviour.patrol().get(state[0]);
                World world = point.world() == null ? mob.getWorld() : Bukkit.getWorld(point.world());
                if (world == null || !world.equals(mob.getWorld())) {
                    state[0] = nextIndex(state[0], behaviour.patrol().size(), behaviour.loop());
                    return;
                }
                Location destination = new Location(world, point.x(), point.y(), point.z());
                if (mob.getLocation().distanceSquared(destination) < REACHED_SQUARED) {
                    state[1] = behaviour.waitTicks();
                    state[0] = nextIndex(state[0], behaviour.patrol().size(), behaviour.loop());
                    mob.getPathfinder().stopPathfinding();
                    return;
                }
                if (state[2]++ % REPATH_TICKS == 0) mob.getPathfinder().moveTo(destination, 1.0);
            }

            @Override
            public void stop() {
                mob.getPathfinder().stopPathfinding();
            }

            @Override
            public GoalKey<Mob> getKey() {
                return key;
            }

            @Override
            public EnumSet<GoalType> getTypes() {
                return EnumSet.of(GoalType.MOVE);
            }
        };
    }

    static Goal<Mob> guard(Plugin plugin, Mob mob, double radius, Supplier<Location> home) {
        GoalKey<Mob> key = guardKey(plugin);
        int[] ticks = {0};
        return new Goal<>() {
            @Override
            public boolean shouldActivate() {
                return away(radius);
            }

            @Override
            public boolean shouldStayActive() {
                return away(REACHED_SQUARED / 2);
            }

            private boolean away(double limit) {
                Location base = home.get();
                if (base == null || !base.getWorld().equals(mob.getWorld())) return false;
                return mob.getLocation().distanceSquared(base) > limit * limit;
            }

            @Override
            public void start() {
                mob.setTarget(null);
                ticks[0] = 0;
            }

            @Override
            public void tick() {
                if (mob.getTarget() != null) mob.setTarget(null);
                Location base = home.get();
                if (base != null && ticks[0]++ % REPATH_TICKS == 0) mob.getPathfinder().moveTo(base, 1.2);
            }

            @Override
            public void stop() {
                mob.getPathfinder().stopPathfinding();
            }

            @Override
            public GoalKey<Mob> getKey() {
                return key;
            }

            @Override
            public EnumSet<GoalType> getTypes() {
                return EnumSet.of(GoalType.MOVE);
            }
        };
    }
}
