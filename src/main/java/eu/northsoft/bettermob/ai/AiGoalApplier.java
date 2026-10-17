package eu.northsoft.bettermob.ai;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.destroystokyo.paper.entity.ai.MobGoals;
import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.skill.Params;
import eu.northsoft.bettermob.skill.SkillStep;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiGoalApplier {
    private static final Map<String, List<String>> ALIASES = Map.of(
            "attacker", List.of("hurtby"),
            "players", List.of("nearestattackable"),
            "player", List.of("nearestattackable"),
            "nearestplayer", List.of("nearestattackable"),
            "nearestplayers", List.of("nearestattackable"),
            "lookatplayers", List.of("lookatplayer"),
            "fleeplayers", List.of("avoidentity"),
            "rangedattack", List.of("rangedbowattack", "rangedcrossbowattack"));

    private static final Set<String> PLAYER_ALIASES = Set.of("players", "player", "nearestplayer", "nearestplayers");
    private static final String PLAYER_CLASS = "net.minecraft.world.entity.player.Player";

    private static final Pattern PRIORITY = Pattern.compile("^(\\d+)\\s+(.*)$");
    static final Object GOAL_LOOKUP = new Object();

    private AiGoalApplier() {}

    private record Token(Integer priority, String name, Map<String, String> params) {
        public static Token parse(String raw) {
            String text = raw.trim();
            Integer priority = null;
            Matcher matcher = PRIORITY.matcher(text);
            if (matcher.matches()) {
                priority = Integer.parseInt(matcher.group(1));
                text = matcher.group(2).trim();
            }
            int brace = text.indexOf('{');
            if (brace < 0) return new Token(priority, text, Map.of());
            int end = text.lastIndexOf('}');
            String inner = end > brace ? text.substring(brace + 1, end) : text.substring(brace + 1);
            return new Token(priority, text.substring(0, brace).trim(), SkillStep.parseParams(inner));
        }
    }

    public static void apply(Mob mob, List<String> selectors, List<String> targetSelectors, BetterMobPlugin plugin, Predicate<Entity> managed) {
        MobGoals mobGoals = Bukkit.getMobGoals();
        applyCategory(mobGoals, mob, selectors, plugin, managed, GoalType.MOVE, GoalType.LOOK, GoalType.JUMP);
        applyCategory(mobGoals, mob, targetSelectors, plugin, managed, GoalType.TARGET);
    }

    private static void applyCategory(MobGoals mobGoals, Mob mob, List<String> raw, BetterMobPlugin plugin,
                                      Predicate<Entity> managed, GoalType... types) {
        if (raw.isEmpty()) return;
        List<Token> tokens = raw.stream().map(Token::parse).toList();

        List<Goal<Mob>> snapshot = new ArrayList<>();
        synchronized (GOAL_LOOKUP) {
            for (GoalType type : types) snapshot.addAll(mobGoals.getAllGoals(mob, type));
        }

        if (tokens.stream().anyMatch(token -> token.name().equalsIgnoreCase("clear"))) {
            synchronized (GOAL_LOOKUP) {
                for (GoalType type : types) mobGoals.removeAllGoals(mob, type);
            }
        }

        int next = 0;
        for (Token token : tokens) {
            if (token.name().equalsIgnoreCase("clear")) continue;
            Goal<Mob> match = findByName(snapshot, token.name());
            if (match == null) match = custom(token, mob, plugin, managed);
            if (match == null) {
                plugin.messages().warn("ai.goalUnavailable", "goal", token.name(), "type", mob.getType());
                if (plugin.debug().info()) plugin.debug().info("available goals of " + mob.getType() + ": " + snapshot.stream().map(AiGoalApplier::keyOf).toList());
                continue;
            }
            int priority = token.priority() != null ? token.priority() : next++;
            mobGoals.addGoal(mob, priority, match);
            if (plugin.debug().verbose()) plugin.debug().verbose("goal '" + keyOf(match) + "' added at priority " + priority + " for " + mob.getType());
        }
    }

    public static void promoteRanged(Mob mob) {
        MobGoals mobGoals = Bukkit.getMobGoals();
        List<Goal<Mob>> goals;
        synchronized (GOAL_LOOKUP) {
            goals = new ArrayList<>(mobGoals.getAllGoals(mob));
        }
        for (Goal<Mob> goal : goals) {
            String key = keyOf(goal);
            if (!key.equals("rangedbowattack") && !key.equals("rangedcrossbowattack")) continue;
            mobGoals.removeGoal(mob, goal);
            mobGoals.addGoal(mob, 0, goal);
        }
    }

    private static Goal<Mob> custom(Token token, Mob mob, BetterMobPlugin plugin, Predicate<Entity> managed) {
        return switch (normalize(token.name())) {
            case "lookattarget" -> {
                yield CustomGoals.lookAtTarget(plugin, mob, Params.parseFloat(token.params().get("r"), 15f));
            }
            case "monsters", "monster" -> CustomGoals.nearestMonster(plugin, mob, managed);
            default -> null;
        };
    }

    private static Goal<Mob> findByName(List<Goal<Mob>> snapshot, String name) {
        String normalized = normalize(name);
        List<Goal<Mob>> candidates = snapshot;
        if (PLAYER_ALIASES.contains(normalized)) {
            List<Goal<Mob>> players = snapshot.stream().filter(AiGoalApplier::targetsPlayers).toList();
            if (!players.isEmpty()) candidates = players;
        }
        for (String candidate : ALIASES.getOrDefault(normalized, List.of(normalized))) {
            Goal<Mob> goal = findGoal(candidates, candidate);
            if (goal != null) return goal;
        }
        return null;
    }

    private static Goal<Mob> findGoal(List<Goal<Mob>> snapshot, String normalized) {
        for (Goal<Mob> goal : snapshot) {
            if (keyOf(goal).equals(normalized)) return goal;
        }

        for (Goal<Mob> goal : snapshot) {
            if (keyOf(goal).endsWith(normalized)) return goal;
        }
        for (Goal<Mob> goal : snapshot) {
            if (keyOf(goal).contains(normalized)) return goal;
        }
        return null;
    }

    private static boolean targetsPlayers(Goal<Mob> goal) {
        try {
            Object handle = goal.getClass().getMethod("getHandle").invoke(goal);
            for (Class<?> type = handle.getClass(); type != null; type = type.getSuperclass()) {
                Field field;
                try {
                    field = type.getDeclaredField("targetType");
                } catch (NoSuchFieldException missing) {
                    continue;
                }
                field.setAccessible(true);
                return field.get(handle) instanceof Class<?> target && isPlayerClass(target);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
        return false;
    }

    private static boolean isPlayerClass(Class<?> target) {
        for (Class<?> type = target; type != null; type = type.getSuperclass()) {
            if (type.getName().equals(PLAYER_CLASS)) return true;
        }
        return false;
    }

    private static String keyOf(Goal<Mob> goal) {
        return normalize(goal.getKey().getNamespacedKey().getKey());
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }
}
