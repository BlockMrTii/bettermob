package eu.northsoft.bettermob.skill.mechanic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class SetNameMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        String name = firstParam(call.params(), "n", "name", "m");
        if (entity == null || name == null) return;
        String text = stripQuotes(name);
        entity.customName(text.isBlank() ? null : LegacyComponentSerializer.legacyAmpersand().deserialize(text));
        entity.setCustomNameVisible(!text.isBlank());
    }
}
