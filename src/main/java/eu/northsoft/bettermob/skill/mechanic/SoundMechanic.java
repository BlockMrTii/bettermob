package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Location;
import org.bukkit.SoundCategory;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class SoundMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        String name = p.get("s");
        if (name == null) return;
        float pitch = parseFloat(p.get("p"), 1f);
        float volume = parseFloat(p.get("v"), 1f);
        Location location = call.target().location();
        location.getWorld().playSound(location, name, SoundCategory.HOSTILE, volume, pitch);
    }
}
