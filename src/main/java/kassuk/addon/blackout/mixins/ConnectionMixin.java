package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.impl.PacketLogger;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import io.netty.channel.ChannelFutureListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Unique private PacketLogger packetLogger = null;

    @Inject(method = "doSendPacket", at = @At("HEAD"))
    private void onSent(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        if (packetLogger == null) packetLogger = Modules.get().get(PacketLogger.class);
        if (packetLogger != null && packetLogger.isActive()) packetLogger.onSent(packet);
    }
}
