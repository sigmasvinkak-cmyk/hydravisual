package com.hydravisual.mixin;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.ModuleManager;
import com.hydravisual.module.modules.Fullbright;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts SimpleOption.getValue() for the gamma option to return
 * Fullbright's custom brightness value (bypasses the 0.0–1.0 clamp).
 */
@Mixin(SimpleOption.class)
public class GammaOptionMixin<T> {

    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void onGetValue(CallbackInfoReturnable<T> cir) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.options == null) return;
            // Only intercept the gamma option
            if ((Object) this != client.options.getGamma()) return;

            if (HydraVisualClient.INSTANCE == null) return;
            ModuleManager mgr = HydraVisualClient.INSTANCE.getModuleManager();
            if (mgr == null) return;

            Fullbright fb = mgr.getFullbrightModule();
            if (fb != null && fb.isEnabled()) {
                cir.setReturnValue((T) Double.valueOf(fb.getBrightness()));
            }
        } catch (Exception ignored) {}
    }
}
