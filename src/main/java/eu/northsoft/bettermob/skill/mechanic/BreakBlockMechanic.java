package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Material;

public final class BreakBlockMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        if (call.target().block() == null) return;
        boolean useTool = Boolean.parseBoolean(call.params().getOrDefault("usetool", "false"));
        if (useTool) call.target().block().breakNaturally();
        else call.target().block().setType(Material.AIR);
    }
}
