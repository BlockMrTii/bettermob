package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.BetterMobPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InlineSkills {
    private static final int CACHE_LIMIT = 4096;

    private final BetterMobPlugin plugin;
    private final Map<String, List<SkillStep>> cache = new ConcurrentHashMap<>();

    InlineSkills(BetterMobPlugin plugin) {
        this.plugin = plugin;
    }

    List<SkillStep> get(String raw) {
        List<SkillStep> cached = cache.get(raw);
        if (cached != null) return cached;
        if (cache.size() >= CACHE_LIMIT) cache.clear();
        return cache.computeIfAbsent(raw, this::parse);
    }

    private List<SkillStep> parse(String raw) {
        List<SkillStep> steps = new ArrayList<>();
        for (String line : splitInline(raw)) {
            SkillStep step = SkillStep.parse(line);
            if (step == null) plugin.messages().warn("skill.inlineLineInvalid", "line", line);
            else steps.add(step);
        }
        return List.copyOf(steps);
    }

    public static List<String> splitInline(String raw) {
        String body = raw.trim();
        if (body.startsWith("[")) body = body.substring(1);
        if (body.endsWith("]")) body = body.substring(0, body.length() - 1);

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
            boolean separator = c == '-' && depth == 0 && (i == 0 || Character.isWhitespace(body.charAt(i - 1)))
                    && i + 1 < body.length() && Character.isWhitespace(body.charAt(i + 1));
            if (separator) {
                lines.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        lines.add(current.toString().trim());
        lines.removeIf(String::isBlank);
        return lines;
    }
}
