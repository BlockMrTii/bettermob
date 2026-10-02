package eu.northsoft.bettermob;

import eu.northsoft.bettermob.api.CustomMechanic;
import eu.northsoft.bettermob.api.MechanicContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fuehrt SkillDefinitions aus - ein kleiner, auf die tatsaechlich gebrauchten Mechaniken
 * beschraenkter Nachbau von MythicMobs' Skill-System. Baut nur die Mechaniken/Conditions
 * ein, die im Pack vorkommen; unbekannte Mechanics/Conditions werden geloggt statt den
 * Server zum Absturz zu bringen.
 */
final class SkillEngine {
    private static final Pattern CONDITION_PATTERN = Pattern.compile("^(\\w+)(\\{([^}]*)})?(\\s+(\\S+))?(\\s+(\\S+))?$");

    private final BetterMobPlugin plugin;
    private final SkillRegistry registry;
    private final MobManager mobManager;
    private final BetterModelHook betterModel;
    private final ModelEngineHook modelEngine;
    private final ItemRegistry items;
    private final Map<UUID, Long> gcdUntilMillis = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, CustomMechanicEntry> customMechanics = new ConcurrentHashMap<>();

    private static final Set<String> BUILTIN_MECHANICS = Set.of("cancelskill", "cancelevent", "skill", "look", "sound",
            "state", "potion", "breakblock", "gcd", "model", "modelengine", "randomskill", "remove", "command",
            "summon", "mountmodel", "delay", "effect:particles", "e:p", "particles", "effect:particlering", "spin", "takeitem", "sudoskill");

    private record CustomMechanicEntry(Plugin owner, CustomMechanic mechanic) {}

    SkillEngine(BetterMobPlugin plugin, SkillRegistry registry, MobManager mobManager, BetterModelHook betterModel, ModelEngineHook modelEngine, ItemRegistry items) {
        this.plugin = plugin;
        this.registry = registry;
        this.mobManager = mobManager;
        this.betterModel = betterModel;
        this.modelEngine = modelEngine;
        this.items = items;
    }

    boolean registerMechanic(Plugin owner, String name, CustomMechanic mechanic) {
        String key = name.toLowerCase(Locale.ROOT);
        if (BUILTIN_MECHANICS.contains(key)) return false;
        return customMechanics.putIfAbsent(key, new CustomMechanicEntry(owner, mechanic)) == null;
    }

    void unregisterMechanic(String name) {
        customMechanics.remove(name.toLowerCase(Locale.ROOT));
    }

    void unregisterMechanics(Plugin owner) {
        customMechanics.values().removeIf(entry -> entry.owner().equals(owner));
    }

    void runById(String skillId, SkillContext context) {
        SkillDefinition skill = registry.get(skillId);
        if (skill == null) {
            plugin.getLogger().warning("Skill '" + skillId + "' ist nicht registriert.");
            return;
        }
        run(skill, context);
    }

    void run(SkillDefinition skill, SkillContext context) {
        for (String condition : skill.conditions) if (!conditionPasses(condition, context, null)) return;
        if (!skill.targetConditions.isEmpty()) {
            Target obstructing = resolve("obstructingblock", Map.of(), context);
            for (String condition : skill.targetConditions) if (!conditionPasses(condition, context, obstructing)) return;
        }
        executeSteps(skill.steps, 0, context);
    }

    /** Fuehrt einen einzelnen Schritt aus - fuer Mob-level "Skills:"-Zeilen, die direkt
     *  eine Mechanic statt einen Skill-Verweis sind (z.B. "sound{...} @self ~onDamaged"). */
    void runStep(SkillStep step, SkillContext context) {
        executeSteps(List.of(step), 0, context);
    }

