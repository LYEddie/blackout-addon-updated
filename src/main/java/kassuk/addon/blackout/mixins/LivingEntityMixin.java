package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.impl.SwingModifier;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "swing", at = @At("RETURN"))
    private void blackoutAddon$startSwing(InteractionHand hand, SwingAnimation animation,
                                        boolean includeSelf, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == mc.player && cir.getReturnValue()) {
            SwingModifier module = Modules.get().get(SwingModifier.class);
            if (module != null) module.startSwing(hand);
        }
    }
}
