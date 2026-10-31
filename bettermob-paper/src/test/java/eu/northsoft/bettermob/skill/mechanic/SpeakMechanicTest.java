package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import eu.northsoft.bettermob.util.Tasks;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpeakMechanicTest {
    @Test
    void chatLineIsScheduledViaRunOwnedAndSentOnlyOnExecution() {
        SkillEngine engine = mock(SkillEngine.class);
        BetterMobPlugin plugin = mock(BetterMobPlugin.class);
        when(engine.plugin()).thenReturn(plugin);

        World world = mock(World.class);
        TextDisplay display = mock(TextDisplay.class);
        when(world.spawn(any(Location.class), eq(TextDisplay.class), any(Consumer.class))).thenAnswer(invocation -> {
            Consumer<TextDisplay> argument = invocation.getArgument(2);
            if (argument != null) {
                argument.accept(display);
            }
            return display;
        });

        LivingEntity caster = mock(LivingEntity.class);
        when(caster.getWorld()).thenReturn(world);
        when(caster.getUniqueId()).thenReturn(UUID.randomUUID());
        when(caster.getName()).thenReturn("Boss");
        when(caster.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(caster.getHeight()).thenReturn(2.0);
        when(caster.getTicksLived()).thenReturn(10);

        Player receiver = mock(Player.class);
        when(receiver.getName()).thenReturn("Alice");

        SkillContext context = SkillContext.of(caster);
        SpeakMechanic mechanic = new SpeakMechanic(engine);
        MechanicCall call = new MechanicCall(null, context, Target.ofEntity(receiver), Map.of("m", "Hello <target.name>"), "Boss");

        AtomicReference<Runnable> scheduledTask = new AtomicReference<>();
        try (MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {
            tasks.when(() -> Tasks.runOwned(eq(plugin), eq(receiver), any(Runnable.class))).thenAnswer(invocation -> {
                scheduledTask.set(invocation.getArgument(2));
                return false;
            });

            mechanic.execute(call);

            verify(receiver, never()).sendMessage(any(Component.class));
            assertNotNull(scheduledTask.get());

            scheduledTask.get().run();
            verify(receiver, times(1)).sendMessage(any(Component.class));
        }
    }

    @Test
    void chatFalseDoesNotScheduleChatMessage() {
        SkillEngine engine = mock(SkillEngine.class);
        BetterMobPlugin plugin = mock(BetterMobPlugin.class);
        when(engine.plugin()).thenReturn(plugin);

        World world = mock(World.class);
        TextDisplay display = mock(TextDisplay.class);
        when(world.spawn(any(Location.class), eq(TextDisplay.class), any(Consumer.class))).thenReturn(display);

        LivingEntity caster = mock(LivingEntity.class);
        when(caster.getWorld()).thenReturn(world);
        when(caster.getUniqueId()).thenReturn(UUID.randomUUID());
        when(caster.getName()).thenReturn("Boss");
        when(caster.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(caster.getHeight()).thenReturn(2.0);
        when(caster.getTicksLived()).thenReturn(10);

        Player receiver = mock(Player.class);
        SkillContext context = SkillContext.of(caster);
        SpeakMechanic mechanic = new SpeakMechanic(engine);
        MechanicCall call = new MechanicCall(null, context, Target.ofEntity(receiver), Map.of("m", "Quietly", "chat", "false"), "Boss");

        try (MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {
            mechanic.execute(call);

            tasks.verify(() -> Tasks.runOwned(any(), eq(receiver), any(Runnable.class)), never());
            verify(receiver, never()).sendMessage(any(Component.class));
        }
    }

    @Test
    void nonPlayerTargetDoesNotScheduleChatMessage() {
        SkillEngine engine = mock(SkillEngine.class);
        BetterMobPlugin plugin = mock(BetterMobPlugin.class);
        when(engine.plugin()).thenReturn(plugin);

        World world = mock(World.class);
        TextDisplay display = mock(TextDisplay.class);
        when(world.spawn(any(Location.class), eq(TextDisplay.class), any(Consumer.class))).thenReturn(display);

        LivingEntity caster = mock(LivingEntity.class);
        when(caster.getWorld()).thenReturn(world);
        when(caster.getUniqueId()).thenReturn(UUID.randomUUID());
        when(caster.getName()).thenReturn("Boss");
        when(caster.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(caster.getHeight()).thenReturn(2.0);
        when(caster.getTicksLived()).thenReturn(10);

        LivingEntity nonPlayer = mock(LivingEntity.class);
        SkillContext context = SkillContext.of(caster);
        SpeakMechanic mechanic = new SpeakMechanic(engine);
        MechanicCall call = new MechanicCall(null, context, Target.ofEntity(nonPlayer), Map.of("m", "Quietly"), "Boss");

        try (MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {
            mechanic.execute(call);

            tasks.verify(() -> Tasks.runOwned(any(), any(Player.class), any(Runnable.class)), never());
        }
    }

    @Test
    void runsOnTargetReturnsFalse() {
        SkillEngine engine = mock(SkillEngine.class);
        SpeakMechanic mechanic = new SpeakMechanic(engine);
        assertFalse(mechanic.runsOnTarget());
    }
}
