package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.Material;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class SetBlockMechanic implements Mechanic {
    private final SkillEngine engine;

    public SetBlockMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        Material material = Material.matchMaterial(firstParam(p, "m", "material", "type", "block") == null ? "" : firstParam(p, "m", "material", "type", "block").trim());
        if (material == null || !material.isBlock()) {
            engine.plugin().messages().warn("skill.setblockUnknownMaterial", "material", firstParam(p, "m", "material", "type", "block"));
            return;
        }
        call.target().location().getBlock().setType(material);
    }
}
