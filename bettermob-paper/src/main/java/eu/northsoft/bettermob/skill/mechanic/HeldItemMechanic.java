package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.item.ItemNbt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class HeldItemMechanic implements Mechanic {
    public enum Kind { NBT, LORE, CONSUME }

    private static final int MAX_LORE_LINES = 32;
    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");

    private final Kind kind;

    public HeldItemMechanic(Kind kind) {
        this.kind = kind;
    }

    static List<String> loreLines(String raw) {
        List<String> lines = new ArrayList<>();
        if (raw == null) return lines;
        String text = raw.trim();
        Matcher matcher = QUOTED.matcher(text);
        if (text.startsWith("\"")) {
            while (matcher.find() && lines.size() < MAX_LORE_LINES) lines.add(matcher.group(1));
            return lines;
        }
        for (String line : text.split("\\|")) {
            if (lines.size() >= MAX_LORE_LINES) break;
            lines.add(line);
        }
        return lines;
    }

    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof LivingEntity living)) return;
        ItemStack stack = ItemNbt.heldItem(living, call.context().event());
        if (stack == null) return;
        switch (kind) {
            case NBT -> {
                String key = firstParam(call.params(), "key", "k");
                String value = firstParam(call.params(), "value", "v", "val");
                if (key == null) return;
                ItemNbt.set(stack, stripQuotes(key), value == null ? null : stripQuotes(value));
            }
            case LORE -> {
                ItemMeta meta = stack.getItemMeta();
                if (meta == null) return;
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines(firstParam(call.params(), "l", "lore", "lines"))) {
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
                }
                meta.lore(lore);
                stack.setItemMeta(meta);
            }
            case CONSUME -> {
                int amount = Math.max(1, Math.min(64, parseInt(firstParam(call.params(), "amount", "a"), 1)));
                stack.setAmount(Math.max(0, stack.getAmount() - amount));
            }
        }
        ItemNbt.updateHeldItem(living, call.context().event(), stack);
    }
}
