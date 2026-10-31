package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobPhase;
import eu.northsoft.bettermob.platform.PlatformLivingEntity;
import eu.northsoft.bettermob.skill.SkillState;
import eu.northsoft.bettermob.skill.core.CoreSkillContext;
import eu.northsoft.bettermob.skill.core.CoreSkillEngine;

import java.util.UUID;

public final class ActiveFabricMob {
    private final UUID uuid;
    private final MobDefinition definition;
    private final PlatformLivingEntity entity;
    private final SkillState skillState = new SkillState();
    private int ticksAlive;
    private int currentPhaseIndex = -1;

    public ActiveFabricMob(UUID uuid, MobDefinition definition, PlatformLivingEntity entity) {
        this.uuid = uuid;
        this.definition = definition;
        this.entity = entity;
    }

    public UUID uuid() {
        return uuid;
    }

    public MobDefinition definition() {
        return definition;
    }

    public PlatformLivingEntity entity() {
        return entity;
    }

    public SkillState skillState() {
        return skillState;
    }

    public int ticksAlive() {
        return ticksAlive;
    }

    public void tick() {
        ticksAlive++;
    }

    public int currentPhaseIndex() {
        return currentPhaseIndex;
    }

    public void checkPhases(CoreSkillEngine engine, BetterMobFabricMod mod) {
        if (!definition.hasPhases() || entity == null || !entity.isValid()) return;
        int nextPhase = MobPhase.select(definition.phases, entity.health(), entity.maxHealth());
        if (nextPhase != -1 && nextPhase != currentPhaseIndex) {
            currentPhaseIndex = nextPhase;
            MobPhase phase = definition.phases.get(nextPhase);
            if (phase.display() != null && !phase.display().isBlank()) {
                entity.customName(phase.display().replace('&', '§'));
                entity.customNameVisible(true);
            }
            if (phase.model() != null && mod != null && mod.platform() != null && mod.platform().model("bettermodel").isPresent()) {
                mod.platform().model("bettermodel").get().attach(entity, phase.model());
            }
            if (phase.start() != null) {
                CoreSkillContext ctx = new CoreSkillContext(entity);
                for (var step : phase.start()) {
                    engine.executeStep(step, ctx);
                }
            }
            for (var trigger : phase.triggersOf(MobDefinition.SkillTrigger.Trigger.PHASE)) {
                engine.executeStep(trigger.step(), new CoreSkillContext(entity));
            }
        }
    }
}
