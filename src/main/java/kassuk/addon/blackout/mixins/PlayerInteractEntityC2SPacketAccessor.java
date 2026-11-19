package kassuk.addon.blackout.mixins;

import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInteractEntityC2SPacket.class)
public interface PlayerInteractEntityC2SPacketAccessor {
    @Accessor("entityId")
    @Final
    @Mutable
    void blackoutAddon$setId(final int id);

    @Accessor("entityId")
    int blackoutAddon$getId();

    @Accessor("type")
    PlayerInteractEntityC2SPacket.InteractTypeHandler blackoutAddon$getType();
}

