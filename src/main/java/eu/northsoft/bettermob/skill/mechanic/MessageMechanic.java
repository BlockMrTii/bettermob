package eu.northsoft.bettermob.skill.mechanic;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class MessageMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        String raw = firstParam(call.params(), "m", "message", "msg");
        if (raw == null || !(call.target().entity() instanceof Player receiver)) return;
        receiver.sendMessage(MessageText.render(raw, receiver, call.context().caster().getName()));
    }

    static Component withName(Component template, String token, String name) {
        return MessageText.withName(template, token, name);
    }
}
