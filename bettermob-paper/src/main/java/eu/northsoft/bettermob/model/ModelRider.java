package eu.northsoft.bettermob.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record ModelRider(UUID id, String seat) {
    public static List<UUID> select(Collection<ModelRider> riders, String seat) {
        String wanted = seat == null || seat.isBlank() ? null : seat.trim().toLowerCase(Locale.ROOT);
        Set<UUID> found = new LinkedHashSet<>();
        for (ModelRider rider : riders) {
            if (wanted == null || rider.seat() != null && rider.seat().toLowerCase(Locale.ROOT).equals(wanted)) found.add(rider.id());
        }
        return new ArrayList<>(found);
    }
}
