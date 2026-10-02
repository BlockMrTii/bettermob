package eu.northsoft.bettermob;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.destroystokyo.paper.entity.ai.MobGoals;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Wendet AIGoalSelectors/AITargetSelectors an wie bei MysticMobs: "clear" leert die
 * Kategorie, jeder weitere Eintrag muss ein Vanilla-Goal sein, das dieser Mob-Typ
 * tatsaechlich besitzt - Paper kann keine neuen Verhalten erfinden, nur vorhandene
 * wieder anmelden. Nicht vorhandene Namen werden uebersprungen und geloggt.
 */
final class AiGoalApplier {
    private AiGoalApplier() {}

    static void apply(Mob mob, List<String> selectors, List<String> targetSelectors, Logger logger) {
        MobGoals mobGoals = Bukkit.getMobGoals();
        applyCategory(mobGoals, mob, selectors, logger, GoalType.MOVE, GoalType.LOOK, GoalType.JUMP);
        applyCategory(mobGoals, mob, targetSelectors, logger, GoalType.TARGET);
    }

    private static void applyCategory(MobGoals mobGoals, Mob mob, List<String> tokens, Logger logger, GoalType... types) {
        if (tokens.isEmpty()) return;

        // Vorhandene Goals sichern, bevor "clear" sie entfernt - nur daraus kann
        // spaeter wieder angemeldet werden.
        List<Goal<Mob>> snapshot = new ArrayList<>();
        for (GoalType type : types) snapshot.addAll(mobGoals.getAllGoals(mob, type));

        if (containsClear(tokens)) for (GoalType type : types) mobGoals.removeAllGoals(mob, type);

        int priority = 0;
        for (String token : tokens) {
            if (token.equalsIgnoreCase("clear")) continue;
            Goal<Mob> match = findByName(snapshot, token);
            if (match == null) {
                logger.warning("AI-Goal '" + token + "' ist fuer Mob-Typ '" + mob.getType() + "' nicht verfuegbar.");
                continue;
            }
            mobGoals.addGoal(mob, priority++, match);
        }
    }

    private static boolean containsClear(List<String> tokens) {
        for (String token : tokens) if (token.equalsIgnoreCase("clear")) return true;
        return false;
    }

    private static Goal<Mob> findByName(List<Goal<Mob>> snapshot, String token) {
        String normalized = normalize(token);
        for (Goal<Mob> goal : snapshot) {
            if (normalize(goal.getKey().getNamespacedKey().getKey()).equals(normalized)) return goal;
        }
        // Vanilla nennt z.B. den Wander-Goal "water_avoiding_random_stroll" statt
        // "random_stroll" - per Teilstring matchen, damit die kurzen MysticMobs-
        // Namen trotzdem das tatsaechlich registrierte Goal treffen.
        for (Goal<Mob> goal : snapshot) {
            if (normalize(goal.getKey().getNamespacedKey().getKey()).contains(normalized)) return goal;
        }
        return null;
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }
}
