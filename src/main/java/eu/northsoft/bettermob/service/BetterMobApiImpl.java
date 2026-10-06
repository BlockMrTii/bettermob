package eu.northsoft.bettermob.service;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.api.BetterMobAPI;
import eu.northsoft.bettermob.api.CustomMechanic;
import eu.northsoft.bettermob.api.MobInfo;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillRegistry;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.Optional;

public final class BetterMobApiImpl implements BetterMobAPI, Listener {
    private final BetterMobPlugin plugin;
    private final MobManager manager;
    private final SkillRegistry skillRegistry;
    private final SkillEngine skillEngine;

    public BetterMobApiImpl(BetterMobPlugin plugin, MobManager manager, SkillRegistry skillRegistry, SkillEngine skillEngine) {
        this.plugin = plugin;
        this.manager = manager;
        this.skillRegistry = skillRegistry;
        this.skillEngine = skillEngine;
    }

    @Override
    public Optional<MobInfo> getMob(String id) {
        MobDefinition definition = id == null ? null : manager.registry().get(id);
        return Optional.ofNullable(definition).map(MobDefinition::toInfo);
    }

    @Override
    public Collection<MobInfo> getMobs() {
        return manager.registry().all().values().stream().map(MobDefinition::toInfo).toList();
    }

    @Override
    public boolean isBetterMob(Entity entity) {
        return manager.definitionOf(entity.getUniqueId()) != null;
    }

    @Override
    public Optional<MobInfo> getMobInfo(Entity entity) {
        return Optional.ofNullable(manager.definitionOf(entity.getUniqueId())).map(MobDefinition::toInfo);
    }

    @Override
    public Optional<LivingEntity> spawn(String id, Location location) {
        MobDefinition definition = id == null ? null : manager.registry().get(id);
        if (definition == null) return Optional.empty();
        return Optional.of(manager.spawn(definition, location));
    }

    @Override
    public Collection<String> getSkillIds() {
        return java.util.List.copyOf(skillRegistry.ids());
    }

    @Override
    public boolean runSkill(String skillId, LivingEntity caster) {
        return runSkill(skillId, caster, null);
    }

    @Override
    public boolean runSkill(String skillId, LivingEntity caster, LivingEntity trigger) {
        if (skillId == null || skillRegistry.get(skillId) == null) return false;
        Tasks.runOwned(plugin, caster, () -> skillEngine.runById(skillId, new SkillContext(caster, trigger, null)));
        return true;
    }

    @Override
    public boolean registerMechanic(Plugin owner, String name, CustomMechanic mechanic) {
        return skillEngine.registerMechanic(owner, name, mechanic);
    }

    @Override
    public void unregisterMechanic(String name) {
        skillEngine.unregisterMechanic(name);
    }

    @Override
    public boolean registerCondition(Plugin owner, String name, eu.northsoft.bettermob.api.CustomCondition condition) {
        return skillEngine.conditionRegistry().registerCustom(owner, name, condition);
    }

    @Override
    public void unregisterCondition(String name) {
        skillEngine.conditionRegistry().unregisterCustom(name);
    }

    @Override
    public boolean registerTargeter(Plugin owner, String name, eu.northsoft.bettermob.api.CustomTargeter targeter) {
        return skillEngine.targeters().registerCustom(owner, name, targeter);
    }

    @Override
    public void unregisterTargeter(String name) {
        skillEngine.targeters().unregisterCustom(name);
    }

    @Override
    public boolean registerPlaceholder(Plugin owner, String namespace, eu.northsoft.bettermob.api.CustomPlaceholder placeholder) {
        return skillEngine.placeholders().register(owner, namespace, placeholder);
    }

    @Override
    public void unregisterPlaceholder(String namespace) {
        skillEngine.placeholders().unregister(namespace);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        skillEngine.unregisterMechanics(event.getPlugin());
        skillEngine.conditionRegistry().unregisterCustom(event.getPlugin());
        skillEngine.targeters().unregisterCustom(event.getPlugin());
        skillEngine.placeholders().unregister(event.getPlugin());
    }

    @Override
    public void reload() {
        plugin.reloadAll();
    }
}
