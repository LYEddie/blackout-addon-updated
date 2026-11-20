package kassuk.addon.blackout.mixins.accessor;

import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundRotateHeadPacket.class)
public interface ClientboundRotateHeadPacketAccessor {
    @Accessor("entityId")
    int blackoutAddon$getId();
}
