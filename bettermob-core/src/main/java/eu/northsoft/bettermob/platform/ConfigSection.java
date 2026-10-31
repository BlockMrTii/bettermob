package eu.northsoft.bettermob.platform;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Platform-neutral hierarchical configuration section, replacing direct
 * dependencies on Bukkit's {@code ConfigurationSection}.
 */
public interface ConfigSection {
    Object get(String path);

    String getString(String path);

    String getString(String path, String def);

    int getInt(String path, int def);

    double getDouble(String path, double def);

    boolean getBoolean(String path, boolean def);

    List<String> getStringList(String path);

    boolean contains(String path);

    boolean isConfigurationSection(String path);

    boolean isList(String path);

    ConfigSection getConfigurationSection(String path);

    Set<String> getKeys(boolean deep);

    Map<String, Object> getValues(boolean deep);
}
