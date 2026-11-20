package kassuk.addon.blackout.mixins.accessor;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface ServerboundMovePlayerPacketAccessor {
    @Accessor("x")
    double blackoutAddon$getX();

    @Accessor("y")
    double blackoutAddon$getY();

    @Accessor("z")
    double blackoutAddon$getZ();
}
