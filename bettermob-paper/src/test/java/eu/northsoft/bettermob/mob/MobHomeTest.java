package eu.northsoft.bettermob.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class MobHomeTest {
    @Test
    void brokenOrMissingValuesGiveNoHome() {
        assertNull(MobHome.parse(null, null, id -> null));
        assertNull(MobHome.parse("1;2", null, id -> null));
        assertNull(MobHome.parse("a;b;c", null, id -> null));
        assertNull(MobHome.parse("not-a-uuid;1;2;3", null, id -> null));
        assertNull(MobHome.parse("00000000-0000-0000-0000-000000000001;1;2;3", null, id -> null));
    }
}
