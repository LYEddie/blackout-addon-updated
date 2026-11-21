package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.impl.PacketLogger;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import io.netty.channel.ChannelFutureListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {
    @Unique private PacketLogger packetLogger = null;
    @Unique private boolean blackoutAddon$sentPositionThisTick;

    @Shadow
    private void sendPacket(Packet<?> packet, ChannelFutureListener callbacks, boolean flush) {
        throw new AssertionError();
    }

    @Inject(method = "doSendPacket", at = @At("HEAD"))
    private void onSent(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        blackoutAddon$separatePositionPackets(packet);
        if (packetLogger == null) packetLogger = Modules.get().get(PacketLogger.class);
        if (packetLogger != null && packetLogger.isActive()) packetLogger.onSent(packet);
    }

    @Unique
    private void blackoutAddon$separatePositionPackets(Packet<?> packet) {
        if (packet instanceof ServerboundClientTickEndPacket) {
            blackoutAddon$sentPositionThisTick = false;
            return;
        }
        if (!(packet instanceof ServerboundMovePlayerPacket movement) || !movement.hasPosition()) return;

        // doSendPacket runs on the channel event loop, preserving wire order.
        if (blackoutAddon$sentPositionThisTick) {
            sendPacket(ServerboundClientTickEndPacket.INSTANCE, null, false);
        }
        blackoutAddon$sentPositionThisTick = true;
    }
}
