package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.platform.PlatformLivingEntity;
import eu.northsoft.bettermob.skill.core.CoreSkillContext;
import eu.northsoft.bettermob.skill.core.CoreSkillEngine;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricMobManager {
    private final BetterMobFabricMod mod;
    private final CoreSkillEngine skillEngine;
    private final Map<UUID, ActiveFabricMob> activeMobs = new ConcurrentHashMap<>();

    public FabricMobManager(BetterMobFabricMod mod, CoreSkillEngine skillEngine) {
        this.mod = mod;
        this.skillEngine = skillEngine;
    }

    public void register(UUID uuid, MobDefinition definition, PlatformLivingEntity entity) {
        ActiveFabricMob mob = new ActiveFabricMob(uuid, definition, entity);
        activeMobs.put(uuid, mob);

        CoreSkillContext ctx = new CoreSkillContext(entity);
        for (var trigger : definition.triggersOf(MobDefinition.SkillTrigger.Trigger.SPAWN)) {
            skillEngine.executeStep(trigger.step(), ctx);
        }
    }

    public ActiveFabricMob get(UUID uuid) {
        return activeMobs.get(uuid);
    }

    public void unregister(UUID uuid) {
        activeMobs.remove(uuid);
    }

    public Map<UUID, ActiveFabricMob> activeMobs() {
        return activeMobs;
    }

    public void registerEvents() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
            ActiveFabricMob mob = activeMobs.get(victim.getUuid());
            if (mob == null) return true;
            if (mob.definition().damageModifiers != null) {
                String causeKey = source.getType().msgId();
                Double modifier = mob.definition().damageModifiers.get(causeKey);
                if (modifier != null && modifier <= 0.0) {
                    return false;
                }
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, baseDamageTaken, damageTaken, blocked) -> {
            LivingEntity attacker = resolveAttacker(source);
            ActiveFabricMob victimMob = activeMobs.get(victim.getUuid());
            if (victimMob != null) {
                CoreSkillContext ctx = new CoreSkillContext(
                        victimMob.entity(),
                        attacker != null ? FabricAdapters.living(attacker) : null
                );
                for (var trigger : victimMob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.DAMAGED)) {
                    skillEngine.executeStep(trigger.step(), ctx);
                }
                victimMob.checkPhases(skillEngine, mod);
                for (var trigger : victimMob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.HEALTH)) {
                    if (trigger.health() != null && trigger.health().matches(victim.getHealth(), victim.getMaxHealth())) {
                        skillEngine.executeStep(trigger.step(), ctx);
                    }
                }
            }

            if (attacker != null) {
                ActiveFabricMob attackerMob = activeMobs.get(attacker.getUuid());
                if (attackerMob != null) {
                    CoreSkillContext ctx = new CoreSkillContext(
                            attackerMob.entity(),
                            FabricAdapters.living(victim)
                    );
                    for (var trigger : attackerMob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.ATTACK)) {
                        skillEngine.executeStep(trigger.step(), ctx);
                    }
                }
            }
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
            ActiveFabricMob mob = activeMobs.remove(victim.getUuid());
            if (mob == null) return;
            LivingEntity killer = resolveAttacker(source);
            CoreSkillContext ctx = new CoreSkillContext(
                    mob.entity(),
                    killer != null ? FabricAdapters.living(killer) : null
            );
            for (var trigger : mob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.DEATH)) {
                skillEngine.executeStep(trigger.step(), ctx);
            }
            if (mob.definition().drops != null && mob.entity() != null && mob.entity().world() != null) {
                var dropResult = eu.northsoft.bettermob.drop.DropRoller.roll(mob.definition().drops);
                for (var drop : dropResult.items()) {
                    mob.entity().world().dropItem(mob.entity().position(), drop.itemKey(), drop.amount());
                }
                if (dropResult.exp() > 0) {
                    mob.entity().world().dropExperience(mob.entity().position(), dropResult.exp());
                }
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (hand != Hand.MAIN_HAND || !(entity instanceof LivingEntity living)) return ActionResult.PASS;
            ActiveFabricMob mob = activeMobs.get(entity.getUuid());
            if (mob == null) return ActionResult.PASS;
            if (mob.definition().options != null && !mob.definition().options.interactable()) {
                return ActionResult.FAIL;
            }
            CoreSkillContext ctx = new CoreSkillContext(
                    mob.entity(),
                    FabricAdapters.living(player)
            );
            for (var trigger : mob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.INTERACT)) {
                skillEngine.executeStep(trigger.step(), ctx);
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (mod.spawnerManager() != null) {
                mod.spawnerManager().tick();
            }
            if (activeMobs.isEmpty()) return;
            var iterator = activeMobs.values().iterator();
            while (iterator.hasNext()) {
                ActiveFabricMob mob = iterator.next();
                if (!mob.entity().isValid()) {
                    iterator.remove();
                    continue;
                }
                mob.tick();
                for (var trigger : mob.definition().triggersOf(MobDefinition.SkillTrigger.Trigger.TIMER)) {
                    int interval = trigger.timerTicks() > 0 ? trigger.timerTicks() : 20;
                    if (mob.ticksAlive() % interval == 0) {
                        skillEngine.executeStep(trigger.step(), new CoreSkillContext(mob.entity()));
                    }
                }
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity.isRemoved()) {
                activeMobs.remove(entity.getUuid());
            }
        });
    }

    private static LivingEntity resolveAttacker(DamageSource source) {
        if (source == null) return null;
        Entity attacker = source.getAttacker();
        if (attacker instanceof ProjectileEntity proj && proj.getOwner() instanceof LivingEntity owner) {
            return owner;
        }
        return attacker instanceof LivingEntity living ? living : null;
    }
}