    private void executeSteps(List<SkillStep> steps, int index, SkillContext context) {
        // Kein Existenz-Check hier am Einstieg: run()/runStep() kommen immer direkt aus
        // einem frischen Event/Trigger, der Caster lebt zu dem Zeitpunkt garantiert noch.
        // Der teure serverweite Bukkit.getEntity()-Lookup lohnt sich nur dort, wo wirklich
        // Zeit vergangen sein kann - also erst beim Wiederaufnehmen nach "delay" unten.
        for (int i = index; i < steps.size(); i++) {
            SkillStep step = steps.get(i);
            if (step instanceof SkillStep.Delay delay) {
                int next = i + 1;
                Tasks.runLater(plugin, context.caster(), delay.ticks(), () -> executeSteps(steps, next, context));
                return;
            }
            if (step instanceof SkillStep.Mechanic mechanic && runMechanic(mechanic, context)) return;
        }
    }

    /** Liefert true, wenn "cancelskill" gefeuert hat - dann bricht der Skill komplett ab. */
    private boolean runMechanic(SkillStep.Mechanic mechanic, SkillContext context) {
        Map<String, String> p = mechanic.params();

        // Inline-Bedingung am Zeilenende ("?cond{...}" / "?!cond{...}"): unbekannte
        // Bedingungen (Variablen/Factions gibt es bei uns nicht) gelten als erfuellt,
        // damit die Mechanic trotzdem laeuft statt komplett zu verschwinden.
        if (mechanic.inlineCondition() != null) {
            boolean passes = conditionPasses(mechanic.inlineCondition(), context, null);
            if (mechanic.negated() == passes) return false;
        }

        // "cd=N" (Sekunden) sperrt diese Zeile fuer den Caster, egal welche Mechanic sie ist.
        if (p.containsKey("cd") && !acquireCooldown(context, mechanic)) return false;

        // MythicMobs erlaubt "delay" sowohl als eigene Zeile zwischen Mechaniken als auch
        // als Parameter einer einzelnen Mechanic ("command{c=...;delay=70}"): diese eine
        // Mechanic feuert dann isoliert verzoegert, ohne die restlichen Schritte aufzuhalten.
        if (p.containsKey("delay")) {
            int ticks = parseInt(p.get("delay"), 0);
            // cd ebenfalls entfernen: die Kopie hat eine neue Identitaet und wuerde sonst
            // einen zweiten, wirkungslosen Cooldown-Eintrag anlegen.
            SkillStep.Mechanic withoutDelay = new SkillStep.Mechanic(mechanic.name(),
                    without(without(p, "delay"), "cd"), mechanic.targeter(), mechanic.targeterParams(), null, false);
            Tasks.runLater(plugin, context.caster(), ticks, () -> runMechanic(withoutDelay, context));
            return false;
        }

        if (mechanic.name().equals("cancelskill")) return true;

        Target target = resolve(mechanic.targeter(), mechanic.targeterParams(), context);
        // Ein Targeter wie @PIR findet unter Umstaenden niemanden - dann gibt es nichts auszufuehren.
        if (target == null) return false;
        switch (mechanic.name()) {
            case "cancelevent" -> {
                if (context.event() != null) context.event().setCancelled(true);
            }
            case "skill" -> {
                String id = firstParam(p, "s", "skill", "skills");
                if (id != null) runById(id.trim(), context);
            }
            case "look" -> look(target, context.caster());
            case "sound" -> sound(target, p);
            case "state" -> state(context.caster(), p.get("state"));
            case "potion" -> potion(target, p);
            case "breakblock" -> breakBlock(target, p);
            case "gcd" -> setGcd(context.caster().getUniqueId(), p);
            case "model" -> model(target, p);
            case "modelengine" -> modelEngineAttach(target, p);
            case "randomskill" -> randomSkill(context, p);
            case "remove" -> remove(target);
            case "command" -> command(context, p);
            case "summon" -> summon(target, p);
            case "mountmodel" -> mountModel(context, target, p);
            case "effect:particles", "e:p", "particles" -> particles(target, p);
            case "effect:particlering" -> particleRing(target, p);
            case "spin" -> spin(target, p);
            case "takeitem" -> takeItem(target, p);
            case "sudoskill" -> sudoSkill(context, target, p);
            default -> {
                CustomMechanicEntry custom = customMechanics.get(mechanic.name());
                if (custom == null) {
                    plugin.getLogger().warning("Skill-Mechanic '" + mechanic.name() + "' wird nicht unterstuetzt.");
                } else {
                    try {
                        custom.mechanic().execute(new MechanicContext(context.caster(), context.trigger(), context.event(),
                                target.entity(), target.location(), p));
                    } catch (RuntimeException exception) {
                        plugin.getLogger().warning("Eigene Mechanic '" + mechanic.name() + "' von " + custom.owner().getName()
                                + " ist fehlgeschlagen: " + exception);
                    }
                }
            }
        }
        return false;
    }

