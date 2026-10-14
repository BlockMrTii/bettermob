package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CalculatorTest {
    @Test
    void followsOperatorPrecedenceAndParentheses() {
        assertEquals(7.0, Calculator.evaluate("1 + 2 * 3"));
        assertEquals(9.0, Calculator.evaluate("(1 + 2) * 3"));
        assertEquals(0.5, Calculator.evaluate("1 / 2"));
        assertEquals(1.0, Calculator.evaluate("7 % 3"));
        assertEquals(-3.0, Calculator.evaluate("-(1 + 2)"));
        assertEquals(5.5, Calculator.evaluate("5.0 + (5.0 - 5.0) * 0.1 + 0.5"));
    }

    @Test
    void theCountdownEquationFromTheMinekartsPackWorks() {
        assertEquals(2.0 + (5.0 - 2.0) * 0.1, Calculator.evaluate("2 + (5.0 - 2) * 0.1"));
    }

    @Test
    void rejectsAnythingThatIsNotPlainArithmetic() {
        assertNull(Calculator.evaluate(""));
        assertNull(Calculator.evaluate("1 / 0"));
        assertNull(Calculator.evaluate("1 +"));
        assertNull(Calculator.evaluate("(1 + 2"));
        assertNull(Calculator.evaluate("1 + abc"));
        assertNull(Calculator.evaluate("Math.abs(1)"));
        assertNull(Calculator.evaluate("1".repeat(300)));
        assertNull(Calculator.evaluate("(".repeat(100) + "1" + ")".repeat(100)));
    }
}
