package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.util.Tasks;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatPromptsTest {
    @Test
    void chatInputIsPlainAndLimited() {
        assertEquals("Rainbow Road", ChatPrompts.input("  Rainbow Road "));
        assertEquals("abc", ChatPrompts.input("a{b};c"));
        assertEquals(256, ChatPrompts.input("x".repeat(400)).length());
        assertEquals("", Substitutions.withItemNbt("<caster.itemnbt.trackname>", (java.util.function.Function<String, String>) null));
        assertEquals("Track ", Substitutions.withItemNbt("Track <caster.itemnbt.checkpoint>", (java.util.function.Function<String, String>) null));
    }

    @Test
    void onChatDispatchesViaTasksRunOn() {
        SkillEngine engine = mock(SkillEngine.class);
        BetterMobPlugin plugin = mock(BetterMobPlugin.class);
        when(engine.plugin()).thenReturn(plugin);
        ChatPrompts prompts = new ChatPrompts(engine);
        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        LivingEntity caster = mock(LivingEntity.class);
        when(caster.isValid()).thenReturn(true);
        SkillContext context = mock(SkillContext.class);
        when(context.caster()).thenReturn(caster);
        Map<String, String> vars = new HashMap<>();
        when(context.variables()).thenReturn(vars);

        prompts.await(player, "test_skill", context, 100, true);

        AsyncChatEvent event = mock(AsyncChatEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(event.message()).thenReturn(Component.text("hello world"));

        try (MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {
            prompts.onChat(event);
            verify(event).setCancelled(true);
            tasks.verify(() -> Tasks.runOn(eq(plugin), eq(caster), any(Runnable.class)));
        }
    }
}
