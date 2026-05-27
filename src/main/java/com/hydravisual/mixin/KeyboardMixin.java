package com.hydravisual.mixin;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.Module;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.glfw.GLFW;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) return;
        if (HydraVisualClient.INSTANCE == null) return;

        // Right Shift — toggle Fullbright
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            Module fullbright = HydraVisualClient.INSTANCE.getModuleManager().getModule("Fullbright");
            if (fullbright != null) fullbright.toggle();
        }

        // H — toggle HUD (FPS + Coords)
        if (key == GLFW.GLFW_KEY_H && modifiers == GLFW.GLFW_MOD_ALT) {
            Module fps = HydraVisualClient.INSTANCE.getModuleManager().getModule("FPS");
            Module coords = HydraVisualClient.INSTANCE.getModuleManager().getModule("Coords");
            if (fps != null) fps.toggle();
            if (coords != null) coords.toggle();
        }
    }
}
