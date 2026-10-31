package eu.northsoft.bettermob.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory {@link ConfigSection} backed by a nested {@link Map}.
 */
public class MapConfigSection implements ConfigSection {
    private final Map<String, Object> values;

    public MapConfigSection(Map<String, Object> values) {
        this.values = values == null ? new LinkedHashMap<>() : values;
    }

    @Override
    public Object get(String path) {
        if (path == null || path.isEmpty()) return values;
        String[] parts = path.split("\\.", 2);
        Object current = values.get(parts[0]);
        if (parts.length == 1 || current == null) return current;
        if (current instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> subMap = (Map<String, Object>) map;
            return new MapConfigSection(subMap).get(parts[1]);
        }
        if (current instanceof ConfigSection section) {
            return section.get(parts[1]);
        }
        return null;
    }

    @Override
    public String getString(String path) {
        Object val = get(path);
        return val == null ? null : String.valueOf(val);
    }

    @Override
    public String getString(String path, String def) {
        String val = getString(path);
        return val == null ? def : val;
    }

    @Override
    public int getInt(String path, int def) {
        Object val = get(path);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    @Override
    public double getDouble(String path, double def) {
        Object val = get(path);
        if (val instanceof Number n) return n.doubleValue();
        if (val instanceof String s) {
            try { return Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        Object val = get(path);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) {
            if ("true".equalsIgnoreCase(s.trim())) return true;
            if ("false".equalsIgnoreCase(s.trim())) return false;
        }
        return def;
    }

    @Override
    public List<String> getStringList(String path) {
        Object val = get(path);
        if (!(val instanceof List<?> list)) return Collections.emptyList();
        List<String> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item != null) result.add(String.valueOf(item));
        }
        return result;
    }

    @Override
    public boolean contains(String path) {
        return get(path) != null;
    }

    @Override
    public boolean isConfigurationSection(String path) {
        Object val = get(path);
        return val instanceof Map || val instanceof ConfigSection;
    }

    @Override
    public boolean isList(String path) {
        return get(path) instanceof List;
    }

    @Override
    public ConfigSection getConfigurationSection(String path) {
        Object val = get(path);
        if (val instanceof ConfigSection section) return section;
        if (val instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> subMap = (Map<String, Object>) map;
            return new MapConfigSection(subMap);
        }
        return null;
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        if (!deep) return Collections.unmodifiableSet(values.keySet());
        Set<String> keys = new LinkedHashSet<>();
        collectKeys("", values, keys);
        return Collections.unmodifiableSet(keys);
    }

    private void collectKeys(String prefix, Map<String, Object> map, Set<String> keys) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            keys.add(key);
            if (entry.getValue() instanceof Map<?, ?> sub) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) sub;
                collectKeys(key, subMap, keys);
            }
        }
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        if (!deep) return Collections.unmodifiableMap(values);
        Map<String, Object> result = new LinkedHashMap<>();
        collectValues("", values, result);
        return Collections.unmodifiableMap(result);
    }

    private void collectValues(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            result.put(key, entry.getValue());
            if (entry.getValue() instanceof Map<?, ?> sub) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) sub;
                collectValues(key, subMap, result);
            }
        }
    }
}
