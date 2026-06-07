package com.hydravisual.mixin;

import com.hydravisual.HydraVisualClient;
import com.hydravisual.module.modules.ViewModel;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {

    /**
     * Push our own matrix at HEAD and apply ViewModel transforms.
     * This isolates our transforms from the other hand's rendering.
     * We pop at RETURN to keep the matrix stack balanced.
     */
    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"))
    private void onHead(
            AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack item, float equipProgress,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers,
            int light, CallbackInfo ci) {
        matrices.push();
        try {
            if (HydraVisualClient.INSTANCE == null) return;
            ViewModel vm = HydraVisualClient.INSTANCE.getModuleManager().getViewModelModule();
            if (vm == null || !vm.isEnabled()) return;

            Arm mainArm = player.getMainArm();
            boolean isRight = (hand == Hand.MAIN_HAND) == (mainArm == Arm.RIGHT);

            if (isRight) {
                matrices.translate(vm.getRX(), vm.getRY(), vm.getRZ());
                float s = vm.getRS();
                matrices.scale(s, s, s);
            } else {
                matrices.translate(vm.getLX(), vm.getLY(), vm.getLZ());
                float s = vm.getLS();
                matrices.scale(s, s, s);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Pop the matrix we pushed at HEAD, so the stack stays balanced.
     * Because we pushed before the method's own push(), our pop
     * restores the state after the method's pop() — keeping each
     * hand's transforms fully isolated.
     */
    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void onReturn(
            AbstractClientPlayerEntity player, float tickDelta, float pitch,
            Hand hand, float swingProgress, ItemStack item, float equipProgress,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers,
            int light, CallbackInfo ci) {
        matrices.pop();
    }
}
