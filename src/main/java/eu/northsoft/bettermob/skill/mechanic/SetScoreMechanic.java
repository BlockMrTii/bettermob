package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.ScoreboardManager;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseNumber;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class SetScoreMechanic implements Mechanic {
    private final boolean enabled;

    public SetScoreMechanic() {
        this(!Tasks.FOLIA);
    }

    SetScoreMechanic(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void execute(MechanicCall call) {
        if (!enabled) return;
        Entity entity = call.target().entity();
        String objectiveName = firstParam(call.params(), "objective", "obj", "o");
        double value = parseNumber(firstParam(call.params(), "value", "v"), Double.NaN);
        if (entity == null || objectiveName == null || Double.isNaN(value)) return;
        try {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            Objective objective = manager == null ? null : manager.getMainScoreboard().getObjective(stripQuotes(objectiveName));
            if (objective == null) return;
            String entry = entity instanceof Player player ? player.getName() : entity.getUniqueId().toString();
            objective.getScore(entry).setScore((int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value)));
        } catch (UnsupportedOperationException ignored) {
        }
    }
}
