package eu.northsoft.bettermob.skill.condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConditionParamsTest {
    @Test
    void textWithoutAPlaceholderIsLeftAlone() {
        assertEquals("h=<50%", ConditionParams.papi("h=<50%", key -> "x"));
        assertEquals("v=a b", ConditionParams.papi("v=a b", key -> "x"));
    }

    @Test
    void valuesAreInsertedWithoutSkillSyntaxAndUnknownOnesBecomeEmpty() {
        assertEquals("val1=\"Yes\";val2=\"Yes\"", ConditionParams.papi("val1=\"<placeholder.Cubees-Extension-1>\";val2=\"Yes\"", key -> "Yes"));
        assertEquals("a=x b=", ConditionParams.papi("a=<placeholder.one> b=<placeholder.two>", key -> key.equals("one") ? "{x};" : null));
        assertEquals("a=", ConditionParams.papi("a=<placeholder.one>", key -> "%one%"));
    }
}
