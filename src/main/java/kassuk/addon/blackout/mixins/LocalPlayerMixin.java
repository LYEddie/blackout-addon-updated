package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.managers.Managers;
import kassuk.addon.blackout.modules.impl.TickShift;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Shadow
    @Final
    public ClientPacketListener connection;
    @Unique
    private static boolean sent = false;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void sendPacketsHead(CallbackInfo ci) {
        sent = false;
    }

    @Inject(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void onSendPacket(CallbackInfo ci) {
        sent = true;
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void sendPacketsTail(CallbackInfo ci) {
        if (!sent) {
            TickShift tickShift = Modules.get().get(TickShift.class);
            if (tickShift.isActive()) {
                tickShift.unSent = Math.min(tickShift.packets.get(), tickShift.unSent + 1);
            }
        }
    }

    @Redirect(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void blackoutAddon$sendMovementPacket(ClientPacketListener handler, Packet<?> packet) {
        Packet<?> result = packet;
        if (packet instanceof ServerboundMovePlayerPacket.PosRot full) {
            result = Managers.ROTATION.onFull(full);
        } else if (packet instanceof ServerboundMovePlayerPacket.Pos position) {
            result = Managers.ROTATION.onPositionOnGround(position);
        } else if (packet instanceof ServerboundMovePlayerPacket.Rot look) {
            result = Managers.ROTATION.onLookAndOnGround(look);
        } else if (packet instanceof ServerboundMovePlayerPacket.StatusOnly ground) {
            result = Managers.ROTATION.onOnlyOnground(ground);
        }
        if (result != null) handler.send(result);
    }
}