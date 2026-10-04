package eu.northsoft.bettermob.api;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collection;
import java.util.Optional;

public interface BetterMobAPI {
    static BetterMobAPI get() {
        RegisteredServiceProvider<BetterMobAPI> provider = org.bukkit.Bukkit.getServicesManager().getRegistration(BetterMobAPI.class);
        if (provider == null) throw new IllegalStateException("BetterMob ist nicht aktiv.");
        return provider.getProvider();
    }

    Optional<MobInfo> getMob(String id);

    Collection<MobInfo> getMobs();

    boolean isBetterMob(Entity entity);

    Optional<MobInfo> getMobInfo(Entity entity);

    Optional<LivingEntity> spawn(String id, Location location);

    Collection<String> getSkillIds();

    boolean runSkill(String skillId, LivingEntity caster);

    boolean runSkill(String skillId, LivingEntity caster, LivingEntity trigger);

    boolean registerMechanic(Plugin owner, String name, CustomMechanic mechanic);

    void unregisterMechanic(String name);

    boolean registerCondition(Plugin owner, String name, CustomCondition condition);

    void unregisterCondition(String name);

    boolean registerTargeter(Plugin owner, String name, CustomTargeter targeter);

    void unregisterTargeter(String name);

    boolean registerPlaceholder(Plugin owner, String namespace, CustomPlaceholder placeholder);

    void unregisterPlaceholder(String namespace);

    void reload();
}
