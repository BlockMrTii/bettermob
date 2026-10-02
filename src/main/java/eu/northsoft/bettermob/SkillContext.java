package eu.northsoft.bettermob;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;

/**
 * Laufzeit-Infos fuer einen Skill-Aufruf: wer ihn ausgeloest hat und welches Event ggf.
 * per CancelEvent abbrechbar ist. Caster ist absichtlich LivingEntity statt Mob - die
 * Skill-Mechaniken brauchen nur generische Entity-Methoden, und so kann z.B. ein Spieler
 * per /bettermob skill als Caster auftreten, nicht nur gespawnte BetterMob-Mobs.
 */
record SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event) {
    static SkillContext of(LivingEntity caster) {
        return new SkillContext(caster, null, null);
    }
}
