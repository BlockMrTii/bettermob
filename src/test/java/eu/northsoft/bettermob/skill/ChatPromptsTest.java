package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatPromptsTest {
    @Test
    void chatInputIsPlainAndLimited() {
        assertEquals("Rainbow Road", ChatPrompts.input("  Rainbow Road "));
        assertEquals("abc", ChatPrompts.input("a{b};c"));
        assertEquals(256, ChatPrompts.input("x".repeat(400)).length());
        assertEquals("", Substitutions.withItemNbt("<caster.itemnbt.trackname>", null));
        assertEquals("Track ", Substitutions.withItemNbt("Track <caster.itemnbt.checkpoint>", null));
    }
}
