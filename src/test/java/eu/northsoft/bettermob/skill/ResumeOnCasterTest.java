package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResumeOnCasterTest {
    private final Plugin plugin = mock(Plugin.class);
    private final LivingEntity caster = mock(LivingEntity.class);
    private final Runnable rest = mock(Runnable.class);

    @Test
    void withoutRegionsTheRestRunsRightAway() {
        SkillEngine.resumeOnCaster(plugin, caster, rest);
        verify(rest).run();
    }

    @Test
    void theRestIsHandedToTheCasterWhileItExists() {
        try (MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {
            SkillEngine.resumeOnCaster(plugin, caster, rest);
            tasks.verify(() -> Tasks.runOwned(eq(plugin), eq(caster), eq(rest), notNull()));
            tasks.verify(() -> Tasks.runAt(any(), any(), any()), never());
        }
    }

    @Test
    void theRestRunsWhereTheCasterWasOnceItIsGone() {
        Location last = new Location(null, 1, 2, 3);
        when(caster.getLocation()).thenReturn(last);
        try (MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {
            tasks.when(() -> Tasks.runOwned(eq(plugin), eq(caster), eq(rest), any())).thenAnswer(call -> {
                call.<Runnable>getArgument(3).run();
                return false;
            });
            SkillEngine.resumeOnCaster(plugin, caster, rest);
            tasks.verify(() -> Tasks.runAt(plugin, last, rest));
        }
    }
}
