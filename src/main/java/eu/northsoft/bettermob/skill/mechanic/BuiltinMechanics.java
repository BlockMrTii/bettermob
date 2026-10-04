package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;

public final class BuiltinMechanics {
    private BuiltinMechanics() {}

    public static void registerAll(MechanicRegistry registry, SkillEngine engine) {
        registry.register(new CancelEventMechanic(), "cancelevent");
        registry.register(new SkillMechanic(engine), "skill");
        registry.register(new LookMechanic(), "look");
        registry.register(new SoundMechanic(), "sound");
        registry.register(new MessageMechanic(), "message", "msg");
        registry.register(new StateMechanic(engine), "state");
        registry.register(new PotionMechanic(engine), "potion");
        registry.register(new BreakBlockMechanic(), "breakblock");
        registry.register(new GcdMechanic(engine), "gcd");
        registry.register(new ModelMechanic(engine), "model");
        registry.register(new ModelEngineMechanic(engine), "modelengine");
        registry.register(new RandomSkillMechanic(engine), "randomskill");
        registry.register(new RemoveMechanic(), "remove");
        registry.register(new CommandMechanic(engine), "command");
        registry.register(new SummonMechanic(engine), "summon");
        registry.register(new MountModelMechanic(engine), "mountmodel");
        registry.register(new ParticlesMechanic(engine), "effect:particles", "e:p", "particles");
        registry.register(new ParticleRingMechanic(engine), "effect:particlering");
        registry.register(new SpinMechanic(engine), "spin");
        registry.register(new TakeItemMechanic(engine), "takeitem");
        registry.register(new SudoSkillMechanic(engine), "sudoskill");
        registry.register(new DamageMechanic(engine), "damage");
        registry.register(new ThrowMechanic(), "throw");
        registry.register(new LungeMechanic(), "lunge");
        registry.register(new SetBlockMechanic(engine), "setblock");
        registry.register(new EquipMechanic(engine), "equip");
        registry.register(new AuraMechanic(engine), "aura", "ondamaged", "onattack", "ontick", "ondeath", "onshoot");
        registry.register(new TagMechanic(true), "addtag");
        registry.register(new TagMechanic(false), "removetag");
        registry.register(new BodyRotationMechanic(engine), "bodyrotation");
        registry.register(new IgniteMechanic(), "ignite");
        registry.register(new ShootMechanic(engine), "shoot");
        registry.register(new StunMechanic(engine), "stun");
        registry.register(new VelocityMechanic(engine), "velocity");
        registry.register(new FreezeMechanic(), "freeze");
        registry.register(new SetNoDamageTicksMechanic(), "setnodamageticks");
        registry.register(new TotemMechanic(engine), "totem");
        registry.register(new HealMechanic(), "heal");
        registry.register(new TeleportMechanic(), "teleport");
        registry.register(new ExplosionMechanic(), "explosion");
        registry.register(new LightningMechanic(), "lightning");
        registry.register(new SetSpeedMechanic(), "setspeed");
        registry.register(new SetAiMechanic(), "setai");
    }
}
