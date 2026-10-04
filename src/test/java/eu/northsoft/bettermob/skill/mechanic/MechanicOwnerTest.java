package eu.northsoft.bettermob.skill.mechanic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MechanicOwnerTest {
    @Test
    void mechanicsThatChangeTheCasterOrTheEventStayOnTheCallingRegion() {
        assertFalse(new CancelEventMechanic().runsOnTarget());
        assertFalse(new LookMechanic().runsOnTarget());
        assertFalse(new LungeMechanic().runsOnTarget());
        assertFalse(new TeleportMechanic().runsOnTarget());
    }

    @Test
    void mechanicsThatChangeTheTargetRunOnTheTargetRegion() {
        assertTrue(((Mechanic) call -> { }).runsOnTarget());
    }
}
