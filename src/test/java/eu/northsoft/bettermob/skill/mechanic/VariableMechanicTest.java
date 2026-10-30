package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillState;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VariableMechanicTest {
    @Test
    void concurrentAddDoesNotLoseUpdates() throws InterruptedException {
        SkillEngine engine = mock(SkillEngine.class);
        BetterMobPlugin plugin = mock(BetterMobPlugin.class);
        when(engine.plugin()).thenReturn(plugin);
        SkillState state = new SkillState();
        when(engine.state()).thenReturn(state);

        LivingEntity caster = mock(LivingEntity.class);
        UUID casterId = UUID.randomUUID();
        when(caster.getUniqueId()).thenReturn(casterId);

        SkillContext context = SkillContext.of(caster);
        VariableMechanic mechanic = new VariableMechanic(engine, VariableMechanic.Mode.ADD);
        MechanicCall call = new MechanicCall(null, context, Target.ofEntity(caster), Map.of("var", "caster.counter", "val", "1"), "caster");

        int threadCount = 8;
        int incrementsPerThread = 250;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < incrementsPerThread; i++) {
                        mechanic.execute(call);
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        try {
            startLatch.countDown();
            assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        Map<String, String> scope = state.existingVariablesOf(casterId);
        assertEquals(String.valueOf(threadCount * incrementsPerThread), scope.get("counter"));
    }
}
