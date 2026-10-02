package eu.northsoft.bettermob;

import org.bukkit.Material;

import java.util.List;

final class ItemDefinition {
    final String id;
    final Material material;
    final int model;
    final String displayName;
    final List<String> lore;
    final List<MobDefinition.SkillTrigger> skillTriggers;

    ItemDefinition(String id, Material material, int model, String displayName, List<String> lore,
                   List<MobDefinition.SkillTrigger> skillTriggers) {
        this.id = id;
        this.material = material;
        this.model = model;
        this.displayName = displayName;
        this.lore = lore;
        this.skillTriggers = skillTriggers;
    }
}
