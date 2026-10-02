package eu.northsoft.bettermob;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MobDefinition {
    final String id;
    final EntityType type;
    final String displayName;
    final String modelId;
    final double health;
    final double damage;
    final boolean removeAi;
    final List<String> aiGoalSelectors;
    final List<String> aiTargetSelectors;
    final Options options;
    final boolean threatTable;
    final Map<DamageCause, Double> damageModifiers;
    final List<SkillTrigger> skillTriggers;

    MobDefinition(String id, EntityType type, String displayName, String modelId,
                  double health, double damage, boolean removeAi,
                  List<String> aiGoalSelectors, List<String> aiTargetSelectors,
                  Options options, boolean threatTable, Map<DamageCause, Double> damageModifiers,
                  List<SkillTrigger> skillTriggers) {
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
    }

    MobInfo toInfo() {
        return new MobInfo(id, type, displayName, modelId, health, damage);
    }

    /** -1 bei movementSpeed/knockbackResistance heisst: Vanilla-Wert unangetastet lassen; itemHead ist eine Item-ID oder null. */
    record Options(boolean collidable, double movementSpeed, boolean preventOtherDrops, boolean silent,
                   boolean preventRenaming, boolean preventLeashing, boolean alwaysShowName, boolean preventSunburn,
                   boolean invincible, boolean invisible, boolean canMove, boolean interactable, boolean marker,
                   String itemHead, double knockbackResistance) {
        static final Options DEFAULT = new Options(true, -1, false, false, false, false, false, true, false,
                false, true, true, false, null, -1);
    }

    /**
     * Eine Zeile aus Skills: "<mechanic>{params} @targeter ~onTrigger[:ticks]" - wie bei
     * MythicMobs. Die Mechanic kann direkt sound/model/randomskill/... sein, oder ueber
     * "skill{s=<id>}" eine Skill-Datei aus skills/ aufrufen.
     */
    record SkillTrigger(SkillStep step, Trigger trigger, int timerTicks) {
        private static final Pattern PATTERN = Pattern.compile("^(.*\\S)\\s+~on(\\w+?)(?::(\\d+))?\\s*$", Pattern.CASE_INSENSITIVE);

        static SkillTrigger parse(String line) {
            Matcher matcher = PATTERN.matcher(line.trim());
            if (!matcher.matches()) return null;
            Trigger trigger = Trigger.parse(matcher.group(2));
            if (trigger == null) return null;
            SkillStep step = SkillStep.parse(matcher.group(1));
            if (step == null) return null;
            int ticks = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 20;
            return new SkillTrigger(step, trigger, ticks);
        }

        enum Trigger {
            SPAWN, LOAD, INTERACT, DAMAGED, ATTACK, DEATH, TIMER, USE;

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
                    default -> null;
                };
            }
        }
    }
}
