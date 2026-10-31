package eu.northsoft.bettermob.mob;

import java.util.Locale;

public final class EquipmentSlots {
    private EquipmentSlots() {}

    public static boolean isKnownSlot(String name) {
        if (name == null) return false;
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "hand", "mainhand", "main_hand", "offhand", "off_hand", "head", "helmet", "chest", "chestplate",
                    "legs", "leggings", "feet", "boots" -> true;
            default -> false;
        };
    }
}
