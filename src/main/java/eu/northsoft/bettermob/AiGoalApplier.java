package eu.northsoft.bettermob;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalType;
import com.destroystokyo.paper.entity.ai.MobGoals;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Wendet AIGoalSelectors/AITargetSelectors an wie bei MysticMobs: "clear" leert die
 * Kategorie, jeder weitere Eintrag muss ein Vanilla-Goal sein, das dieser Mob-Typ
 * tatsaechlich besitzt - Paper kann keine neuen Verhalten erfinden, nur vorhandene
 * wieder anmelden. Nicht vorhandene Namen werden uebersprungen und geloggt.
 */
final class AiGoalApplier {
    /** MythicMobs-Namen, die bei Paper anders heissen (normalisiert, ohne _ und -). */
    private static final Map<String, String> ALIASES = Map.of(
            "attacker", "hurtby",
            "players", "nearestattackable",
            "nearestplayer", "nearestattackable",
            "nearestplayers", "nearestattackable",
            "lookatplayers", "lookatplayer",
            "fleeplayers", "avoidentity");

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
        // MythicMobs-Zeilen tragen Parameter ("meleeattack{attackReach=2}") - die gehoeren
        // nicht zum Namen. Paper kann sie ohnehin nicht setzen.
        int brace = token.indexOf('{');
        String normalized = normalize(brace < 0 ? token : token.substring(0, brace));
        normalized = ALIASES.getOrDefault(normalized, normalized);

        for (Goal<Mob> goal : snapshot) {
            if (keyOf(goal).equals(normalized)) return goal;
        }
        // Vanilla nennt z.B. den Wander-Goal "water_avoiding_random_stroll" statt
        // "random_stroll" - per Teilstring matchen. Endet der Name auf den Suchbegriff,
        // gewinnt er vor blossem Enthaltensein: der Eisengolem hat zusaetzlich
        // "golem_random_stroll_in_village", das nur im Dorf wandert.
        for (Goal<Mob> goal : snapshot) {
            if (keyOf(goal).endsWith(normalized)) return goal;
        }
        for (Goal<Mob> goal : snapshot) {
            if (keyOf(goal).contains(normalized)) return goal;
        }
        return null;
    }

    private static String keyOf(Goal<Mob> goal) {
        return normalize(goal.getKey().getNamespacedKey().getKey());
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }
}
