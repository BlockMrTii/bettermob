package eu.northsoft.bettermob.mob;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ThreatTables {
    private static final class Threat {
        final WeakReference<LivingEntity> attacker;
        final String name;
        double amount;

        Threat(LivingEntity attacker) {
            this.attacker = new WeakReference<>(attacker);
            this.name = attacker.getName();
        }
    }

    private final Map<UUID, Map<UUID, Threat>> tables = new ConcurrentHashMap<>();

    void track(UUID mobId) {
        tables.putIfAbsent(mobId, new ConcurrentHashMap<>());
    }

    void untrack(UUID mobId) {
        tables.remove(mobId);
    }

    void register(UUID mobId, LivingEntity attacker, double amount) {
        Map<UUID, Threat> table = tables.get(mobId);
        if (table != null) table.computeIfAbsent(attacker.getUniqueId(), id -> new Threat(attacker)).amount += amount;
    }

    List<Map.Entry<String, Double>> top(UUID mobId, int limit) {
        Map<UUID, Threat> table = tables.get(mobId);
        if (table == null) return List.of();
        List<Map.Entry<String, Double>> rows = new ArrayList<>();
        for (Threat threat : table.values()) {
            if (threat.attacker.get() != null) rows.add(Map.entry(threat.name, threat.amount));
        }
        rows.sort(Map.Entry.<String, Double>comparingByValue().reversed());
        return rows.size() > limit ? List.copyOf(rows.subList(0, limit)) : rows;
    }

    void retarget(Mob mob) {
        Map<UUID, Threat> table = tables.get(mob.getUniqueId());
        if (table == null || !mob.isValid()) return;

        LivingEntity top = null;
        double best = -1;
        var iterator = table.values().iterator();
        while (iterator.hasNext()) {
            Threat threat = iterator.next();
            LivingEntity attacker = threat.attacker.get();
            if (attacker == null) {
                iterator.remove();
                continue;
            }
            if (!Bukkit.isOwnedByCurrentRegion(attacker)) continue;
            if (attacker.isDead() || !attacker.getWorld().equals(mob.getWorld())) {
                iterator.remove();
                continue;
            }
            if (threat.amount > best) {
                best = threat.amount;
                top = attacker;
            }
        }
        if (top != null) mob.setTarget(top);
    }
}
