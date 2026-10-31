package eu.northsoft.bettermob.fabric.mixin;

import eu.northsoft.bettermob.fabric.BetterMobFabricMod;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "heal", at = @At("TAIL"))
    private void bettermob$onHeal(float amount, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.getEntityWorld().isClient() && BetterMobFabricMod.instance() != null) {
            BetterMobFabricMod.instance().mobManager().onHeal(entity, amount);
        }
    }
}
