package eu.northsoft.bettermob.debug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AggroViewTest {
    @Test
    void aLineHasOnePointPerStepButNeverMoreThanTheCap() {
        assertEquals(1, AggroView.points(0.1));
        assertEquals(4, AggroView.points(3));
        assertEquals(AggroView.MAX_POINTS, AggroView.points(500));
    }
}
