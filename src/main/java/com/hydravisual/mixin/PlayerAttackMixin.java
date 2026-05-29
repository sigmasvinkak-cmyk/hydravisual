package com.hydravisual.mixin;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.ModuleManager;
import com.hydravisual.module.modules.HitSound;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerAttackMixin {

    /**
     * Перехватываем метод attack() у ClientPlayerEntity.
     * isCriticalHit определяется через скорость падения игрока (vanilla логика).
     */
    @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
    private void onAttack(Entity target, CallbackInfo ci) {
        try {
            // Только если это наш игрок (ClientPlayerEntity)
            if (!((Object)this instanceof ClientPlayerEntity player)) return;

            ModuleManager mgr = HydraVisualClient.INSTANCE.getModuleManager();
            HitSound hitSound = mgr.getHitSoundModule();
            if (hitSound == null || !hitSound.isEnabled()) return;

            boolean isCrit = player.fallDistance > 0.0F
                    && !player.isOnGround()
                    && !player.isClimbing()
                    && !player.isTouchingWater()
                    && !player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
                    && !player.hasVehicle()
                    && target instanceof net.minecraft.entity.LivingEntity;

            hitSound.onHit(target, isCrit);
        } catch (Exception ignored) {}
    }
}
