package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.drop.DropRegistry;
import eu.northsoft.bettermob.drop.DropTable;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class LootMechanic implements Mechanic {
    private final SkillEngine engine;
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    public LootMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        Location at = call.target().location();
        if (at == null || at.getWorld() == null) return;
        Player player = call.target().entity() instanceof Player found ? found : null;
        boolean drop = "drop".equalsIgnoreCase(firstParam(p, "mode", "target"));
        if (!drop && player == null) return;

        List<ItemStack> items = new ArrayList<>();
        int exp = 0;
        String tableId = firstParam(p, "table", "droptable");
        String vanilla = p.get("vanilla");
        if (tableId != null) {
            DropRegistry drops = engine.drops();
            DropTable table = drops == null ? null : drops.get(tableId);
            if (table == null) {
                warnOnce("skill.lootUnknownTable", "table", tableId);
                return;
            }
            DropRegistry.Result result = drops.roll(table);
            items.addAll(result.items());
            exp = result.exp();
        } else if (vanilla != null) {
            items.addAll(rollVanilla(vanilla, call, player, at, parseInt(firstParam(p, "lootingmodifier", "looting"), 0)));
        } else {
            return;
        }

        if (engine.debug().verbose()) engine.debug().verbose("loot " + (tableId != null ? tableId : vanilla) + " gave " + items.size() + " stack(s), " + exp + " exp", engine.subject(call.context().caster()));
        if (drop) {
            for (ItemStack stack : items) at.getWorld().dropItemNaturally(at, stack);
            int orbExp = exp;
            if (orbExp > 0) at.getWorld().spawn(at, ExperienceOrb.class, orb -> orb.setExperience(orbExp));
            return;
        }
        for (ItemStack stack : items) {
            for (ItemStack left : player.getInventory().addItem(stack).values()) player.getWorld().dropItem(player.getLocation(), left);
        }
        if (exp > 0) player.giveExp(exp);
    }

    private Collection<ItemStack> rollVanilla(String id, MechanicCall call, Player player, Location at, int looting) {
        NamespacedKey key = NamespacedKey.fromString(id.trim().toLowerCase(java.util.Locale.ROOT));
        LootTable table = key == null ? null : Bukkit.getLootTable(key);
        if (table == null) {
            warnOnce("skill.lootUnknownVanilla", "table", id);
            return List.of();
        }
        LivingEntity caster = call.context().caster();
        LootContext.Builder builder = new LootContext.Builder(at).lootedEntity(caster).lootingModifier(looting);
        if (player != null) builder.killer(player);
        return table.populateLoot(new Random(), builder.build());
    }

    private void warnOnce(String key, String name, String value) {
        if (warned.add(key + value)) engine.plugin().messages().warn(key, name, value);
    }
}
