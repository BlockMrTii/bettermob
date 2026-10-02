package eu.northsoft.bettermob;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

/**
 * Reflection-Hook, damit BetterMob auch ohne installierte BetterModel-API laden kann.
 * Klassen/Methoden werden einmal pro Name lazy aufgeloest und dann wiederverwendet
 * (statt bei jedem attach()/play()-Aufruf per Class.forName()+getMethod() neu gesucht zu
 * werden) - "state"-Skills koennen ueber ~onTimer sehr oft pro Sekunde laufen, da faellt
 * eine wiederholte Reflection-Methodensuche spuerbar ins Gewicht.
 */
final class BetterModelHook {
    private static final String API = "kr.toxicity.model.api.BetterModel";
    private static final String ADAPTER = "kr.toxicity.model.api.bukkit.platform.BukkitAdapter";
    private static final String PLATFORM_ENTITY = "kr.toxicity.model.api.platform.PlatformEntity";
    private static final String MODIFIER = "kr.toxicity.model.api.animation.AnimationModifier";
    private static final String ITERATOR_TYPE = "kr.toxicity.model.api.animation.AnimationIterator$Type";

    private final BetterMobPlugin plugin;

    // Lazy, einmal pro Prozesslaufzeit aufgeloest - "available()" wird weiterhin frisch
    // geprueft, aber die eigentlichen Method-Objekte muessen nicht jedes Mal neu gesucht werden.
    private Method modelOrNullMethod;
    private Method adaptMethod;
    private Class<?> platformEntityClass;
    private Object playOnceModifier;
    private Method animateMethod;
    private Class<?> modifierClass;

