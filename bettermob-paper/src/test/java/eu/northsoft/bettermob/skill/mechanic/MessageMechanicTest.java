package eu.northsoft.bettermob.skill.mechanic;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageMechanicTest {
    @Test
    void theNameIsInsertedAsPlainTextWithoutColorCodesOrPlaceholders() {
        Component template = LegacyComponentSerializer.legacyAmpersand().deserialize("&aHi <caster.name>");
        Component result = MessageMechanic.withName(template, "<caster.name>", "&4[Server] %player_name%");
        assertEquals("Hi &4[Server] %player_name%", PlainTextComponentSerializer.plainText().serialize(result));
    }
}
