package eu.northsoft.bettermob.api;

import org.bukkit.entity.Entity;

import java.util.List;

@FunctionalInterface
public interface CustomTargeter {
    List<Entity> resolve(TargeterContext context);
}
