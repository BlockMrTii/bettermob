package eu.northsoft.bettermob.api;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;

import java.util.Map;

public record MechanicContext(LivingEntity caster, LivingEntity trigger, Cancellable event,
                              Entity target, Location location, Map<String, String> params) {
}
