package eu.northsoft.bettermob.skill.mechanic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandMechanicTest {
    @Test
    void asArgumentKeepsTheNameASingleHarmlessToken() {
        assertEquals("Bob_parent_add_admin", CommandMechanic.asArgument("Bob parent add admin"));
        assertEquals("Bobplayer", CommandMechanic.asArgument("Bob%player%"));
        assertEquals("Steve_1", CommandMechanic.asArgument("Steve_1"));
    }
}
