package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillState;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class ScoreConditionTest {
    @Test
    void disabledConditionReturnsFalseWithoutAccessingScoreboard() {
        SkillEngine engine = mock(SkillEngine.class);
        ScoreCondition condition = new ScoreCondition(engine, false);
        LivingEntity caster = mock(LivingEntity.class);
        SkillContext context = SkillContext.of(caster);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            assertFalse(condition.test(context, "o=kills;v=5", null));
            bukkit.verify(Bukkit::getScoreboardManager, never());
        }
    }

    @Test
    void enabledConditionMatchesScore() {
        SkillEngine engine = mock(SkillEngine.class);
        when(engine.state()).thenReturn(new SkillState());
        ScoreCondition condition = new ScoreCondition(engine, true);
        LivingEntity caster = mock(LivingEntity.class);
        when(caster.getUniqueId()).thenReturn(UUID.randomUUID());
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Bob");
        SkillContext context = SkillContext.of(caster).withTrigger(player);

        ScoreboardManager manager = mock(ScoreboardManager.class);
        Scoreboard scoreboard = mock(Scoreboard.class);
        Objective objective = mock(Objective.class);
        Score score = mock(Score.class);
        when(manager.getMainScoreboard()).thenReturn(scoreboard);
        when(scoreboard.getObjective("kills")).thenReturn(objective);
        when(objective.getScore("Bob")).thenReturn(score);
        when(score.isScoreSet()).thenReturn(true);
        when(score.getScore()).thenReturn(10);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScoreboardManager).thenReturn(manager);
            assertTrue(condition.test(context, "o=kills;v=>=5", null));
            assertFalse(condition.test(context, "o=kills;v=<5", null));
        }
    }

    @Test
    void enabledConditionReturnsFalseWhenScoreNotSet() {
        SkillEngine engine = mock(SkillEngine.class);
        when(engine.state()).thenReturn(new SkillState());
        ScoreCondition condition = new ScoreCondition(engine, true);
        LivingEntity caster = mock(LivingEntity.class);
        when(caster.getUniqueId()).thenReturn(UUID.randomUUID());
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Bob");
        SkillContext context = SkillContext.of(caster).withTrigger(player);

        ScoreboardManager manager = mock(ScoreboardManager.class);
        Scoreboard scoreboard = mock(Scoreboard.class);
        Objective objective = mock(Objective.class);
        Score score = mock(Score.class);
        when(manager.getMainScoreboard()).thenReturn(scoreboard);
        when(scoreboard.getObjective("kills")).thenReturn(objective);
        when(objective.getScore("Bob")).thenReturn(score);
        when(score.isScoreSet()).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScoreboardManager).thenReturn(manager);
            assertFalse(condition.test(context, "o=kills;v=10", null));
        }
    }

    @Test
    void unsupportedOperationExceptionReturnsFalse() {
        SkillEngine engine = mock(SkillEngine.class);
        when(engine.state()).thenReturn(new SkillState());
        ScoreCondition condition = new ScoreCondition(engine, true);
        LivingEntity caster = mock(LivingEntity.class);
        when(caster.getUniqueId()).thenReturn(UUID.randomUUID());
        SkillContext context = SkillContext.of(caster);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScoreboardManager).thenThrow(new UnsupportedOperationException());
            assertFalse(condition.test(context, "o=kills;v=10", null));
        }
    }
}
