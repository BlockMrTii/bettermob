package eu.northsoft.bettermob.skill.condition;

import org.bukkit.block.Block;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class BlockTypes {
    private static final int CACHE_LIMIT = 1024;
    private static final Map<String, Set<String>> CACHE = new ConcurrentHashMap<>();

    private BlockTypes() {}

    static boolean contains(String paramsRaw, Block block) {
        if (paramsRaw == null) return false;
        Set<String> types = CACHE.get(paramsRaw);
        if (types == null) {
            if (CACHE.size() >= CACHE_LIMIT) CACHE.clear();
            types = parse(paramsRaw);
            CACHE.put(paramsRaw, types);
        }
        return types.contains(block.getType().name());
    }

    static Set<String> parse(String paramsRaw) {
        int eq = paramsRaw.indexOf('=');
        String list = eq < 0 ? paramsRaw : paramsRaw.substring(eq + 1);
        return Stream.of(list.split(",")).map(type -> type.trim().toUpperCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }
}
