package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class EntitiesInRadiusTargeter implements Targeter {
    private final CandidateFilters filters;
    private final boolean playersOnly;
    private final int defaultLimit;
    private final boolean nearOrigin;

    public EntitiesInRadiusTargeter(CandidateFilters filters, boolean playersOnly, int defaultLimit, boolean nearOrigin) {
        this.filters = filters;
        this.playersOnly = playersOnly;
        this.defaultLimit = defaultLimit;
        this.nearOrigin = nearOrigin;
    }

    @Override
    public List<Target> resolve(Map<String, String> params, SkillContext context) {
        Location center = nearOrigin && context.origin() != null ? context.origin() : context.caster().getLocation();
        double radius = parseFloat(firstParam(params, "r", "radius"), playersOnly ? 10f : 5f);
        List<String> conditions = params.containsKey("conditions") ? SkillEngine.splitInline(params.get("conditions")) : List.of();
        List<LivingEntity> hits = new ArrayList<>();
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand || living.isDead()) continue;
            if (playersOnly && !(entity instanceof Player)) continue;
            if (living.getLocation().distanceSquared(center) > radius * radius) continue;
            if (filters.matches(living, conditions, context)) hits.add(living);
        }
        String sort = params.getOrDefault("sort", "nearest").toLowerCase(Locale.ROOT);
        switch (sort) {
            case "random" -> Collections.shuffle(hits);
            case "farthest" -> hits.sort(Comparator.comparingDouble((LivingEntity entity) -> entity.getLocation().distanceSquared(center)).reversed());
            default -> hits.sort(Comparator.comparingDouble(entity -> entity.getLocation().distanceSquared(center)));
        }
        int limit = parseInt(params.get("limit"), defaultLimit);
        List<Target> targets = new ArrayList<>();
        for (LivingEntity hit : hits) {
            if (targets.size() >= limit) break;
            targets.add(Target.ofEntity(hit));
        }
        return targets;
    }
}
