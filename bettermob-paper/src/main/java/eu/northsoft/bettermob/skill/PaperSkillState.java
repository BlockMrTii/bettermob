package eu.northsoft.bettermob.skill;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class PaperSkillState extends SkillState {
    public boolean acquireSkillCooldown(LivingEntity caster, SkillDefinition skill) {
        return caster != null && acquireSkillCooldown(caster.getUniqueId(), skill);
    }

    public boolean acquireStepCooldown(LivingEntity caster, Object step, float seconds) {
        return caster != null && acquireStepCooldown(caster.getUniqueId(), step, seconds);
    }

    public boolean skillOnCooldown(LivingEntity caster, String skillId) {
        return caster != null && skillOnCooldown(caster.getUniqueId(), skillId);
    }

    public boolean hasAura(LivingEntity entity, String name) {
        return entity != null && hasAura(entity.getUniqueId(), name);
    }

    public void applyDamage(LivingEntity victim, double amount, LivingEntity source) {
        if (victim == null) return;
        runWithApplyingDamage(() -> victim.damage(amount, source != null && Bukkit.isOwnedByCurrentRegion(source) ? source : null));
    }
}
