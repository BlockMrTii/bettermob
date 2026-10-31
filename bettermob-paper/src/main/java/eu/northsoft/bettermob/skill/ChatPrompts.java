package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.util.Tasks;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatPrompts implements Listener {
    private static final int MAX_INPUT = 256;

    private record Pending(String skill, SkillContext context, long until, boolean cancel) {}

    private final SkillEngine engine;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatPrompts(SkillEngine engine) {
        this.engine = engine;
    }

    public void await(Player player, String skill, SkillContext context, int ticks, boolean cancel) {
        pending.put(player.getUniqueId(), new Pending(skill, context, System.currentTimeMillis() + ticks * 50L, cancel));
    }

    static String input(String message) {
        String text = Params.stripPlaceholderValue(message).trim();
        return text.length() > MAX_INPUT ? text.substring(0, MAX_INPUT) : text;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Pending prompt = pending.remove(player.getUniqueId());
        if (prompt == null || prompt.until() < System.currentTimeMillis()) return;
        if (prompt.cancel()) event.setCancelled(true);
        String text = input(PlainTextComponentSerializer.plainText().serialize(event.message()));
        SkillContext context = prompt.context();
        Tasks.runOn(engine.plugin(), context.caster(), () -> {
            if (!context.caster().isValid()) return;
            context.variables().put("input", text);
            engine.runById(prompt.skill(), context);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }
}
