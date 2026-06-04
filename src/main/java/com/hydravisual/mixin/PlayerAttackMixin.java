package com.hydravisual.mixin;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.modules.HitSound;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerAttackMixin {

    @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        try {
            if (!((Object) this instanceof ClientPlayerEntity player)) return;

            var mgr = HydraVisualClient.INSTANCE.getModuleManager();
            HitSound hs = mgr.getHitSoundModule();
            if (hs == null || !hs.isEnabled()) return;

            // Vanilla crit logic — must match exactly
            boolean charged = player.getAttackCooldownProgress(0.5F) > 0.9F;
            boolean isCrit = charged
                    && player.fallDistance > 0.0F
                    && !player.isOnGround()
                    && !player.isClimbing()
                    && !player.isTouchingWater()
                    && !player.hasStatusEffect(StatusEffects.BLINDNESS)
                    && !player.hasVehicle()
                    && !player.isSprinting()
                    && target instanceof LivingEntity;

            hs.onHit(target, isCrit);
        } catch (Exception ignored) {}
    }
}