    private static Map<String, String> without(Map<String, String> params, String key) {
        Map<String, String> copy = new LinkedHashMap<>(params);
        copy.remove(key);
        return copy;
    }

    private boolean conditionPasses(String raw, SkillContext context, Target targetOverride) {
        Matcher matcher = CONDITION_PATTERN.matcher(raw.trim());
        if (!matcher.matches()) return true;
        String name = matcher.group(1).toLowerCase(Locale.ROOT);
        String paramsRaw = matcher.group(3);
        String expected = matcher.group(5);

        boolean actual = switch (name) {
            case "offgcd" -> !hasActiveGcd(context.caster().getUniqueId());
            case "onground" -> context.caster().isOnGround();
            case "blocktype" -> targetOverride != null && targetOverride.block() != null
                    && containsBlockType(paramsRaw, targetOverride.block());
            default -> {
                plugin.getLogger().warning("Skill-Condition '" + name + "' wird nicht unterstuetzt - wird ignoriert.");
                yield true;
            }
        };
        if (expected == null || expected.equalsIgnoreCase("cancel")) return actual;
        return actual == Boolean.parseBoolean(expected);
    }

    private boolean containsBlockType(String paramsRaw, Block block) {
        if (paramsRaw == null) return false;
        int eq = paramsRaw.indexOf('=');
        String list = eq < 0 ? paramsRaw : paramsRaw.substring(eq + 1);
        for (String type : list.split(",")) {
            if (type.trim().equalsIgnoreCase(block.getType().name())) return true;
        }
        return false;
    }

    private void look(Target target, LivingEntity caster) {
        Location from = caster.getLocation();
        Location facing = from.clone();
        facing.setDirection(target.location().toVector().subtract(from.toVector()));
        caster.setRotation(facing.getYaw(), facing.getPitch());
    }

    private void sound(Target target, Map<String, String> p) {
        String name = p.get("s");
        if (name == null) return;
        float pitch = parseFloat(p.get("p"), 1f);
        float volume = parseFloat(p.get("v"), 1f);
        Location location = target.location();
        location.getWorld().playSound(location, name, SoundCategory.HOSTILE, volume, pitch);
    }

    /** ponytail: nur PLAY_ONCE, li/lo/speed/n aus der Vorlage werden von BetterModels
     *  AnimationModifier hier nicht ausgewertet - fuer eine Loop-Steuerung Modifier erweitern. */
    private void state(LivingEntity caster, String stateName) {
        if (stateName == null) return;
        betterModel.play(mobManager.trackerFor(caster.getUniqueId()), stateName);
    }

    private void potion(Target target, Map<String, String> p) {
        if (!(target.entity() instanceof LivingEntity living)) return;
        PotionEffectType type = PotionEffectType.getByName(p.getOrDefault("type", "SLOW"));
        if (type == null) {
            plugin.getLogger().warning("Unbekannter Potion-Typ '" + p.get("type") + "'.");
            return;
        }
        int duration = parseInt(p.get("duration"), 20);
        int level = parseInt(p.get("level"), 1);
        living.addPotionEffect(new PotionEffect(type, duration, Math.max(0, level - 1)));
    }

