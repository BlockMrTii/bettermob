package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobDefinitionParser;
import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformLivingEntity;
import eu.northsoft.bettermob.platform.PlatformWorld;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.platform.Vec3;
import eu.northsoft.bettermob.platform.YamlConfig;
import eu.northsoft.bettermob.skill.core.CoreSkillEngine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActiveFabricMobTest {
    private static class MockLivingEntity implements PlatformLivingEntity {
        private final UUID uuid = UUID.randomUUID();
        private double health = 100.0;
        private double maxHealth = 100.0;
        private String customName = "Original";
        private boolean customNameVisible = false;

        @Override public UUID uuid() { return uuid; }
        @Override public String typeKey() { return "minecraft:zombie"; }
        @Override public Position position() { return new Position("minecraft:overworld", 0, 64, 0, 0, 0); }
        @Override public PlatformWorld world() { return null; }
        @Override public boolean isValid() { return true; }
        @Override public void teleport(Position position) {}
        @Override public Vec3 velocity() { return new Vec3(0, 0, 0); }
        @Override public void velocity(Vec3 velocity) {}
        @Override public void remove() {}
        @Override public Object handle() { return this; }
        @Override public String customName() { return customName; }
        @Override public void customName(String name) { this.customName = name; }
        @Override public boolean isCustomNameVisible() { return customNameVisible; }
        @Override public void customNameVisible(boolean visible) { this.customNameVisible = visible; }
        @Override public double health() { return health; }
        @Override public void health(double health) { this.health = health; }
        @Override public double maxHealth() { return maxHealth; }
        @Override public void maxHealth(double maxHealth) { this.maxHealth = maxHealth; }
        @Override public void damage(double amount, PlatformEntity source) { this.health -= amount; }
    }

    @Test
    void transitionsPhaseOnHealthThreshold() {
        String yaml = """
                Type: ZOMBIE
                Display: 'Phase 0 Boss'
                Health: 100
                Phases:
                  - Health: '<50%'
                    Display: 'Phase 1 Enraged'
                    OnStart:
                      - test_step{} @self
                """;

        MobDefinition def = MobDefinitionParser.parse("boss", YamlConfig.load(yaml), err -> {});
        assertNotNull(def);
        assertEquals(1, def.phases.size());

        MockLivingEntity entity = new MockLivingEntity();
        ActiveFabricMob mob = new ActiveFabricMob(entity.uuid(), def, entity);
        CoreSkillEngine engine = new CoreSkillEngine();
        List<String> executed = new ArrayList<>();
        engine.registerMechanic("test_step", (ctx, target, params) -> executed.add("started"));

        BetterMobFabricMod mod = new BetterMobFabricMod();
        mob.checkPhases(engine, mod);
        assertEquals(-1, mob.currentPhaseIndex());
        assertEquals(0, executed.size());

        entity.health(40.0);
        mob.checkPhases(engine, mod);

        assertEquals(0, mob.currentPhaseIndex());
        assertEquals("Phase 1 Enraged", entity.customName());
        assertEquals(1, executed.size());
    }

    @Test
    void managesActiveMobsLifecycle() {
        BetterMobFabricMod mod = new BetterMobFabricMod();
        CoreSkillEngine engine = new CoreSkillEngine();
        FabricMobManager manager = new FabricMobManager(mod, engine);

        String yaml = "Type: ZOMBIE\nHealth: 50\n";
        MobDefinition def = MobDefinitionParser.parse("simple_mob", YamlConfig.load(yaml), err -> {});

        MockLivingEntity entity = new MockLivingEntity();
        manager.register(entity.uuid(), def, entity);

        assertNotNull(manager.get(entity.uuid()));
        assertEquals("simple_mob", manager.get(entity.uuid()).definition().id);

        manager.unregister(entity.uuid());
        assertNull(manager.get(entity.uuid()));
    }
}