    BetterModelHook(BetterMobPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Frisch bei jedem Aufruf geprueft statt einmal im Konstruktor gecacht: softdepend
     * sortiert die Ladereihenfolge nur, garantiert aber nicht, dass BetterModel beim
     * BetterMob-onEnable schon fertig aktiviert ist - ein einmal auf false gecachtes
     * Flag wuerde dann bis zum naechsten Neustart falsch bleiben.
     */
    boolean available() {
        return Bukkit.getPluginManager().isPluginEnabled("BetterModel") && classExists(API);
    }

    /** Wie attach(), aber ohne Warnung wenn es das Modell nicht gibt - fuer das automatische
     *  Anhaengen ueber den Mob-Namen, das bei Mobs ohne eigenes Modell (z.B. Armor-Stand-Karten) normal ins Leere laeuft. */
    Object attachIfPresent(Entity entity, String modelId) {
        if (!available()) return null;
        try {
            if (modelOrNullMethod().invoke(null, modelId) == null) return null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            return null;
        }
        return attach(entity, modelId);
    }

    Object attach(Entity entity, String modelId) {
        if (!Bukkit.getPluginManager().isPluginEnabled("BetterModel")) {
            plugin.getLogger().warning("BetterModel ist beim Spawn von '" + modelId + "' nicht aktiv - kein Modell angehaengt.");
            return null;
        }
        if (!classExists(API)) {
            plugin.getLogger().warning("BetterModel-API-Klasse '" + API + "' nicht gefunden - passt die installierte BetterModel-Version?");
            return null;
        }
        try {
            Object renderer = modelOrNullMethod().invoke(null, modelId);
            if (renderer == null) {
                plugin.getLogger().warning("BetterModel-Modell '" + modelId + "' existiert nicht.");
                return null;
            }
            Object adapted = adaptMethod().invoke(null, entity);
            return renderer.getClass().getMethod("getOrCreate", platformEntityClass()).invoke(renderer, adapted);
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("BetterModel '" + modelId + "' konnte nicht angehängt werden: " + rootMessage(exception));
            return null;
        }
    }

    /** Fuer den "state"-Skill-Mechanic: spielt eine BetterModel-Animation einmal ab. */
    boolean play(Object tracker, String animation) {
        if (tracker == null || !available()) return false;
        try {
            Object modifier = playOnceModifier();
            Method method = animateMethod(tracker);
            Object result;
            try {
                result = method.invoke(tracker, animation, modifier);
            } catch (IllegalArgumentException mismatch) {
                // Der gecachte Method-Handle stammt von einer anderen Tracker-Implementierung
                // (z.B. verschiedene Modell-Typen liefern unterschiedliche Klassen) - einmal
                // frisch fuer DIESEN Tracker aufloesen statt dauerhaft kaputt zu bleiben.
                method = tracker.getClass().getMethod("animate", String.class, modifierClass());
                result = method.invoke(tracker, animation, modifier);
            }
            return result instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("BetterModel-Animation '" + animation + "' fehlgeschlagen: " + rootMessage(exception));
            return false;
        }
    }

    private Method modelOrNullMethod() throws ReflectiveOperationException {
        if (modelOrNullMethod == null) modelOrNullMethod = Class.forName(API).getMethod("modelOrNull", String.class);
        return modelOrNullMethod;
    }

    private Method adaptMethod() throws ReflectiveOperationException {
        if (adaptMethod == null) adaptMethod = Class.forName(ADAPTER).getMethod("adapt", Entity.class);
        return adaptMethod;
    }

    private Class<?> platformEntityClass() throws ReflectiveOperationException {
        if (platformEntityClass == null) platformEntityClass = Class.forName(PLATFORM_ENTITY);
        return platformEntityClass;
    }

    private Class<?> modifierClass() throws ReflectiveOperationException {
        if (modifierClass == null) modifierClass = Class.forName(MODIFIER);
        return modifierClass;
    }

    /** AnimationModifier.builder().type(PLAY_ONCE).build() ist immer dasselbe Ergebnis - einmal bauen, immer wiederverwenden. */
    private Object playOnceModifier() throws ReflectiveOperationException {
        if (playOnceModifier != null) return playOnceModifier;
        Class<?> modifier = modifierClass();
        Class<?> typeClass = Class.forName(ITERATOR_TYPE);
        Object builder = modifier.getMethod("builder").invoke(null);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Object type = Enum.valueOf((Class<? extends Enum>) typeClass, "PLAY_ONCE");
        builder.getClass().getMethod("type", typeClass).invoke(builder, type);
        playOnceModifier = builder.getClass().getMethod("build").invoke(builder);
        return playOnceModifier;
    }

    private Method animateMethod(Object tracker) throws ReflectiveOperationException {
        if (animateMethod == null) animateMethod = tracker.getClass().getMethod("animate", String.class, modifierClass());
        return animateMethod;
    }

    /**
     * Fuer den "mountmodel"-Skill-Mechanic: setzt den Rider auf eine benannte Bone-Hitbox
     * (Sitz). Die genauen Klassennamen von HitBox/MountControllers sind aus der Doku nicht
     * 1:1 bekannt, deshalb werden Methoden/Konstanten defensiv ueber den tatsaechlichen
     * Parametertyp aufgeloest statt eine fest verdrahtete, moeglicherweise falsche
     * vollqualifizierte Klasse zu erraten - schlaegt ein Schritt fehl, gibt's eine
     * konkrete Logzeile statt eines stillen Nichts-Passiert.
     */
    /**
     * Weltposition eines Bones (z.B. der Bombe am Modell): BetterModel liefert sie relativ zum Modell-Ursprung
     * (Vector3f in Bloecken), also auf die Tracker-Position addiert. null, wenn der Bone fehlt.
     */
    org.bukkit.Location bonePosition(Object tracker, String boneName) {
        if (tracker == null) return null;
        try {
            Object bone = tracker.getClass().getMethod("bone", String.class).invoke(tracker, boneName);
            if (bone == null) {
                plugin.getLogger().warning("@ModelPart: Bone '" + boneName + "' existiert nicht am Modell.");
                return null;
            }
            Object offset = bone.getClass().getMethod("worldPosition").invoke(bone);
            org.bukkit.Location origin = (org.bukkit.Location) tracker.getClass().getMethod("location").invoke(tracker);
            Class<?> vector = offset.getClass();
            double x = ((Number) vector.getField("x").get(offset)).doubleValue();
            double y = ((Number) vector.getField("y").get(offset)).doubleValue();
            double z = ((Number) vector.getField("z").get(offset)).doubleValue();
            plugin.getLogger().info("@ModelPart " + boneName + ": Offset " + String.format("%.2f %.2f %.2f", x, y, z)
                    + " ab " + origin.getBlockX() + " " + origin.getBlockY() + " " + origin.getBlockZ());
            return origin.clone().add(x, y, z);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("@ModelPart '" + boneName + "' fehlgeschlagen: " + exception);
            return null;
        }
    }

    boolean mount(Object tracker, String seat, Entity rider) {
        if (tracker == null) return false;
        try {
            Object bone = tracker.getClass().getMethod("bone", String.class).invoke(tracker, seat);
            if (bone == null) {
                plugin.getLogger().warning("mountmodel: Bone/Sitz '" + seat + "' existiert nicht an diesem Modell.");
                return false;
            }
            Object hitBox = bone.getClass().getMethod("getHitBox").invoke(bone);
            if (hitBox == null) {
                plugin.getLogger().warning("mountmodel: Bone '" + seat + "' hat keine Hitbox zum Draufsitzen.");
                return false;
            }
            allowControl(hitBox);

            Object adapted = adaptMethod().invoke(null, rider);
            Method mountMethod = findMethod(hitBox.getClass(), "mount", 1);
            if (mountMethod == null) {
                plugin.getLogger().warning("mountmodel: HitBox-Klasse '" + hitBox.getClass().getName() + "' hat keine mount(...)-Methode.");
                return false;
            }
            mountMethod.invoke(hitBox, adapted);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("mountmodel Sitz '" + seat + "' fehlgeschlagen: " + rootMessage(exception));
            return false;
        }
    }

    /** Ohne das bleibt der Sitz nur dekorativ - "CONTROL" erlaubt dem Rider, das Modell wirklich zu steuern. */
    private void allowControl(Object hitBox) {
        Method method = findMethod(hitBox.getClass(), "mountController", 1);
        if (method == null) return;
        Object control = resolveConstant(method.getParameterTypes()[0], "CONTROL");
        if (control == null) {
            plugin.getLogger().warning("mountmodel: Steuerungs-Konstante 'CONTROL' fuer " + method.getParameterTypes()[0].getName() + " nicht gefunden - Sitz bleibt ohne Lenkung.");
            return;
        }
        try {
            method.invoke(hitBox, control);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("mountmodel: Steuerungsmodus konnte nicht gesetzt werden: " + rootMessage(exception));
        }
    }

    private static Object resolveConstant(Class<?> type, String name) {
        if (type.isEnum()) {
            for (Object constant : type.getEnumConstants()) if (constant.toString().equalsIgnoreCase(name)) return constant;
        }
        try {
            return type.getField(name).get(null);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            return Class.forName(type.getName() + "s").getField(name).get(null);
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name, int paramCount) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == paramCount) return method;
        }
        return null;
    }

    void close(Object tracker) {
        if (tracker == null) return;
        try {
            tracker.getClass().getMethod("close").invoke(tracker);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
