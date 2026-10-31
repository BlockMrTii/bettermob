package eu.northsoft.bettermob.skill.core;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformLivingEntity;
import eu.northsoft.bettermob.platform.PlatformWorld;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.platform.Vec3;
import eu.northsoft.bettermob.skill.SkillStep;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreSkillEngineTest {

    private static class DummyLivingEntity implements PlatformLivingEntity {
        private final UUID uuid = UUID.randomUUID();
        private double health = 20.0;
        private double maxHealth = 20.0;
        private Position position = new Position("minecraft:overworld", 0, 64, 0, 0, 0);

        @Override public UUID uuid() { return uuid; }
        @Override public String typeKey() { return "minecraft:zombie"; }
        @Override public Position position() { return position; }
        @Override public PlatformWorld world() { return null; }
        @Override public boolean isValid() { return true; }
        @Override public void teleport(Position position) { this.position = position; }
        @Override public Vec3 velocity() { return new Vec3(0, 0, 0); }
        @Override public void velocity(Vec3 velocity) {}
        @Override public void remove() {}
        @Override public Object handle() { return this; }
        @Override public double health() { return health; }
        @Override public void health(double health) { this.health = health; }
        @Override public double maxHealth() { return maxHealth; }
        @Override public void maxHealth(double maxHealth) { this.maxHealth = maxHealth; }
        @Override public void damage(double amount, PlatformEntity source) { this.health -= amount; }
    }

    @Test
    void executesMechanicWhenConditionsHold() {
        CoreSkillEngine engine = new CoreSkillEngine();
        List<String> executed = new ArrayList<>();
        engine.registerMechanic("test_action", (ctx, target, params) -> {
            executed.add(params.get("msg"));
        });

        DummyLivingEntity caster = new DummyLivingEntity();
        CoreSkillContext context = new CoreSkillContext(caster);

        SkillStep step = SkillStep.parse("test_action{msg=Hello} @self ?chance{c=1.0}");
        engine.executeStep(step, context);

        assertEquals(1, executed.size());
        assertEquals("Hello", executed.get(0));
    }

    @Test
    void skipsMechanicWhenConditionFails() {
        CoreSkillEngine engine = new CoreSkillEngine();
        AtomicBoolean ran = new AtomicBoolean(false);
        engine.registerMechanic("never", (ctx, target, params) -> ran.set(true));

        DummyLivingEntity caster = new DummyLivingEntity();
        CoreSkillContext context = new CoreSkillContext(caster);

        SkillStep step = SkillStep.parse("never{} @self ?chance{c=0.0}");
        engine.executeStep(step, context);

        assertTrue(!ran.get());
    }

    @Test
    void resolvesVariablesInMechanicParameters() {
        CoreSkillEngine engine = new CoreSkillEngine();
        List<String> results = new ArrayList<>();
        engine.registerMechanic("echo", (ctx, target, params) -> results.add(params.get("val")));

        DummyLivingEntity caster = new DummyLivingEntity();
        engine.state().variablesOf(caster.uuid()).put("count", "42");
        CoreSkillContext context = new CoreSkillContext(caster);

        SkillStep step = SkillStep.parse("echo{val=<caster.var.count>} @self");
        engine.executeStep(step, context);

        assertEquals(1, results.size());
        assertEquals("42", results.get(0));
    }
}
