package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobDefinitionParser;
import eu.northsoft.bettermob.platform.ConfigSection;
import eu.northsoft.bettermob.platform.YamlConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Manages loading and caching of MobDefinitions from the Fabric config directory.
 */
public final class FabricMobRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("bettermob");
    private final Path mobsDir;
    private final Map<String, MobDefinition> mobs = new ConcurrentHashMap<>();

    public FabricMobRegistry() {
        this(FabricLoader.getInstance().getConfigDir().resolve("bettermob").resolve("mobs"));
    }

    public FabricMobRegistry(Path mobsDir) {
        this.mobsDir = mobsDir;
    }

    public void load() {
        ensureDefaultTemplates();
        Map<String, MobDefinition> loaded = new LinkedHashMap<>();

        if (Files.isDirectory(mobsDir)) {
            try (Stream<Path> stream = Files.walk(mobsDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> {
                            String name = p.getFileName().toString();
                            return name.endsWith(".yml") || name.endsWith(".yaml");
                        })
                        .sorted()
                        .forEach(file -> loadFile(file, loaded));
            } catch (IOException e) {
                LOGGER.error("Failed to read mobs directory: {}", mobsDir, e);
            }
        }

        mobs.clear();
        mobs.putAll(loaded);
        LOGGER.info("Loaded {} BetterMob definition(s) from {}", mobs.size(), mobsDir);
    }

    private void loadFile(Path file, Map<String, MobDefinition> target) {
        String fileName = file.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String fileId = dot > 0 ? fileName.substring(0, dot) : fileName;

        try {
            ConfigSection root = YamlConfig.load(file.toFile());
            Map<String, ConfigSection> sections = MobDefinitionParser.extractSections(fileId, root);
            for (Map.Entry<String, ConfigSection> entry : sections.entrySet()) {
                String id = entry.getKey().toLowerCase(Locale.ROOT);
                if (target.containsKey(id)) {
                    LOGGER.warn("Duplicate mob ID '{}' found in {}, skipping", id, file);
                    continue;
                }
                MobDefinition def = MobDefinitionParser.parse(id, entry.getValue(), LOGGER::warn);
                if (def != null) {
                    target.put(id, def);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse mob file {}", file, e);
        }
    }

    private void ensureDefaultTemplates() {
        if (!Files.exists(mobsDir)) {
            try {
                Files.createDirectories(mobsDir);
                saveDefaultTemplate("mobs/skeleton_knight.yml");
                saveDefaultTemplate("mobs/zombie_brute.yml");
                saveDefaultTemplate("mobs/nm_wumpus.yml");
            } catch (IOException e) {
                LOGGER.error("Failed to create default mobs directory at {}", mobsDir, e);
            }
        }
    }

    private void saveDefaultTemplate(String resourcePath) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) return;
            Path target = mobsDir.resolve(new File(resourcePath).getName());
            if (!Files.exists(target)) {
                Files.copy(in, target);
                LOGGER.info("Created default template: {}", target.getFileName());
            }
        } catch (IOException e) {
            LOGGER.warn("Could not copy default template {}", resourcePath, e);
        }
    }

    public MobDefinition get(String id) {
        if (id == null) return null;
        return mobs.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, MobDefinition> all() {
        return Collections.unmodifiableMap(mobs);
    }
}
