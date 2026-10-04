package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.integration.PlaceholderHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class MessageMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        String raw = firstParam(call.params(), "m", "message", "msg");
        if (raw == null || !(call.target().entity() instanceof Player receiver)) return;
        Component template = LegacyComponentSerializer.legacyAmpersand().deserialize(PlaceholderHook.apply(receiver, stripQuotes(raw)));
        receiver.sendMessage(withName(withName(template, "<caster.name>", call.context().caster().getName()), "<target.name>", receiver.getName()));
    }

    static Component withName(Component template, String token, String name) {
        return template.replaceText(builder -> builder.matchLiteral(token).replacement(Component.text(name)));
    }
}
