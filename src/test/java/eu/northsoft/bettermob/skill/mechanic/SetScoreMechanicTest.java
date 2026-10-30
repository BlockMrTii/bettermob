package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetScoreMechanicTest {
    @Test
    void disabledMechanicDoesNotAccessScoreboard() {
        SetScoreMechanic mechanic = new SetScoreMechanic(false);
        LivingEntity caster = mock(LivingEntity.class);
        Player target = mock(Player.class);
        MechanicCall call = new MechanicCall(null, SkillContext.of(caster), Target.ofEntity(target), Map.of("objective", "kills", "value", "5"), "caster");
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            mechanic.execute(call);
            bukkit.verify(Bukkit::getScoreboardManager, never());
        }
    }

    @Test
    void enabledMechanicSetsScoreForPlayer() {
        SetScoreMechanic mechanic = new SetScoreMechanic(true);
        LivingEntity caster = mock(LivingEntity.class);
        Player target = mock(Player.class);
        when(target.getName()).thenReturn("Alice");
        MechanicCall call = new MechanicCall(null, SkillContext.of(caster), Target.ofEntity(target), Map.of("objective", "kills", "value", "5"), "caster");
        ScoreboardManager manager = mock(ScoreboardManager.class);
        Scoreboard scoreboard = mock(Scoreboard.class);
        Objective objective = mock(Objective.class);
        Score score = mock(Score.class);
        when(manager.getMainScoreboard()).thenReturn(scoreboard);
        when(scoreboard.getObjective("kills")).thenReturn(objective);
        when(objective.getScore("Alice")).thenReturn(score);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScoreboardManager).thenReturn(manager);
            mechanic.execute(call);
            verify(score).setScore(5);
        }
    }

    @Test
    void enabledMechanicSetsScoreForNonPlayerEntityByUuid() {
        SetScoreMechanic mechanic = new SetScoreMechanic(true);
        LivingEntity caster = mock(LivingEntity.class);
        Entity target = mock(Entity.class);
        UUID id = UUID.randomUUID();
        when(target.getUniqueId()).thenReturn(id);
        MechanicCall call = new MechanicCall(null, SkillContext.of(caster), Target.ofEntity(target), Map.of("obj", "test_obj", "v", "12"), "caster");
        ScoreboardManager manager = mock(ScoreboardManager.class);
        Scoreboard scoreboard = mock(Scoreboard.class);
        Objective objective = mock(Objective.class);
        Score score = mock(Score.class);
        when(manager.getMainScoreboard()).thenReturn(scoreboard);
        when(scoreboard.getObjective("test_obj")).thenReturn(objective);
        when(objective.getScore(id.toString())).thenReturn(score);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScoreboardManager).thenReturn(manager);
            mechanic.execute(call);
            verify(score).setScore(12);
        }
    }

    @Test
    void unsupportedOperationExceptionIsIgnored() {
        SetScoreMechanic mechanic = new SetScoreMechanic(true);
        LivingEntity caster = mock(LivingEntity.class);
        Player target = mock(Player.class);
        MechanicCall call = new MechanicCall(null, SkillContext.of(caster), Target.ofEntity(target), Map.of("o", "kills", "v", "1"), "caster");
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScoreboardManager).thenThrow(new UnsupportedOperationException());
            assertDoesNotThrow(() -> mechanic.execute(call));
        }
    }
}