    private void breakBlock(Target target, Map<String, String> p) {
        if (target.block() == null) return;
        boolean useTool = Boolean.parseBoolean(p.getOrDefault("usetool", "false"));
        if (useTool) target.block().breakNaturally();
        else target.block().setType(Material.AIR);
    }

    /** ponytail: "n" (Modell-Name/-Slot aus der Vorlage) wird nicht ausgewertet - wir
     *  haben pro Entity nur einen BetterModel-Tracker, kein benanntes Multi-Model-Setup. */
    private void model(Target target, Map<String, String> p) {
        String modelId = p.get("mid");
        if (modelId == null || target.entity() == null) return;
        Object tracker = betterModel.attach(target.entity(), modelId);
        mobManager.replaceTracker(target.entity(), tracker);
    }

    /** Pendant zu "model", haengt das Modell aber ueber ModelEngine statt BetterModel an -
     *  fuer Packs, deren Modelle in ModelEngine statt BetterModel registriert sind. */
    private void modelEngineAttach(Target target, Map<String, String> p) {
        String modelId = p.get("mid");
        if (modelId == null || target.entity() == null) return;
        Object tracker = modelEngine.attach(target.entity(), modelId);
        mobManager.replaceModelEngineTracker(target.entity(), tracker);
    }

    /** "s=a,b,c" (auch "skills=...", ueber mehrere YAML-Zeilen) waehlt zufaellig einen der
     *  Skills. "sync" wird ignoriert - wir sind ohnehin immer auf dem Main-Thread unterwegs. */
    private void randomSkill(SkillContext context, Map<String, String> p) {
        String list = firstParam(p, "s", "skills", "skill");
        if (list == null) return;
        String[] ids = list.split(",");
        if (ids.length == 0) return;
        runById(ids[ThreadLocalRandom.current().nextInt(ids.length)].trim(), context);
    }

    private boolean acquireCooldown(SkillContext context, SkillStep.Mechanic mechanic) {
        float seconds = parseFloat(mechanic.params().get("cd"), 0f);
        if (seconds <= 0) return true;
        long now = System.currentTimeMillis();
        String key = context.caster().getUniqueId() + "#" + System.identityHashCode(mechanic);
        Long until = cooldowns.get(key);
        if (until != null && until > now) return false;
        if (cooldowns.size() > 2048) cooldowns.values().removeIf(time -> time <= now);
        cooldowns.put(key, now + (long) (seconds * 1000));
        return true;
    }

    private static String firstParam(Map<String, String> params, String... keys) {
        for (String key : keys) {
            String value = params.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private void particles(Target target, Map<String, String> p) {
        Particle particle = particle(firstParam(p, "p", "particle"));
        if (particle != null) spawnParticles(target.location(), particle, p);
    }

    /** Ring aus "points" Punkten mit "radius" um das Ziel, an jedem Punkt ein Partikelschub. */
    private void particleRing(Target target, Map<String, String> p) {
        Particle particle = particle(firstParam(p, "particle", "p"));
        if (particle == null) return;
        double radius = parseFloat(p.get("radius"), 1f);
        int points = Math.max(1, parseInt(p.get("points"), 8));
        Location center = target.location();
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            spawnParticles(center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), particle, p);
        }
    }

    private void spawnParticles(Location location, Particle particle, Map<String, String> p) {
        int amount = Math.max(1, parseInt(firstParam(p, "amount", "a"), 1));
        double horizontal = parseFloat(p.get("hs"), 0f);
        double vertical = parseFloat(p.get("vs"), 0f);
        double speed = parseFloat(firstParam(p, "speed", "s"), 0f);
        try {
            location.getWorld().spawnParticle(particle, location, amount, horizontal, vertical, horizontal, speed);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Partikel '" + particle + "' braucht Zusatzdaten (z.B. Farbe) und wird nicht unterstuetzt.");
        }
    }

