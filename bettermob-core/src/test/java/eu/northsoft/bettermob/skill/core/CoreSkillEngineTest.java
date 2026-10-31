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
        private Vec3 velocity = new Vec3(0, 0, 0);
        private String customName = "Dummy";
        private int fireTicks = 0;
        private String potionType;
        private int potionDuration;
        private int potionLevel;
        private final List<String> receivedMessages = new ArrayList<>();
        private final List<String> receivedActionBars = new ArrayList<>();
        private PlatformWorld world;

        @Override public UUID uuid() { return uuid; }
        @Override public String typeKey() { return "minecraft:zombie"; }
        @Override public Position position() { return position; }
        @Override public PlatformWorld world() { return world; }
        public void setWorld(PlatformWorld world) { this.world = world; }
        @Override public boolean isValid() { return true; }
        @Override public void teleport(Position position) { this.position = position; }
        @Override public Vec3 velocity() { return velocity; }
        @Override public void velocity(Vec3 velocity) { this.velocity = velocity; }
        @Override public void remove() {}
        @Override public Object handle() { return this; }
        @Override public String customName() { return customName; }
        @Override public void customName(String name) { this.customName = name; }
        @Override public double health() { return health; }
        @Override public void health(double health) { this.health = health; }
        @Override public double maxHealth() { return maxHealth; }
        @Override public void maxHealth(double maxHealth) { this.maxHealth = maxHealth; }
        @Override public void damage(double amount, PlatformEntity source) { this.health -= amount; }
        @Override public boolean isPlayer() { return true; }
        @Override public void sendMessage(String message) { receivedMessages.add(message); }
        @Override public void sendActionBar(String message) { receivedActionBars.add(message); }
        @Override public void setFireTicks(int ticks) { this.fireTicks = ticks; }
        @Override public void addPotionEffect(String type, int durationTicks, int amplifier) {
            this.potionType = type;
            this.potionDuration = durationTicks;
            this.potionLevel = amplifier;
        }
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

    @Test
    void executesMessageAndActionBarMechanics() {
        CoreSkillEngine engine = new CoreSkillEngine();
        DummyLivingEntity caster = new DummyLivingEntity();
        caster.customName("BossMob");
        DummyLivingEntity target = new DummyLivingEntity();
        target.customName("HeroPlayer");

        CoreSkillContext context = new CoreSkillContext(caster, target);

        SkillStep msgStep = SkillStep.parse("message{m=\"<caster.name> strikes <target.name>!\"} @target");
        engine.executeStep(msgStep, context);

        assertEquals(1, target.receivedMessages.size());
        assertEquals("BossMob strikes HeroPlayer!", target.receivedMessages.get(0));

        SkillStep barStep = SkillStep.parse("actionbar{m=\"Danger near <caster.name>\"} @target");
        engine.executeStep(barStep, context);

        assertEquals(1, target.receivedActionBars.size());
        assertEquals("Danger near BossMob", target.receivedActionBars.get(0));
    }

    @Test
    void executesVelocityAndPotionAndIgniteMechanics() {
        CoreSkillEngine engine = new CoreSkillEngine();
        DummyLivingEntity caster = new DummyLivingEntity();
        DummyLivingEntity target = new DummyLivingEntity();
        CoreSkillContext context = new CoreSkillContext(caster, target);

        SkillStep velStep = SkillStep.parse("velocity{x=0.5;y=1.0;z=0.0;m=SET} @target");
        engine.executeStep(velStep, context);
        assertEquals(0.5, target.velocity().x(), 1e-4);
        assertEquals(1.0, target.velocity().y(), 1e-4);

        SkillStep potStep = SkillStep.parse("potion{t=speed;d=100;l=2} @target");
        engine.executeStep(potStep, context);
        assertEquals("speed", target.potionType);
        assertEquals(100, target.potionDuration);
        assertEquals(1, target.potionLevel);

        SkillStep igniteStep = SkillStep.parse("ignite{t=80} @target");
        engine.executeStep(igniteStep, context);
        assertEquals(80, target.fireTicks);
    }

    @Test
    void resolvesEntitiesInRadiusTargeter() {
        CoreSkillEngine engine = new CoreSkillEngine();
        DummyLivingEntity caster = new DummyLivingEntity();
        caster.teleport(new Position("minecraft:overworld", 0, 64, 0, 0, 0));

        DummyLivingEntity near = new DummyLivingEntity();
        near.teleport(new Position("minecraft:overworld", 2, 64, 0, 0, 0));

        DummyLivingEntity far = new DummyLivingEntity();
        far.teleport(new Position("minecraft:overworld", 50, 64, 0, 0, 0));

        PlatformWorld dummyWorld = new PlatformWorld() {
            @Override public String key() { return "minecraft:overworld"; }
            @Override public long time() { return 1000; }
            @Override public java.util.Optional<PlatformEntity> spawn(String typeKey, Position position) { return java.util.Optional.empty(); }
            @Override public java.util.Optional<PlatformEntity> entity(UUID uuid) { return java.util.Optional.empty(); }
            @Override public java.util.Collection<PlatformEntity> nearbyEntities(Position center, double radius) {
                return List.of(caster, near, far);
            }
            @Override public String blockKey(int x, int y, int z) { return "minecraft:air"; }
            @Override public Object handle() { return this; }
        };
        caster.setWorld(dummyWorld);

        CoreSkillContext context = new CoreSkillContext(caster);
        List<PlatformEntity> hitEntities = new ArrayList<>();
        engine.registerMechanic("record_hit", (ctx, tgt, params) -> {
            if (tgt.entity() != null) hitEntities.add(tgt.entity());
        });

        SkillStep step = SkillStep.parse("record_hit{} @eir{r=5}");
        engine.executeStep(step, context);

        assertEquals(1, hitEntities.size());
        assertEquals(near.uuid(), hitEntities.get(0).uuid());
    }
}
