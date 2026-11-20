package kassuk.addon.blackout.mixins.accessor;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundInteractPacket.class)
public interface ServerboundInteractPacketAccessor {
    @Accessor("entityId")
    @Final
    @Mutable
    void blackoutAddon$setId(final int id);

    @Accessor("entityId")
    int blackoutAddon$getId();

    @Accessor("action")
    ServerboundInteractPacket.Action blackoutAddon$getType();
}

