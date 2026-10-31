package eu.northsoft.bettermob.paper;

import eu.northsoft.bettermob.platform.ConfigSection;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record PaperConfigSection(ConfigurationSection section) implements ConfigSection {
    public static ConfigSection of(ConfigurationSection section) {
        return section == null ? null : new PaperConfigSection(section);
    }

    @Override
    public Object get(String path) {
        return section.get(path);
    }

    @Override
    public String getString(String path) {
        return section.getString(path);
    }

    @Override
    public String getString(String path, String def) {
        return section.getString(path, def);
    }

    @Override
    public int getInt(String path, int def) {
        return section.getInt(path, def);
    }

    @Override
    public double getDouble(String path, double def) {
        return section.getDouble(path, def);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return section.getBoolean(path, def);
    }

    @Override
    public List<String> getStringList(String path) {
        return section.getStringList(path);
    }

    @Override
    public boolean contains(String path) {
        return section.contains(path);
    }

    @Override
    public boolean isConfigurationSection(String path) {
        return section.isConfigurationSection(path);
    }

    @Override
    public boolean isList(String path) {
        return section.isList(path);
    }

    @Override
    public ConfigSection getConfigurationSection(String path) {
        ConfigurationSection sub = section.getConfigurationSection(path);
        return sub == null ? null : new PaperConfigSection(sub);
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        return section.getKeys(deep);
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        return section.getValues(deep);
    }
}
