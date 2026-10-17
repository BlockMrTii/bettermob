package eu.northsoft.bettermob.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import eu.northsoft.bettermob.mob.Behaviour;
import eu.northsoft.bettermob.util.RegionEntities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.EnumSet;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BehaviourGoals {
    public static final int GUARD_PRIORITY = 1;
    public static final int LEADER_PRIORITY = 5;
    public static final int PATROL_PRIORITY = 6;
    private static final double REACHED_SQUARED = 2.25;
    private static final int REPATH_TICKS = 20;

    private BehaviourGoals() {}

    public static GoalKey<Mob> patrolKey(Plugin plugin) {
        return GoalKey.of(Mob.class, new NamespacedKey(plugin, "patrol"));
    }

    public static GoalKey<Mob> leaderKey(Plugin plugin) {
        return GoalKey.of(Mob.class, new NamespacedKey(plugin, "leader"));
    }

    public static GoalKey<Mob> guardKey(Plugin plugin) {
        return GoalKey.of(Mob.class, new NamespacedKey(plugin, "guard"));
    }

    public static void apply(Plugin plugin, Mob mob, Behaviour behaviour, Supplier<Location> home, Function<Entity, String> mobId) {
        var goals = Bukkit.getMobGoals();
        synchronized (AiGoalApplier.GOAL_LOOKUP) {
            goals.removeGoal(mob, patrolKey(plugin));
            goals.removeGoal(mob, guardKey(plugin));
            goals.removeGoal(mob, leaderKey(plugin));
        }
        if (behaviour == null) return;
        if (behaviour.hasGuard()) goals.addGoal(mob, GUARD_PRIORITY, guard(plugin, mob, behaviour.guardRadius(), home));
        if (behaviour.leader() != null) goals.addGoal(mob, LEADER_PRIORITY, follow(plugin, mob, behaviour.leader(), home, mobId));
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

    static Goal<Mob> follow(Plugin plugin, Mob mob, Behaviour.Leader settings, Supplier<Location> home, Function<Entity, String> mobId) {
        GoalKey<Mob> key = leaderKey(plugin);
        LivingEntity[] leader = {null};
        boolean[] lost = {false};
        int[] ticks = {0, 0};
        return new Goal<>() {
            private LivingEntity current() {
                LivingEntity found = leader[0];
                if (found != null && found.isValid() && found.getWorld().equals(mob.getWorld())) return Bukkit.isOwnedByCurrentRegion(found) ? found : null;
                if (found != null) {
                    leader[0] = null;
                    if (settings.onLoss() != Behaviour.LeaderLoss.FIND) lost[0] = true;
                }
                if (lost[0] || ticks[0]++ % REPATH_TICKS != 0) return null;
                double best = Double.MAX_VALUE;
                for (Entity entity : RegionEntities.near(mob.getLocation(), settings.range())) {
                    if (!(entity instanceof LivingEntity candidate) || candidate.equals(mob) || !settings.mob().equalsIgnoreCase(mobId.apply(candidate))) continue;
                    double distance = candidate.getLocation().distanceSquared(mob.getLocation());
                    if (distance < best) {
                        best = distance;
                        leader[0] = candidate;
                    }
                }
                return leader[0];
            }

            private Location destination() {
                if (lost[0]) return settings.onLoss() == Behaviour.LeaderLoss.HOME ? home.get() : null;
                LivingEntity found = current();
                return found == null ? null : found.getLocation();
            }

            private boolean far(double limit) {
                Location to = destination();
                return to != null && to.getWorld().equals(mob.getWorld()) && mob.getLocation().distanceSquared(to) > limit * limit;
            }

            @Override
            public boolean shouldActivate() {
                return mob.getTarget() == null && far(settings.distance());
            }

            @Override
            public boolean shouldStayActive() {
                return mob.getTarget() == null && far(Math.max(1, settings.distance() * 0.6));
            }

            @Override
            public void tick() {
                Location to = destination();
                if (to != null && ticks[1]++ % REPATH_TICKS == 0) mob.getPathfinder().moveTo(to, 1.1);
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
