package eu.northsoft.bettermob.paper;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformLivingEntity;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public class PaperLivingEntity extends PaperEntity implements PlatformLivingEntity {
    public PaperLivingEntity(LivingEntity entity) {
        super(entity);
    }

    private LivingEntity living() {
        return (LivingEntity) entity;
    }

    @Override
    public double health() {
        return living().getHealth();
    }

    @Override
    public void health(double health) {
        living().setHealth(Math.max(0, Math.min(health, maxHealth())));
    }

    @Override
    public double maxHealth() {
        AttributeInstance attribute = living().getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? living().getHealth() : attribute.getValue();
    }

    @Override
    public void maxHealth(double maxHealth) {
        AttributeInstance attribute = living().getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) attribute.setBaseValue(maxHealth);
    }

    @Override
    public void damage(double amount, PlatformEntity source) {
        if (source == null) living().damage(amount);
        else living().damage(amount, PaperAdapters.bukkit(source));
    }

    @Override
    public LivingEntity handle() {
        return living();
    }
}
