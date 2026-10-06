package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;

import java.util.Locale;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class BossBarMechanic implements Mechanic {
    private static final int DEFAULT_TICKS = 100;
    private static final int COUNTDOWN_STEP = 2;

    private final SkillEngine engine;

    public BossBarMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        String raw = firstParam(call.params(), "m", "message", "msg");
        if (raw == null || !(call.target().entity() instanceof Player receiver)) return;
        int duration = Math.max(1, MessageText.ticks(firstParam(call.params(), "d", "duration"), DEFAULT_TICKS));
        float progress = Math.max(0f, Math.min(1f, parseFloat(firstParam(call.params(), "p", "progress"), 1f)));
        boolean countdown = "true".equalsIgnoreCase(call.params().get("countdown"));

        BossBar bar = BossBar.bossBar(MessageText.render(raw, receiver, call.context().caster().getName()), progress,
                color(call.params().get("color")), overlay(call.params().get("style")));
        receiver.showBossBar(bar);
        Tasks.runLater(engine.plugin(), receiver, duration, () -> receiver.hideBossBar(bar));
        if (countdown) {
            for (int elapsed = COUNTDOWN_STEP; elapsed < duration; elapsed += COUNTDOWN_STEP) {
                float remaining = progress * (1f - (float) elapsed / duration);
                Tasks.runLater(engine.plugin(), receiver, elapsed, () -> bar.progress(remaining));
            }
        }
    }

    static BossBar.Color color(String name) {
        if (name == null) return BossBar.Color.PURPLE;
        try {
            return BossBar.Color.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return BossBar.Color.PURPLE;
        }
    }

    static BossBar.Overlay overlay(String name) {
        if (name == null) return BossBar.Overlay.PROGRESS;
        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "SEGMENTED_6", "NOTCHED_6" -> BossBar.Overlay.NOTCHED_6;
            case "SEGMENTED_10", "NOTCHED_10" -> BossBar.Overlay.NOTCHED_10;
            case "SEGMENTED_12", "NOTCHED_12" -> BossBar.Overlay.NOTCHED_12;
            case "SEGMENTED_20", "NOTCHED_20" -> BossBar.Overlay.NOTCHED_20;
            default -> BossBar.Overlay.PROGRESS;
        };
    }
}
