package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgumentsTest {
    @Test
    void everyParameterExceptTheReservedOnesBecomesAnArgument() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("s", "heal_pulse");
        params.put("sync", "true");
        params.put("cd", "2");
        params.put("amount", "4");
        params.put("Radius", "6");
        Map<String, String> arguments = Arguments.from(params);
        assertEquals(Map.of("amount", "4", "radius", "6"), arguments);
    }

    @Test
    void givenArgumentsWinOverDefaults() {
        Map<String, String> merged = Arguments.withDefaults(Map.of("amount", "6"), Map.of("amount", "2", "who", "friend"));
        assertEquals("6", merged.get("amount"));
        assertEquals("friend", merged.get("who"));
        Map<String, String> given = Map.of("amount", "1");
        assertTrue(given == Arguments.withDefaults(given, Map.of()));
    }

    @Test
    void placeholdersAreReplacedAndMissingOnesStayAndAreReported() {
        List<String> missing = new ArrayList<>();
        String result = Arguments.resolve("heal <arg.amount> and <arg.Who> <arg.nope>", Map.of("amount", "4", "who", "Rex"), missing::add);
        assertEquals("heal 4 and Rex <arg.nope>", result);
        assertEquals(List.of("nope"), missing);
    }

    @Test
    void argumentValuesCannotInjectSkillSyntax() {
        String result = Arguments.resolve("<arg.x>", Map.of("x", "1} - command{c=op me;a=%p%"), name -> { });
        assertFalse(result.matches(".*[{}\\[\\];=%].*"), result);
    }

    @Test
    void mentionsFindsArgumentPlaceholders() {
        assertTrue(Arguments.mentions(Map.of("a", "x <arg.y>")));
        assertFalse(Arguments.mentions(Map.of("a", "<caster.name>")));
    }

    @Test
    void aContextKeepsItsVariablesWhenArgumentsAreSet() {
        SkillContext context = new SkillContext(null, null, null);
        context.variables().put("hits", "3");
        SkillContext withArguments = context.withArguments(Map.of("amount", "4"));
        assertTrue(context.variables() == withArguments.variables());
        assertEquals("4", withArguments.arguments().get("amount"));
        assertTrue(context.withTrigger(null).arguments().isEmpty());
        assertEquals("4", withArguments.withOrigin(null).arguments().get("amount"));
    }
}
