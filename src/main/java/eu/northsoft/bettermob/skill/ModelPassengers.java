package eu.northsoft.bettermob.skill;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityRemoveEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ModelPassengers implements Listener {
    private final Map<UUID, Map<String, Set<UUID>>> bySeat = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> ownerOfRider = new ConcurrentHashMap<>();
    private final Map<UUID, String> seatOfRider = new ConcurrentHashMap<>();

    public void add(UUID owner, String seat, UUID rider) {
        remove(rider);
        String key = seat.toLowerCase(Locale.ROOT);
        bySeat.computeIfAbsent(owner, id -> new ConcurrentHashMap<>()).computeIfAbsent(key, name -> ConcurrentHashMap.newKeySet()).add(rider);
        ownerOfRider.put(rider, owner);
        seatOfRider.put(rider, key);
    }

    public void remove(UUID rider) {
        UUID owner = ownerOfRider.remove(rider);
        String seat = seatOfRider.remove(rider);
        if (owner == null || seat == null) return;
        bySeat.computeIfPresent(owner, (id, seats) -> {
            seats.computeIfPresent(seat, (name, riders) -> {
                riders.remove(rider);
                return riders.isEmpty() ? null : riders;
            });
            return seats.isEmpty() ? null : seats;
        });
    }

    public List<UUID> riders(UUID owner, String seat) {
        Map<String, Set<UUID>> seats = bySeat.get(owner);
        if (seats == null) return List.of();
        if (seat == null || seat.isBlank()) {
            Set<UUID> all = new LinkedHashSet<>();
            seats.values().forEach(all::addAll);
            return new ArrayList<>(all);
        }
        Set<UUID> riders = seats.get(seat.toLowerCase(Locale.ROOT));
        return riders == null ? List.of() : new ArrayList<>(riders);
    }

    public UUID ownerOf(UUID rider) {
        return ownerOfRider.get(rider);
    }

    public List<UUID> clear(UUID owner) {
        List<UUID> riders = riders(owner, null);
        riders.forEach(this::remove);
        return riders;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        if (event.getCause() == EntityRemoveEvent.Cause.UNLOAD) return;
        UUID id = event.getEntity().getUniqueId();
        remove(id);
        clear(id);
    }
}
