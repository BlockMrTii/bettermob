package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.integration.PlaceholderHook;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class MessageMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        String raw = firstParam(call.params(), "m", "message", "msg");
        if (raw == null || !(call.target().entity() instanceof Player receiver)) return;
        String text = PlaceholderHook.apply(receiver, stripQuotes(raw)
                .replace("<caster.name>", call.context().caster().getName())
                .replace("<target.name>", receiver.getName()));
        receiver.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
    }
}
