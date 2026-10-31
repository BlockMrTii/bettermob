package eu.northsoft.bettermob.mob;

public record BossBarSettings(String title, double range, BossBarColor color, BossBarStyle style,
                              boolean createFog, boolean darkenSky, boolean playMusic) {
    public static final String DEFAULT_TITLE = "<mob.name>";
    public static final double DEFAULT_RANGE = 64;

    public static BossBarSettings defaults() {
        return new BossBarSettings(DEFAULT_TITLE, DEFAULT_RANGE, BossBarColor.RED, BossBarStyle.SOLID, false, false, false);
    }
}
