package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.api.CustomMechanic;
import eu.northsoft.bettermob.api.MechanicContext;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class MechanicRegistry {
    private record Entry(Plugin owner, boolean custom, Mechanic mechanic) {}

    private static final Set<String> HANDLED_BY_ENGINE = Set.of("cancelskill", "delay");

    private final BetterMobPlugin plugin;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final List<Mechanic> builtins = new ArrayList<>();

    public MechanicRegistry(BetterMobPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(Mechanic mechanic, String... names) {
        builtins.add(mechanic);
        for (String name : names) entries.put(name, new Entry(null, false, mechanic));
    }

    public List<Mechanic> builtins() {
        return builtins;
    }

    public Mechanic get(String name) {
        Entry entry = entries.get(name);
        return entry == null ? null : entry.mechanic();
    }

    public boolean registerCustom(Plugin owner, String name, CustomMechanic custom) {
        String key = name.toLowerCase(Locale.ROOT);
        if (HANDLED_BY_ENGINE.contains(key)) return false;
        Mechanic mechanic = call -> {
            try {
                custom.execute(new MechanicContext(call.context().caster(), call.context().trigger(), call.context().event(),
                        call.target().entity(), call.target().location(), call.params()));
            } catch (RuntimeException exception) {
                plugin.messages().warn("skill.customMechanicFailed", "mechanic", call.step().name(), "plugin", owner.getName(), "error", exception);
            }
        };
        return entries.putIfAbsent(key, new Entry(owner, true, mechanic)) == null;
    }

    public void unregisterCustom(String name) {
        entries.computeIfPresent(name.toLowerCase(Locale.ROOT), (key, entry) -> entry.custom() ? null : entry);
    }

    public void unregisterCustom(Plugin owner) {
        entries.values().removeIf(entry -> entry.custom() && Objects.equals(entry.owner(), owner));
    }
}
