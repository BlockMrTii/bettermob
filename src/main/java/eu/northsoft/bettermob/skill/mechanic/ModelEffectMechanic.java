package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.item.EquipmentSupport;
import eu.northsoft.bettermob.model.BetterModelHook;
import eu.northsoft.bettermob.model.ModelEngineHook;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseNumber;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class ModelEffectMechanic implements Mechanic {
    public enum Kind { TINT, GLOW, PART, ITEM, LOCK, MODIFY, DEFAULT }

    private final SkillEngine engine;
    private final Kind kind;

    public ModelEffectMechanic(SkillEngine engine, Kind kind) {
        this.engine = engine;
        this.kind = kind;
    }

    static int parseColor(String value, int fallback) {
        if (value == null) return fallback;
        String text = stripQuotes(value).replace("#", "").trim();
        try {
            if (text.contains(",")) {
                String[] parts = text.split(",");
                if (parts.length != 3) return fallback;
                return (clamp(parts[0]) << 16) | (clamp(parts[1]) << 8) | clamp(parts[2]);
            }
            return text.length() == 6 ? Integer.parseInt(text, 16) : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int clamp(String part) {
        return Math.max(0, Math.min(255, Integer.parseInt(part.trim())));
    }

    private static boolean flag(Map<String, String> p, boolean fallback, String... keys) {
        String value = firstParam(p, keys);
        return value == null ? fallback : !"false".equalsIgnoreCase(stripQuotes(value));
    }

    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        if (entity == null) return;
        UUID id = entity.getUniqueId();
        Object betterModel = engine.mobManager().trackerFor(id);
        Object modelEngine = betterModel == null ? engine.mobManager().modelEngineTrackerFor(id) : null;
        if (betterModel == null && modelEngine == null) return;
        Map<String, String> p = call.params();
        String part = firstParam(p, "p", "part", "pid", "bone", "b");
        if (part != null) part = stripQuotes(part);
        boolean exact = flag(p, true, "em", "exactmatch", "exact");
        BetterModelHook better = engine.betterModel();
        ModelEngineHook me = engine.modelEngine();
        switch (kind) {
            case TINT -> {
                int color = parseColor(firstParam(p, "c", "color"), 0xFFFFFF);
                if (betterModel != null) better.tint(betterModel, part, exact, color);
                else me.tint(modelEngine, part, exact, color);
            }
            case GLOW -> {
                boolean glowing = flag(p, true, "g", "glow", "glowing");
                int color = parseColor(firstParam(p, "c", "color"), 0xFFFFFF);
                if (betterModel != null) better.glow(betterModel, part, exact, glowing, color);
                else me.glow(modelEngine, part, exact, glowing, color);
            }
            case PART -> {
                boolean visible = flag(p, false, "v", "visible", "visibility");
                if (betterModel != null) better.partVisible(betterModel, part, exact, visible);
                else me.partVisible(modelEngine, part, exact, visible);
            }
            case ITEM -> {
                String itemId = firstParam(p, "i", "item", "material", "m");
                ItemStack item = null;
                if (itemId != null && !stripQuotes(itemId).equalsIgnoreCase("air")) {
                    item = EquipmentSupport.itemFor(engine.items(), stripQuotes(itemId));
                    if (item == null) {
                        engine.plugin().messages().warn("skill.displayItemUnknown", "item", itemId);
                        return;
                    }
                }
                if (betterModel != null) better.setItem(betterModel, part, exact, item);
                else me.setItem(modelEngine, part, exact, item);
            }
            case LOCK -> {
                boolean locked = flag(p, true, "l", "lock", "locked");
                if (betterModel != null) better.lockRotation(betterModel, locked);
                else me.lockRotation(modelEngine, locked);
            }
            case MODIFY -> {
                String state = firstParam(p, "s", "state", "animation", "a");
                if (state == null) return;
                String speedText = firstParam(p, "sp", "speed");
                Float speed = speedText == null ? null : (float) parseNumber(speedText, 1.0);
                String loop = firstParam(p, "l", "loop", "loopmode");
                if (betterModel != null) better.modifyAnimation(betterModel, stripQuotes(state), speed, loop);
                else me.modifyAnimation(modelEngine, stripQuotes(state), speed, loop);
            }
            case DEFAULT -> {
                String type = firstParam(p, "t", "type", "state");
                String animation = firstParam(p, "s", "animation", "a");
                if (type == null || animation == null) return;
                if (betterModel != null) {
                    engine.plugin().messages().warnOnce("betterModel.unsupported", "action", "defaultstate");
                    return;
                }
                me.defaultState(modelEngine, type.toLowerCase(Locale.ROOT), stripQuotes(animation),
                        parseFloat(firstParam(p, "li", "lerpin"), 0f), parseFloat(firstParam(p, "lo", "lerpout"), 0f),
                        parseFloat(firstParam(p, "sp", "speed"), 1f));
            }
        }
    }
}
