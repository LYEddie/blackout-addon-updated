package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.impl.SwingModifier;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
    @ModifyArgs(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;submitArmWithItem(Lnet/minecraft/client/renderer/state/level/PlayerRenderState;Lnet/minecraft/client/renderer/state/level/FirstPersonHandsAndItemsRenderState;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void blackoutAddon$modifySwing(Args args) {
        SwingModifier module = Modules.get().get(SwingModifier.class);
        if (module != null && module.isActive()) {
            args.set(7, module.getY(args.get(4)));
            args.set(5, module.getSwing(args.get(4)));
        }
    }
}
