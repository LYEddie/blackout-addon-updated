package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.impl.PacketLogger;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.ClientConnection;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Unique private PacketLogger packetLogger = null;

    @Inject(method = "sendInternal", at = @At("HEAD"))
    private void onSent(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        if (packetLogger == null) packetLogger = Modules.get().get(PacketLogger.class);
        if (packetLogger != null && packetLogger.isActive()) packetLogger.onSent(packet);
    }
}
