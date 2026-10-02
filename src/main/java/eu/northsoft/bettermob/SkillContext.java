package eu.northsoft.bettermob;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;

/**
 * Laufzeit-Infos fuer einen Skill-Aufruf: wer ihn ausgeloest hat und welches Event ggf.
 * per CancelEvent abbrechbar ist. Caster ist absichtlich LivingEntity statt Mob - die
 * Skill-Mechaniken brauchen nur generische Entity-Methoden, und so kann z.B. ein Spieler
 * per /bettermob skill als Caster auftreten, nicht nur gespawnte BetterMob-Mobs.
 *
 * @param origin          Ursprungsort eines Totems (@EntitiesNearOrigin), sonst null
 * @param targetIsTrigger Zeilen ohne eigenen Targeter treffen den Ausloeser statt den Caster - so
 *                        erben verschachtelte Skills das Ziel der Zeile, die sie aufgerufen hat
 */
record SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event, Location origin, boolean targetIsTrigger) {
    SkillContext(LivingEntity caster, LivingEntity trigger, Cancellable event) {
        this(caster, trigger, event, null, false);
    }

    static SkillContext of(LivingEntity caster) {
        return new SkillContext(caster, null, null);
    }

    SkillContext withTrigger(LivingEntity newTrigger) {
        return new SkillContext(caster, newTrigger, event, origin, true);
    }

    SkillContext withOrigin(Location newOrigin) {
        return new SkillContext(caster, trigger, event, newOrigin, targetIsTrigger);
    }
}
