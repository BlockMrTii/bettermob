package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.integration.PlaceholderHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import static eu.northsoft.bettermob.skill.Params.stripQuotes;

final class MessageText {
    private MessageText() {}

    static Component render(String raw, Player receiver, String casterName) {
        Component template = LegacyComponentSerializer.legacyAmpersand().deserialize(PlaceholderHook.apply(receiver, stripQuotes(raw)));
        return withName(withName(template, "<caster.name>", casterName), "<target.name>", receiver.getName());
    }

    static Component withName(Component template, String token, String name) {
        return template.replaceText(builder -> builder.matchLiteral(token).replacement(Component.text(name)));
    }

    static int ticks(String value, int fallback) {
        if (value == null) return fallback;
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
