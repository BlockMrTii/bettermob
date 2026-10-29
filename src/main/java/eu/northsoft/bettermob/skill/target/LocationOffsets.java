package eu.northsoft.bettermob.skill.target;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

final class LocationOffsets {
    private LocationOffsets() {}

    static Location apply(Location base, Map<String, String> params) {
        double forward = parseFloat(firstParam(params, "fo", "forwardoffset"), 0f);
        double side = parseFloat(firstParam(params, "so", "sideoffset"), 0f);
        double up = parseFloat(firstParam(params, "yo", "yoffset"), 0f);
        if (forward == 0 && side == 0 && up == 0) return base;
        double yaw = Math.toRadians(base.getYaw());
        Vector ahead = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector right = new Vector(-ahead.getZ(), 0, ahead.getX());
        return base.clone().add(ahead.multiply(forward)).add(right.multiply(side)).add(0, up, 0);
    }
}
