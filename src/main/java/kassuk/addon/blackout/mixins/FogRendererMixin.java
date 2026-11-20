package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.impl.Fog;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @ModifyArgs(method = "applyFog(Lnet/minecraft/client/render/Camera;IZLnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void blackoutAddon$modifyFog(Args args) {
        Fog fog = Modules.get().get(Fog.class);
        if (fog != null && fog.isActive()) fog.modifyFog(args);
    }
}
