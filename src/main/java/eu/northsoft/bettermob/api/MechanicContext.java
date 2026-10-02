package eu.northsoft.bettermob.api;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;

import java.util.Map;

/**
 * Alles, was eine eigene Mechanic beim Aufruf wissen muss.
 *
 * @param caster  wer den Skill ausfuehrt
 * @param trigger Ausloeser (z.B. der Angreifer bei ~onDamaged), kann null sein
 * @param event   abbrechbares Ausloese-Event, kann null sein
 * @param target  aufgeloestes Ziel des Targeters der Zeile (Entity oder null)
 * @param location Position des Ziels (bei Entity dessen Position)
 * @param params  Parameter aus den geschweiften Klammern, Keys in Kleinbuchstaben
 */
public record MechanicContext(LivingEntity caster, LivingEntity trigger, Cancellable event,
                              Entity target, Location location, Map<String, String> params) {
}
