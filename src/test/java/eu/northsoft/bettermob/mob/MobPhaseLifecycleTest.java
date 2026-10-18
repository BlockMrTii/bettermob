package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.debug.DebugManager;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.item.EquipmentSupport;
import eu.northsoft.bettermob.model.BetterModelHook;
import eu.northsoft.bettermob.model.ModelEngineHook;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillStep;
import eu.northsoft.bettermob.util.Tasks;
import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MobPhaseLifecycleTest {
    private BetterMobPlugin plugin;
    private MobManager manager;
    private LivingEntity entity;
    private SkillEngine engine;
    private ItemRegistry items;
    private PersistentDataContainer data;
    private EntityEquipment equipment;
    private MobDefinition definition;
    private final Map<NamespacedKey, Object> stored = new HashMap<>();
    private static final String YAML = """
            Phases:
              - Health: '<70%'
                Options: {Silent: true, CanMove: false, Scale: 2}
                Equipment: []
                Skills:
                  - sound{s=start} @self ~onPhase
                  - sound{s=tick} @self ~onTimer:7
                  - sound{s=move} @self ~onMove:3
                  - sound{s=signal} @self ~onSignal:rage
              - Health: '<=30%'
                Skills: []
            """;

    /** Supplies stable attribute identities without launching a Minecraft server. */
    @BeforeAll
    static void attributes() {
        RegistryAccess access = mock(RegistryAccess.class, call -> Proxy.newProxyInstance(
                Registry.class.getClassLoader(), new Class<?>[]{Registry.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getOrThrow")) {
                        return Proxy.newProxyInstance(Attribute.class.getClassLoader(), new Class<?>[]{Attribute.class},
                                (attribute, attrMethod, values) -> switch (attrMethod.getName()) {
                                    case "equals" -> attribute == values[0];
                                    case "hashCode" -> System.identityHashCode(attribute);
                                    case "toString" -> args[0].toString();
                                    default -> null;
                                });
                    }
                    return null;
                }));
        try (MockedStatic<RegistryAccess> registry = mockStatic(RegistryAccess.class)) {
            registry.when(RegistryAccess::registryAccess).thenReturn(access);
            assertNotNull(Attribute.MAX_HEALTH);
        }
    }

    /** Builds a mob with mocked entity APIs and real phase parsing and manager logic. */
    @BeforeEach
    void setup() throws Exception {
        plugin = mock(BetterMobPlugin.class);
        when(plugin.getName()).thenReturn("BetterMob");
        when(plugin.namespace()).thenReturn("bettermob");
        DebugManager debug = mock(DebugManager.class);
        when(plugin.debug()).thenReturn(debug);
        when(debug.enter(any())).thenReturn((Runnable) () -> {});
        items = mock(ItemRegistry.class);
        manager = new MobManager(plugin, mock(MobRegistry.class), mock(BetterModelHook.class), mock(ModelEngineHook.class), items);
        engine = mock(SkillEngine.class);
        manager.setSkillEngine(engine);
        entity = mock(LivingEntity.class);
        when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
        when(entity.isValid()).thenReturn(true);
        when(entity.getHealth()).thenReturn(60.0);
        AttributeInstance max = mock(AttributeInstance.class);
        when(max.getValue()).thenReturn(100.0);
        when(entity.getAttribute(Attribute.MAX_HEALTH)).thenReturn(max);
        equipment = mock(EntityEquipment.class);
        when(entity.getEquipment()).thenReturn(equipment);
        data = mock(PersistentDataContainer.class);
        when(entity.getPersistentDataContainer()).thenReturn(data);
        doAnswer(call -> { stored.put(call.getArgument(0), call.getArgument(2)); return null; })
                .when(data).set(any(), any(), any());
        when(data.get(any(), any())).thenAnswer(call -> stored.get(call.getArgument(0)));
        definition = definition(YAML);
    }

    /** Parses definitions using the same phase and option readers as the registry. */
    private static MobDefinition definition(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return new MobDefinition("boss", null, "Boss", null, 100, 1, false, List.of(), List.of(),
                MobDefinition.Options.parse(config.getConfigurationSection("Options"), MobDefinition.Options.DEFAULT), false, Map.of(),
                List.of(MobDefinition.SkillTrigger.parse("sound{s=base} @self ~onDamaged"),
                        MobDefinition.SkillTrigger.parse("sound{s=base_timer} @self ~onTimer:20"),
                        MobDefinition.SkillTrigger.parse("sound{s=base_move} @self ~onMove:5")), null, null, null,
                config.getStringList("Equipment"), null, null, null, MobPhase.parse(config, message -> fail(message)));
    }

    /** Entry runs once; healing restores base values and a later downward crossing can enter again. */
    @Test
    void entryAndHealingReplaceTriggersAndReverseValues() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(entity)).thenReturn(true);
            AttributeInstance scale = mock(AttributeInstance.class);
            when(scale.getDefaultValue()).thenReturn(1.0);
            when(entity.getAttribute(Attribute.SCALE)).thenReturn(scale);
            manager.checkHealth(entity, definition, 100, 60);
            manager.checkHealth(entity, definition, 60, 50);
            verify(scale).setBaseValue(2.0);
            assertEquals(0, manager.phaseIndex(entity.getUniqueId()));
            verify(entity).setSilent(true);
            verify(entity).setAI(false);
            verify(equipment).clear();
            verify(engine, times(1)).runStep(any(), any());
            manager.fireTrigger(entity, definition, MobDefinition.SkillTrigger.Trigger.DAMAGED, null, null);
            verify(engine, times(1)).runStep(any(), any());
            manager.checkHealth(entity, definition, 50, 100);
            assertEquals(-1, manager.phaseIndex(entity.getUniqueId()));
            verify(scale).setBaseValue(1.0);
            verify(entity).setSilent(false);
            verify(entity).setAI(true);
            manager.fireTrigger(entity, definition, MobDefinition.SkillTrigger.Trigger.DAMAGED, null, null);
            verify(engine, times(2)).runStep(any(), any());
            manager.checkHealth(entity, definition, 100, 60);
            verify(engine, times(3)).runStep(any(), any());
        }
    }

    /** Equipment replaces the prior set and healing restores the configured base equipment. */
    @Test
    void equipmentIsAppliedAndRestored() throws Exception {
        MobDefinition equipped = definition("""
                Equipment: [base:HAND]
                Phases:
                  - Health: '<70%'
                    Equipment: [phase:HAND]
                """);
        ItemStack base = mock(ItemStack.class);
        ItemStack phase = mock(ItemStack.class);
        try (MockedStatic<EquipmentSupport> support = mockStatic(EquipmentSupport.class, CALLS_REAL_METHODS);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(entity)).thenReturn(true);
            support.when(() -> EquipmentSupport.itemFor(items, "base")).thenReturn(base);
            support.when(() -> EquipmentSupport.itemFor(items, "phase")).thenReturn(phase);
            manager.checkHealth(entity, equipped, 100, 60);
            verify(equipment).setItem(EquipmentSlot.HAND, phase);
            manager.checkHealth(entity, equipped, 60, 100);
            verify(equipment).setItem(EquipmentSlot.HAND, base);
            verify(equipment, times(2)).clear();
        }
    }

    /** Loading uses persisted state without replaying entry skills, and reload reselects changed definitions. */
    @Test
    void persistenceAndReloadDoNotRepeatStartSkills() throws Exception {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class); MockedStatic<Tasks> tasks = mockStatic(Tasks.class)) {
            tasks.when(() -> Tasks.runTimer(any(), any(), anyLong(), anyLong(), any())).thenReturn((Runnable) () -> {});
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(entity)).thenReturn(true);
            manager.checkHealth(entity, definition, 100, 60);
            assertEquals(0, stored.get(new NamespacedKey(plugin, "phase")));
            clearInvocations(engine);
            MobRegistry registry = mock(MobRegistry.class);
            when(registry.get("boss")).thenReturn(definition);
            stored.put(manager.mobIdKey, "boss");
            MobManager restored = new MobManager(plugin, registry, mock(BetterModelHook.class), mock(ModelEngineHook.class), mock(ItemRegistry.class));
            restored.setSkillEngine(engine);
            restored.handleLoad(entity);
            assertEquals(0, restored.phaseIndex(entity.getUniqueId()));
            verifyNoInteractions(engine);
            MobDefinition changed = definition("Phases: [{Health: '<20%'}]");
            invoke(restored, "rebind", entity, changed);
            assertEquals(-1, restored.phaseIndex(entity.getUniqueId()));
            verify(engine, never()).runStep(any(), any());
            invoke(restored, "rebind", entity, definition("Phases: []"));
            assertEquals(-1, restored.phaseIndex(entity.getUniqueId()));
        }
    }

    /** Timer and movement callbacks use phase lines and transitions cancel the previous schedules. */
    @Test
    void phaseTimersAndMovementAreScheduledAndCancelled() throws Exception {
        List<Runnable> callbacks = new ArrayList<>();
        List<Runnable> cancels = new ArrayList<>();
        List<Long> periods = new ArrayList<>();
        try (MockedStatic<Tasks> tasks = mockStatic(Tasks.class); MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(entity)).thenReturn(true);
            tasks.when(() -> Tasks.runTimer(eq(plugin), eq(entity), anyLong(), anyLong(), any())).thenAnswer(call -> {
                callbacks.add(call.getArgument(4));
                periods.add(call.getArgument(3));
                Runnable cancel = mock(Runnable.class);
                cancels.add(cancel);
                return cancel;
            });
            invoke(manager, "scheduleTimers", entity, definition);
            manager.checkHealth(entity, definition, 100, 60);
            assertEquals(List.of(5L, 20L, 3L, 7L), periods);
            clearInvocations(engine);
            when(entity.getLocation()).thenReturn(new Location(null, 0, 0, 0), new Location(null, 1, 0, 0));
            callbacks.get(2).run();
            callbacks.get(2).run();
            callbacks.get(3).run();
            verify(engine, times(2)).runStep(any(), any());
            manager.checkHealth(entity, definition, 60, 20);
            for (Runnable cancel : cancels) verify(cancel).run();
            assertEquals(0, manager.timerCount());
            clearInvocations(engine);
            manager.checkHealth(entity, definition, 20, 60);
            verifyNoInteractions(engine);
            assertEquals(List.of(5L, 20L, 3L, 7L, 3L, 7L), periods);
            manager.checkHealth(entity, definition, 60, 100);
            assertEquals(List.of(5L, 20L, 3L, 7L, 3L, 7L, 5L, 20L), periods);
            manager.release(entity);
            assertEquals(0, manager.timerCount());
            for (Runnable cancel : cancels) verify(cancel).run();
        }
    }

    /** Invokes lifecycle boundaries directly so tests do not depend on a global server registry. */
    private static void invoke(MobManager manager, String name, LivingEntity entity, MobDefinition definition) throws Exception {
        Method method = MobManager.class.getDeclaredMethod(name, LivingEntity.class, MobDefinition.class);
        method.setAccessible(true);
        method.invoke(manager, entity, definition);
    }
}
