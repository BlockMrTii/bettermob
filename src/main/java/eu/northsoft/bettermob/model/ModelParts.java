package eu.northsoft.bettermob.model;

import java.util.Locale;

public final class ModelParts {
    private ModelParts() {}

    public static boolean matches(String bone, String part, boolean exact) {
        if (part == null || part.isBlank()) return true;
        if (bone == null) return false;
        String wanted = part.trim().toLowerCase(Locale.ROOT);
        String name = bone.toLowerCase(Locale.ROOT);
        return exact ? name.equals(wanted) : name.contains(wanted);
    }
}
