package kassuk.addon.blackout.mixins.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;

@Mixin(ServerboundTeleportToEntityPacket.class)
public interface ServerboundTeleportToEntityPacketAccessor {
    @Accessor("uuid")
    UUID blackoutAddon$getID();
}
