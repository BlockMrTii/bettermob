package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class SpeakMechanic implements Mechanic {
    private static final int MAX_TICKS = 20 * 60;

    private final SkillEngine engine;
    private record Bubble(TextDisplay display, int tick) {}

    private final Map<UUID, Bubble> bubbles = new ConcurrentHashMap<>();

    public SpeakMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        String raw = firstParam(call.params(), "m", "message", "msg");
        if (raw == null) return;
        LivingEntity caster = call.context().caster();
        String text = stripQuotes(raw).replace("<caster.name>", caster.getName());
        if (!"false".equalsIgnoreCase(firstParam(call.params(), "chat", "c")) && call.target().entity() instanceof Player receiver) {
            receiver.sendMessage(MessageText.render("&f" + caster.getName() + ": " + text, receiver, caster.getName()));
        }
        Bubble old = bubbles.get(caster.getUniqueId());
        if (old != null && old.tick() == caster.getTicksLived()) return;
        int ticks = Math.max(1, Math.min(MAX_TICKS, parseInt(firstParam(call.params(), "d", "duration"), 60)));
        Location at = caster.getLocation().add(0, caster.getHeight() + parseFloat(firstParam(call.params(), "yo", "yoffset", "offset"), 0.5f), 0);
        if (old != null) Tasks.runOn(engine.plugin(), old.display(), old.display()::remove);
        Component line = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        TextDisplay bubble = caster.getWorld().spawn(at, TextDisplay.class, display -> {
            display.text(line);
            display.setBillboard(Display.Billboard.CENTER);
            display.setPersistent(false);
        });
        Bubble current = new Bubble(bubble, caster.getTicksLived());
        bubbles.put(caster.getUniqueId(), current);
        Tasks.runLater(engine.plugin(), bubble, ticks, () -> {
            bubbles.remove(caster.getUniqueId(), current);
            bubble.remove();
        }, () -> bubbles.remove(caster.getUniqueId(), current));
    }
}
