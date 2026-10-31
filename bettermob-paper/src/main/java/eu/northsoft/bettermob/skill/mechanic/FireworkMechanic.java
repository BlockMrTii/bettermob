package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillTags;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class FireworkMechanic implements Mechanic {
    private static final int MAX_TICKS = 100;

    private final SkillEngine engine;

    public FireworkMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    static List<Color> colors(String raw, Color fallback) {
        List<Color> colors = new ArrayList<>();
        if (raw != null) {
            for (String part : stripQuotes(raw).split(",")) {
                int rgb = ModelEffectMechanic.parseColor(part.trim(), -1);
                if (rgb >= 0) colors.add(Color.fromRGB(rgb));
            }
        }
        if (colors.isEmpty() && fallback != null) colors.add(fallback);
        return colors;
    }

    static FireworkEffect.Type type(String raw) {
        if (raw == null) return FireworkEffect.Type.BALL;
        try {
            return FireworkEffect.Type.valueOf(stripQuotes(raw).trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return FireworkEffect.Type.BALL;
        }
    }

    @Override
    public void execute(MechanicCall call) {
        Location at = call.target().location();
        if (at == null || at.getWorld() == null) return;
        FireworkEffect effect = FireworkEffect.builder()
                .with(type(firstParam(call.params(), "type", "t")))
                .withColor(colors(firstParam(call.params(), "color", "colors", "c"), Color.WHITE))
                .withFade(colors(firstParam(call.params(), "fadecolor", "fadecolors", "fc"), null))
                .flicker("true".equalsIgnoreCase(firstParam(call.params(), "flicker", "f")))
                .trail("true".equalsIgnoreCase(firstParam(call.params(), "trail", "tr")))
                .build();
        int ticks = Math.max(0, Math.min(MAX_TICKS, parseInt(firstParam(call.params(), "duration", "d"), 0)));
        Firework firework = at.getWorld().spawn(at, Firework.class, rocket -> {
            rocket.addScoreboardTag(SkillTags.EFFECT_TAG);
            rocket.setPersistent(false);
            FireworkMeta meta = rocket.getFireworkMeta();
            meta.addEffect(effect);
            meta.setPower(0);
            rocket.setFireworkMeta(meta);
        });
        if (ticks == 0) firework.detonate();
        else Tasks.runLater(engine.plugin(), firework, ticks, firework::detonate);
    }
}
