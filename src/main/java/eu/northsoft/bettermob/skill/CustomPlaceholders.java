package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.api.CustomPlaceholder;
import eu.northsoft.bettermob.api.PlaceholderContext;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CustomPlaceholders {
    private static final Pattern PLACEHOLDER = Pattern.compile("<([a-z0-9_]+)\\.([A-Za-z0-9_.]+)>");

    private record Entry(Plugin owner, CustomPlaceholder placeholder) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean register(Plugin owner, String namespace, CustomPlaceholder placeholder) {
        String key = namespace.toLowerCase(Locale.ROOT);
        if (key.equals("caster") || key.equals("target")) return false;
        return entries.putIfAbsent(key, new Entry(owner, placeholder)) == null;
    }

    public void unregister(String namespace) {
        entries.remove(namespace.toLowerCase(Locale.ROOT));
    }

    public void unregister(Plugin owner) {
        entries.values().removeIf(entry -> Objects.equals(entry.owner(), owner));
    }

    public String apply(String value, SkillContext context) {
        if (entries.isEmpty() || value.indexOf('<') < 0) return value;
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            Entry entry = entries.get(matcher.group(1));
            String replacement = matcher.group(0);
            if (entry != null) {
                try {
                    String resolved = entry.placeholder().resolve(matcher.group(2), new PlaceholderContext(context.caster(), context.trigger()));
                    if (resolved != null) replacement = Params.stripPlaceholderValue(resolved);
                } catch (RuntimeException exception) {
                    replacement = matcher.group(0);
                }
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
