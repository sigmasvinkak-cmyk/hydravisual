package com.hydravisual.mixin;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.Module;
import com.hydravisual.module.modules.FPSDisplay;
import com.hydravisual.module.modules.CoordsDisplay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HydraVisualClient.INSTANCE == null) return;

        int y = 4;
        int color = 0xFFFFFFFF;
        int shadow = 0xFF000000;

        // FPS Display
        Module fpsModule = HydraVisualClient.INSTANCE.getModuleManager().getModule("FPS");
        if (fpsModule != null && fpsModule.isEnabled()) {
            String fpsText = ((FPSDisplay) fpsModule).getFPSText();
            context.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer, fpsText, 4, y, 0xFF00FF88, true);
            y += 12;
        }

        // Coords Display
        Module coordsModule = HydraVisualClient.INSTANCE.getModuleManager().getModule("Coords");
        if (coordsModule != null && coordsModule.isEnabled()) {
            String coordsText = ((CoordsDisplay) coordsModule).getCoordsText();
            context.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer, coordsText, 4, y, 0xFFAABBFF, true);
            y += 12;
        }

        // HydraVisual watermark
        context.drawText(net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                "HydraVisual v1.0.0", 4, y, 0x88AAAAAA, true);
    }
}
