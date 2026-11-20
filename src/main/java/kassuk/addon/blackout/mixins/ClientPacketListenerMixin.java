package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.impl.SoundModifier;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
    @Redirect(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private void redirect(ClientLevel instance, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch, boolean useDistance) {
        SoundModifier m = Modules.get().get(SoundModifier.class);

        if (m != null && m.isActive()) {
            if (m.expSound.get()) {
                instance.playLocalSound(x, y, z, sound, category, (float) (volume * m.explosionVolume.get()), (float) (pitch * m.explosionPitch.get()), useDistance);
            }
            return;
        }
        instance.playLocalSound(x, y, z, sound, category, volume, pitch, useDistance);
    }
}
