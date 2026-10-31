package eu.northsoft.bettermob.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BehaviourGoalsTest {
    @Test
    void aLoopingPatrolWrapsAroundAfterTheLastPoint() {
        assertEquals(1, BehaviourGoals.nextIndex(0, 3, true));
        assertEquals(2, BehaviourGoals.nextIndex(1, 3, true));
        assertEquals(0, BehaviourGoals.nextIndex(2, 3, true));
    }

    @Test
    void aOneWayPatrolEndsBehindTheLastPoint() {
        assertEquals(3, BehaviourGoals.nextIndex(2, 3, false));
        assertEquals(1, BehaviourGoals.nextIndex(0, 3, false));
        assertEquals(0, BehaviourGoals.nextIndex(0, 0, true));
    }
}