    private Particle particle(String name) {
        if (name == null) return null;
        try {
            return Particle.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Unbekannter Partikel '" + name + "'.");
            return null;
        }
    }

    /** Dreht das Ziel "duration" Ticks lang um "velocity" Grad pro Tick (z.B. die Karte beim Aufdecken). */
    private void spin(Target target, Map<String, String> p) {
        Entity entity = target.entity();
        if (entity == null) return;
        int duration = parseInt(p.get("duration"), 100);
        float velocity = parseFloat(p.get("velocity"), 10f);
        int[] elapsed = {0};
        Runnable[] cancel = new Runnable[1];
        cancel[0] = Tasks.runTimer(plugin, entity, 1L, 1L, () -> {
            if (++elapsed[0] > duration || !entity.isValid()) {
                cancel[0].run();
                return;
            }
            entity.setRotation(entity.getYaw() + velocity, entity.getPitch());
        });
    }

    /** Nimmt "a" Stueck des BetterMob-Items "i" aus dem Inventar des Ziel-Spielers (z.B. das Pack beim Oeffnen). */
    private void takeItem(Target target, Map<String, String> p) {
        if (!(target.entity() instanceof Player player)) return;
        String itemId = firstParam(p, "i", "item", "type");
        if (itemId == null) return;
        int remaining = Math.max(1, parseInt(firstParam(p, "a", "amount"), 1));

        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || !itemId.trim().equalsIgnoreCase(items.idOf(stack))) continue;
            int taken = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - taken);
            player.getInventory().setItem(slot, stack.getAmount() <= 0 ? null : stack);
            remaining -= taken;
        }
    }

    /** Fuehrt den Skill "s" mit dem Ziel als Caster aus (der Spieler "wirkt" ihn selbst); der urspruengliche Caster wird Trigger. */
    private void sudoSkill(SkillContext context, Target target, Map<String, String> p) {
        if (!(target.entity() instanceof LivingEntity executor)) return;
        String id = firstParam(p, "s", "skill", "skills");
        if (id != null) runById(id.trim(), new SkillContext(executor, context.caster(), null));
    }

    private void remove(Target target) {
        if (target.entity() != null) target.entity().remove();
    }

    /** "c" ist der Command-String, <caster.name>/<target.name> werden ersetzt. Laeuft
     *  immer als Konsolenbefehl - MythicMobs' "AsOp/AsCaster"-Unterscheidung gibt es hier nicht. */
    private void command(SkillContext context, Map<String, String> p) {
        String raw = p.get("c");
        if (raw == null) return;
        String command = stripQuotes(raw)
                .replace("<caster.name>", context.caster().getName())
                .replace("<target.name>", context.trigger() != null ? context.trigger().getName() : context.caster().getName());
        Tasks.runGlobal(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }

    private static String stripQuotes(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) return trimmed.substring(1, trimmed.length() - 1);
        return trimmed;
    }

    /** "type" muss die ID eines in mobs/ registrierten Mobs sein - Vanilla-EntityTypes
     *  direkt summonen unterstuetzen wir nicht, dafuer gibt es keinen Anwendungsfall im Pack. */
    private void summon(Target target, Map<String, String> p) {
        String mobId = firstParam(p, "type", "t", "mob", "m");
        if (mobId == null) return;
        MobDefinition definition = mobManager.registry().get(mobId.trim());
        if (definition == null) {
            plugin.getLogger().warning("summon: Mob '" + mobId + "' ist nicht registriert.");
            return;
        }
        mobManager.spawn(definition, target.location());
    }

    /** Setzt den Zieltrigger (z.B. den klickenden Spieler) auf einen benannten Sitz des
     *  Caster-Modells - BetterModel uebernimmt danach Lenkung/Fahrverhalten selbst. */
    private void mountModel(SkillContext context, Target target, Map<String, String> p) {
        if (!(target.entity() instanceof LivingEntity rider)) return;
        Object tracker = mobManager.trackerFor(context.caster().getUniqueId());
        if (tracker == null) {
            plugin.getLogger().warning("mountmodel: kein BetterModel-Tracker am Caster vorhanden.");
            return;
        }
        betterModel.mount(tracker, p.getOrDefault("seat", "mount"), rider);
    }

    private void setGcd(UUID casterId, Map<String, String> p) {
        int ticks = parseInt(p.get("ticks"), 20);
        gcdUntilMillis.put(casterId, System.currentTimeMillis() + ticks * 50L);
    }

    private boolean hasActiveGcd(UUID casterId) {
        Long until = gcdUntilMillis.get(casterId);
        return until != null && until > System.currentTimeMillis();
    }

    private Target resolve(String targeter, Map<String, String> targeterParams, SkillContext context) {
        return switch (targeter.toLowerCase(Locale.ROOT)) {
            case "trigger", "target" -> context.trigger() != null ? Target.ofEntity(context.trigger()) : Target.ofEntity(context.caster());
            case "obstructingblock" -> Target.ofBlock(obstructingBlock(context.caster()));
            case "forward" -> Target.ofLocation(forwardLocation(context.caster(), targeterParams));
            case "selflocation" -> Target.ofLocation(context.caster().getLocation());
            case "pir", "playersinradius" -> nearestPlayer(context.caster(), targeterParams);
            default -> Target.ofEntity(context.caster());
        };
    }

    /** ponytail: nur der naechste Spieler im Radius "r" - sort/limit aus @PIR{...} werden nicht ausgewertet, im Pack ist es immer NEAREST/1. */
    private Target nearestPlayer(LivingEntity caster, Map<String, String> p) {
        double radius = parseFloat(p.get("r"), 10f);
        Player nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity entity : caster.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player player)) continue;
            double distance = player.getLocation().distanceSquared(caster.getLocation());
            if (distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest == null ? null : Target.ofEntity(nearest);
    }

    /** "f" = Abstand nach vorne, "uel" = ab Augenhoehe statt Fussposition starten, "yoffset" = vertikale
     *  Verschiebung danach, "rotate" = Grad, um die die Blickrichtung seitlich gedreht wird (positiv = rechts). */
    private Location forwardLocation(LivingEntity caster, Map<String, String> p) {
        Location origin = Boolean.parseBoolean(p.getOrDefault("uel", "false")) ? caster.getEyeLocation() : caster.getLocation();
        double distance = parseFloat(p.get("f"), 1f);
        double yOffset = parseFloat(p.get("yoffset"), 0f);
        double rotate = parseFloat(p.get("rotate"), 0f);
        Vector direction = origin.getDirection().normalize();
        // Bukkits rotateAroundY dreht bei positivem Winkel nach links, MythicMobs' rotate nach rechts.
        if (rotate != 0) direction.rotateAroundY(Math.toRadians(-rotate));
        Location target = origin.clone().add(direction.multiply(distance));
        target.add(0, yOffset, 0);
        return target;
    }

    /** Erster nicht-durchsichtbare Block in Blickrichtung des Mobs. */
    private Block obstructingBlock(LivingEntity caster) {
        var result = caster.getWorld().rayTraceBlocks(caster.getEyeLocation(), caster.getEyeLocation().getDirection(), 2.5);
        return result != null ? result.getHitBlock() : caster.getEyeLocation().add(caster.getEyeLocation().getDirection()).getBlock();
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return value == null ? fallback : Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private record Target(Entity entity, Block block, Location rawLocation) {
        static Target ofEntity(Entity entity) {
            return new Target(entity, null, null);
        }

        static Target ofBlock(Block block) {
            return new Target(null, block, null);
        }

        static Target ofLocation(Location location) {
            return new Target(null, null, location);
        }

        Location location() {
            if (entity != null) return entity.getLocation();
            if (block != null) return block.getLocation().add(0.5, 0.5, 0.5);
            return rawLocation;
        }
    }
}
