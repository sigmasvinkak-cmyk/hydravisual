package com.hydravisual.mixin;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.modules.HitSound;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mutes vanilla player attack sounds when HitSound module is enabled.
 */
@Mixin(ClientWorld.class)
public class AttackSoundMuteMixin {

    @Inject(method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZ)V",
            at = @At("HEAD"), cancellable = true)
    private void onPlaySound(double x, double y, double z, SoundEvent sound,
                             SoundCategory category, float volume, float pitch,
                             boolean useDistance, CallbackInfo ci) {
        try {
            if (HydraVisualClient.INSTANCE == null) return;
            HitSound hs = HydraVisualClient.INSTANCE.getModuleManager().getHitSoundModule();
            if (hs == null || !hs.isEnabled()) return;

            // Check if this is a vanilla player attack sound
            String id = sound.id().getPath();
            if (id.startsWith("entity.player.attack.")) {
                ci.cancel();
            }
        } catch (Exception ignored) {}
    }
}
