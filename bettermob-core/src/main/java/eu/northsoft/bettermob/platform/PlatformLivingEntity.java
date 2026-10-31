package eu.northsoft.bettermob.platform;

/** A living entity: has health and can be damaged. */
public interface PlatformLivingEntity extends PlatformEntity {
    double health();

    void health(double health);

    double maxHealth();

    void maxHealth(double maxHealth);

    /** Deals damage, optionally attributed to a source entity. */
    void damage(double amount, PlatformEntity source);

    default void addPotionEffect(String type, int durationTicks, int amplifier) {}

    default boolean isDead() {
        return health() <= 0;
    }
}
