package kassuk.addon.blackout.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ModulesScreen.class, remap = false)
public abstract class ModulesScreenMixin {
    @Unique private boolean blackoutAddon$loggedHiddenCategoryIcons;

    @ModifyExpressionValue(method = "createCategory", at = @At(value = "INVOKE",
        target = "Lmeteordevelopment/meteorclient/gui/GuiTheme;categoryIcons()Z"))
    private boolean blackoutAddon$safeCategoryIcons(boolean enabled) {
        // Registry components become available after the first world connection.
        // Checking the binding also allows icons after disconnecting from a world.
        if (!enabled || Items.AIR.builtInRegistryHolder().areComponentsBound()) return enabled;

        if (!blackoutAddon$loggedHiddenCategoryIcons) {
            BlackOut.LOG.info("Temporarily hid Meteor category icons because item components are not bound yet; the category-icons setting was preserved.");
            blackoutAddon$loggedHiddenCategoryIcons = true;
        }
        return false;
    }
}
