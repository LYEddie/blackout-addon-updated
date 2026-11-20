package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.utils.meteor.BlackOutTab;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Tabs.class, remap = false)
public abstract class TabsMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private static void blackoutAddon$registerTab(CallbackInfo ci) {
        Tabs.add(new BlackOutTab());
    }
}
