package eu.northsoft.bettermob.mob;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

public record BossBarSettings(String title, double range, BarColor color, BarStyle style,
                              boolean createFog, boolean darkenSky, boolean playMusic) {
    public static final String DEFAULT_TITLE = "<mob.name>";
    public static final double DEFAULT_RANGE = 64;

    public static BossBarSettings defaults() {
        return new BossBarSettings(DEFAULT_TITLE, DEFAULT_RANGE, BarColor.RED, BarStyle.SOLID, false, false, false);
    }
}
