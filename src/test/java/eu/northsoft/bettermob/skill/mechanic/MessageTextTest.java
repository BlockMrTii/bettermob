package eu.northsoft.bettermob.skill.mechanic;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageTextTest {
    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void namesAreInsertedAsPlainTextAfterTheColoursWereParsed() {
        Component template = LegacyComponentSerializer.legacyAmpersand().deserialize("&a<caster.name> hits <target.name>");
        Component result = MessageText.withName(MessageText.withName(template, "<caster.name>", "&cEvil&r"), "<target.name>", "Steve");
        assertEquals("&cEvil&r hits Steve", plain(result));
    }

    @Test
    void tickParsingFallsBackAndNeverGoesNegative() {
        assertEquals(40, MessageText.ticks("40", 10));
        assertEquals(10, MessageText.ticks(null, 10));
        assertEquals(10, MessageText.ticks("abc", 10));
        assertEquals(0, MessageText.ticks("-5", 10));
    }

    @Test
    void bossBarColoursAndStylesAcceptTheBukkitNames() {
        assertEquals(BossBar.Color.RED, BossBarMechanic.color("red"));
        assertEquals(BossBar.Color.PURPLE, BossBarMechanic.color("orange"));
        assertEquals(BossBar.Color.PURPLE, BossBarMechanic.color(null));
        assertEquals(BossBar.Overlay.NOTCHED_10, BossBarMechanic.overlay("SEGMENTED_10"));
        assertEquals(BossBar.Overlay.NOTCHED_6, BossBarMechanic.overlay("notched_6"));
        assertEquals(BossBar.Overlay.PROGRESS, BossBarMechanic.overlay("solid"));
    }
}
