package eu.northsoft.bettermob.skill;

import org.bukkit.entity.LivingEntity;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkillState {
    private final Map<UUID, Long> gcdUntilMillis = new ConcurrentHashMap<>();
    private record SkillKey(UUID caster, String skill) {}

    private record StepKey(UUID caster, int step) {}

    private final Map<Object, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> casterVariables = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Aura>> auras = new ConcurrentHashMap<>();
    private static final int MIN_PURGE_SIZE = 2048;

    private volatile int purgeSize = MIN_PURGE_SIZE;
    private final ThreadLocal<Boolean> applyingDamage = ThreadLocal.withInitial(() -> false);

    public void setGcd(UUID casterId, int ticks) {
        gcdUntilMillis.put(casterId, System.currentTimeMillis() + ticks * 50L);
    }

    public boolean hasActiveGcd(UUID casterId) {
        Long until = gcdUntilMillis.get(casterId);
        return until != null && until > System.currentTimeMillis();
    }

    boolean acquireSkillCooldown(LivingEntity caster, SkillDefinition skill) {
        return acquire(skillCooldownKey(caster, skill.id), (long) (skill.cooldown * 1000));
    }

    boolean acquireStepCooldown(LivingEntity caster, Object step, float seconds) {
        return acquire(new StepKey(caster.getUniqueId(), System.identityHashCode(step)), (long) (seconds * 1000));
    }

    public boolean skillOnCooldown(LivingEntity caster, String skillId) {
        Long until = cooldowns.get(skillCooldownKey(caster, skillId));
        return until != null && until > System.currentTimeMillis();
    }

    boolean acquire(Object key, long millis) {
        long now = System.currentTimeMillis();
        if (cooldowns.size() > purgeSize) {
            cooldowns.values().removeIf(time -> time <= now);
            purgeSize = Math.max(MIN_PURGE_SIZE, cooldowns.size() * 2);
        }
        boolean[] acquired = {false};
        cooldowns.compute(key, (ignored, until) -> {
            if (until != null && until > now) return until;
            acquired[0] = true;
            return now + millis;
        });
        return acquired[0];
    }

    private static SkillKey skillCooldownKey(LivingEntity caster, String skillId) {
        return new SkillKey(caster.getUniqueId(), skillId.toLowerCase(Locale.ROOT));
    }

    int cooldownCount() {
        return cooldowns.size();
    }

    public Map<String, Aura> aurasOf(UUID entityId) {
        return auras.computeIfAbsent(entityId, id -> new ConcurrentHashMap<>());
    }

    public Map<String, Aura> activeAuras(UUID entityId) {
        return auras.get(entityId);
    }

    public boolean hasAura(LivingEntity entity, String name) {
        Map<String, Aura> active = auras.get(entity.getUniqueId());
        if (active == null) return false;
        Aura aura = active.get(name.toLowerCase(Locale.ROOT));
        if (aura == null) return false;
        if (aura.until <= System.currentTimeMillis()) {
            active.remove(name.toLowerCase(Locale.ROOT), aura);
            return false;
        }
        return true;
    }

    public void applyDamage(LivingEntity victim, double amount, LivingEntity source) {
        boolean previous = applyingDamage.get();
        applyingDamage.set(true);
        try {
            victim.damage(amount, source);
        } finally {
            applyingDamage.set(previous);
        }
    }

    public boolean isApplyingDamage() {
        return applyingDamage.get();
    }

    public Map<String, String> variablesOf(UUID entityId) {
        return casterVariables.computeIfAbsent(entityId, id -> new ConcurrentHashMap<>());
    }

    public Map<String, String> existingVariablesOf(UUID entityId) {
        return casterVariables.get(entityId);
    }

    public void forget(UUID entityId) {
        casterVariables.remove(entityId);
        Map<String, Aura> removed = auras.remove(entityId);
        if (removed != null) removed.values().forEach(Aura::stop);
        gcdUntilMillis.remove(entityId);
    }
}
