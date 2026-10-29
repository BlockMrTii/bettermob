package eu.northsoft.bettermob.skill.mechanic;

import com.google.gson.JsonParseException;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class JsonMessageMechanic implements Mechanic {
    static String jsonString(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.toString();
    }

    @Override
    public void execute(MechanicCall call) {
        String raw = firstParam(call.params(), "m", "message", "msg");
        if (raw == null || !(call.target().entity() instanceof Player receiver)) return;
        String json = stripQuotes(raw)
                .replace("<target.name>", jsonString(receiver.getName()))
                .replace("<caster.name>", jsonString(call.casterName()));
        try {
            receiver.sendMessage(GsonComponentSerializer.gson().deserialize(json));
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException exception) {
            receiver.sendMessage(MessageText.render(raw, receiver, call.casterName()));
        }
    }
}
