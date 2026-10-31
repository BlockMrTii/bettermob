package eu.northsoft.bettermob.platform;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Platform-neutral YAML loader backed by SnakeYAML SafeConstructor.
 */
public final class YamlConfig {
    private YamlConfig() {}

    public static ConfigSection load(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return load(in);
        }
    }

    public static ConfigSection load(InputStream in) {
        return load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    public static ConfigSection load(String yaml) {
        return load(new StringReader(yaml));
    }

    public static ConfigSection load(Reader reader) {
        LoaderOptions options = new LoaderOptions();
        options.setMaxAliasesForCollections(100);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        Object loaded = yaml.load(reader);
        if (loaded instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) map;
            return new MapConfigSection(cast);
        }
        return new MapConfigSection(new LinkedHashMap<>());
    }
}
