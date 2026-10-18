package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.api.MobInfo;
import eu.northsoft.bettermob.drop.DropTable;
import eu.northsoft.bettermob.skill.Params;
import eu.northsoft.bettermob.skill.SkillStep;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MobDefinition {
    public final String id;
    public final EntityType type;
    public final String displayName;
    public final String modelId;
    public final double health;
    public final double damage;
    public final boolean removeAi;
    public final List<String> aiGoalSelectors;
    public final List<String> aiTargetSelectors;
    public final Options options;
    public final boolean threatTable;
    public final Map<DamageCause, Double> damageModifiers;
    public final List<SkillTrigger> skillTriggers;
    private final Map<SkillTrigger.Trigger, List<SkillTrigger>> triggersByType;

    public final DropTable drops;
    public final String faction;
    public final BossBarSettings bossBar;
    public final EggSettings egg;
    public final Behaviour behaviour;
    public final List<MobPhase> phases;
    public final List<String> equipment;
    public final SpawnRule spawnRule;

    public MobDefinition(String id, EntityType type, String displayName, String modelId,
                  double health, double damage, boolean removeAi,
                  List<String> aiGoalSelectors, List<String> aiTargetSelectors,
                  Options options, boolean threatTable, Map<DamageCause, Double> damageModifiers,
                  List<SkillTrigger> skillTriggers, DropTable drops, String faction,
                  BossBarSettings bossBar, List<String> equipment, SpawnRule spawnRule, EggSettings egg, Behaviour behaviour) {
        this(id, type, displayName, modelId, health, damage, removeAi, aiGoalSelectors, aiTargetSelectors, options, threatTable,
                damageModifiers, skillTriggers, drops, faction, bossBar, equipment, spawnRule, egg, behaviour, List.of());
    }

    public MobDefinition(String id, EntityType type, String displayName, String modelId,
                  double health, double damage, boolean removeAi,
                  List<String> aiGoalSelectors, List<String> aiTargetSelectors,
                  Options options, boolean threatTable, Map<DamageCause, Double> damageModifiers,
                  List<SkillTrigger> skillTriggers, DropTable drops, String faction,
                  BossBarSettings bossBar, List<String> equipment, SpawnRule spawnRule, EggSettings egg, Behaviour behaviour,
                  List<MobPhase> phases) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.modelId = modelId;
        this.health = health;
        this.damage = damage;
        this.removeAi = removeAi;
        this.aiGoalSelectors = aiGoalSelectors;
        this.aiTargetSelectors = aiTargetSelectors;
        this.options = options;
        this.threatTable = threatTable;
        this.damageModifiers = damageModifiers;
        this.skillTriggers = skillTriggers;
        Map<SkillTrigger.Trigger, List<SkillTrigger>> byType = new EnumMap<>(SkillTrigger.Trigger.class);
        for (SkillTrigger trigger : skillTriggers) byType.computeIfAbsent(trigger.trigger(), key -> new ArrayList<>()).add(trigger);
        byType.replaceAll((key, list) -> List.copyOf(list));
        this.triggersByType = byType;
        this.drops = drops;
        this.faction = faction;
        this.bossBar = bossBar;
        this.egg = egg;
        this.behaviour = behaviour;
        this.phases = phases;
        this.equipment = equipment;
        this.spawnRule = spawnRule;
    }

    public boolean hasPhases() {
        return !phases.isEmpty();
    }

    public List<SkillTrigger> triggersOf(SkillTrigger.Trigger type) {
        return triggersByType.getOrDefault(type, List.of());
    }

    public record EggSettings(String material, String name) {}

    public MobInfo toInfo() {
        return new MobInfo(id, type, displayName, modelId, health, damage);
    }

    public record Options(boolean collidable, double movementSpeed, boolean preventOtherDrops, boolean silent,
                   boolean preventRenaming, boolean preventLeashing, boolean alwaysShowName, boolean preventSunburn,
                   boolean invincible, boolean invisible, boolean canMove, boolean interactable, boolean marker,
                   String itemHead, double knockbackResistance, double followRange, boolean preventItemPickup,
                   double scale) {
        static final Options DEFAULT = new Options(true, -1, false, false, false, false, false, true, false,
                false, true, true, false, null, -1, -1, false, -1);
    }

    public record SkillTrigger(SkillStep step, Trigger trigger, int timerTicks, HealthSpec health, String signal) {
        public SkillTrigger(SkillStep step, Trigger trigger, int timerTicks, HealthSpec health) {
            this(step, trigger, timerTicks, health, null);
        }

        public SkillTrigger(SkillStep step, Trigger trigger, int timerTicks) {
            this(step, trigger, timerTicks, null, null);
        }

        private static final Pattern PATTERN = Pattern.compile("^(.*\\S)\\s+~on(\\w+?)(?::([A-Za-z0-9_-]+))?(?:(<=|>=|<|>)(\\d+(?:\\.\\d+)?)(%)?)?(?:\\s+(\\?!?~?\\w+(?:\\{.*})?))?\\s*$", Pattern.CASE_INSENSITIVE);

        public static SkillTrigger parse(String line) {
            Matcher matcher = PATTERN.matcher(line.trim());
            if (!matcher.matches()) return null;
            Trigger trigger = Trigger.parse(matcher.group(2));
            if (trigger == null) return null;
            HealthSpec health = HealthSpec.parse(matcher.group(4), matcher.group(5), matcher.group(6));
            if (trigger == Trigger.HEALTH && health == null) return null;
            if (trigger != Trigger.HEALTH && matcher.group(4) != null) return null;
            SkillStep step = SkillStep.parse(matcher.group(7) == null ? matcher.group(1) : matcher.group(1) + " " + matcher.group(7));
            if (step == null) return null;
            String suffix = matcher.group(3);
            String signal = null;
            if (trigger == Trigger.SIGNAL) {
                if (suffix == null) return null;
                signal = suffix.toLowerCase(Locale.ROOT);
            } else if (suffix != null && !suffix.chars().allMatch(Character::isDigit)) {
                return null;
            }
            int ticks = Params.parseInt(trigger == Trigger.SIGNAL ? null : suffix, trigger == Trigger.MOVE ? 5 : 20);
            return new SkillTrigger(step, trigger, ticks, health, signal);
        }

        public enum Trigger {
            SPAWN, LOAD, INTERACT, DAMAGED, ATTACK, DEATH, TIMER, USE, SHOOT, HEALTH, TARGET, LOSETARGET, ENTERCOMBAT, EXITCOMBAT, KILL, SIGNAL, DESPAWN, MOVE, SWING, PRESSF, PICKUP, DROP, EQUIP;

            static Trigger parse(String value) {
                return switch (value.toLowerCase(Locale.ROOT)) {
                    case "spawn" -> SPAWN;
                    case "load" -> LOAD;
                    case "interact", "rightclick" -> INTERACT;
                    case "damaged", "hurt" -> DAMAGED;
                    case "attack" -> ATTACK;
                    case "death" -> DEATH;
                    case "timer" -> TIMER;
                    case "use" -> USE;
                    case "shoot" -> SHOOT;
                    case "health" -> HEALTH;
                    case "target" -> TARGET;
                    case "losetarget" -> LOSETARGET;
                    case "entercombat" -> ENTERCOMBAT;
                    case "exitcombat" -> EXITCOMBAT;
                    case "kill" -> KILL;
                    case "signal" -> SIGNAL;
                    case "despawn" -> DESPAWN;
                    case "move" -> MOVE;
                    case "swing", "leftclick" -> SWING;
                    case "pressf", "swaphands" -> PRESSF;
                    case "pickup", "itempickup" -> PICKUP;
                    case "drop", "itemdrop" -> DROP;
                    case "equip" -> EQUIP;
                    default -> null;
                };
            }
        }
    }
}
