package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformLivingEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.server.world.ServerWorld;

public class FabricLivingEntity extends FabricEntity implements PlatformLivingEntity {
    public FabricLivingEntity(LivingEntity entity) {
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
        living().setHealth((float) Math.max(0, Math.min(health, maxHealth())));
    }

    @Override
    public double maxHealth() {
        return living().getMaxHealth();
    }

    @Override
    public void maxHealth(double maxHealth) {
        EntityAttributeInstance attribute = living().getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (attribute != null) attribute.setBaseValue(maxHealth);
    }

    @Override
    public void damage(double amount, PlatformEntity source) {
        if (living().getEntityWorld() instanceof ServerWorld sw) {
            if (source != null && source.handle() instanceof net.minecraft.entity.Entity srcEntity) {
                living().damage(sw, sw.getDamageSources().mobAttack(srcEntity instanceof LivingEntity l ? l : null), (float) amount);
            } else {
                living().damage(sw, sw.getDamageSources().generic(), (float) amount);
            }
        }
    }

    @Override
    public LivingEntity handle() {
        return living();
    }
}
