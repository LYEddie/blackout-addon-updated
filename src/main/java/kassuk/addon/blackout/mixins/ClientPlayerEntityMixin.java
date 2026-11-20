package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.managers.Managers;
import kassuk.addon.blackout.modules.impl.SwingModifier;
import kassuk.addon.blackout.modules.impl.TickShift;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;
    @Unique
    private static boolean sent = false;

    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;)V", at = @At(value = "HEAD"))
    private void swingHand(Hand hand, CallbackInfo ci) {
        Modules.get().get(SwingModifier.class).startSwing(hand);
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void sendPacketsHead(CallbackInfo ci) {
        sent = false;
    }

    @Inject(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"))
    private void onSendPacket(CallbackInfo ci) {
        sent = true;
    }

    @Inject(method = "sendMovementPackets", at = @At("TAIL"))
    private void sendPacketsTail(CallbackInfo ci) {
        if (!sent) {
            TickShift tickShift = Modules.get().get(TickShift.class);
            if (tickShift.isActive()) {
                tickShift.unSent = Math.min(tickShift.packets.get(), tickShift.unSent + 1);
            }
        }
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"))
    private void blackoutAddon$sendMovementPacket(ClientPlayNetworkHandler handler, Packet<?> packet) {
        Packet<?> result = packet;
        if (packet instanceof PlayerMoveC2SPacket.Full full) {
            result = Managers.ROTATION.onFull(full);
        } else if (packet instanceof PlayerMoveC2SPacket.PositionAndOnGround position) {
            result = Managers.ROTATION.onPositionOnGround(position);
        } else if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround look) {
            result = Managers.ROTATION.onLookAndOnGround(look);
        } else if (packet instanceof PlayerMoveC2SPacket.OnGroundOnly ground) {
            result = Managers.ROTATION.onOnlyOnground(ground);
        }
        if (result != null) handler.sendPacket(result);
    }
}