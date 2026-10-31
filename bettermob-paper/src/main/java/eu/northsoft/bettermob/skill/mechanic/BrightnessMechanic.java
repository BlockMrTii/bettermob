package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class BrightnessMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        if (!(entity instanceof Display display)) return;
        int block = Math.max(0, Math.min(15, parseInt(firstParam(call.params(), "b", "block"), 15)));
        int sky = Math.max(0, Math.min(15, parseInt(firstParam(call.params(), "s", "sky"), 15)));
        display.setBrightness(new Display.Brightness(block, sky));
    }
}
