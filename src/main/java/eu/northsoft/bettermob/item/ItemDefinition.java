package eu.northsoft.bettermob.item;

import eu.northsoft.bettermob.mob.MobDefinition;
import org.bukkit.Material;

import java.util.List;

public final class ItemDefinition {
    public final String id;
    public final Material material;
    public final int model;
    public final String displayName;
    public final List<String> lore;
    public final List<MobDefinition.SkillTrigger> skillTriggers;

    public ItemDefinition(String id, Material material, int model, String displayName, List<String> lore,
                   List<MobDefinition.SkillTrigger> skillTriggers) {
        this.id = id;
        this.material = material;
        this.model = model;
        this.displayName = displayName;
        this.lore = lore;
        this.skillTriggers = skillTriggers;
    }
}
