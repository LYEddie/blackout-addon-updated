package kassuk.addon.blackout.mixins.accessor;

import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundSetCameraPacket.class)
public interface ClientboundSetCameraPacketAccessor {
    @Accessor("cameraId")
    int blackoutAddon$getId();
}
