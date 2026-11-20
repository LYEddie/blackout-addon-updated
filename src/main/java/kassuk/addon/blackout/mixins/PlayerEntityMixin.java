package kassuk.addon.blackout.mixins;

import kassuk.addon.blackout.modules.impl.AntiCrawl;
import kassuk.addon.blackout.modules.impl.SoundModifier;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Unique
    private Entity blackoutAddon$attackEntity;

    @Inject(method = "attack", at = @At(value = "HEAD"))
    private void inject(Entity target, CallbackInfo ci) {
        blackoutAddon$attackEntity = target;
    }

    @Redirect(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/Entity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"))
    private void modifyCrystalHitSound(World instance, Entity except, double x, double y, double z, SoundEvent sound, SoundCategory category, float volume, float pitch) {
        SoundModifier m = Modules.get().get(SoundModifier.class);

        if ((Object) this == mc.player && blackoutAddon$attackEntity instanceof EndCrystalEntity && m != null && m.isActive()) {
            if (m.crystalHits.get()) {
                instance.playSound(except, x, y, z, sound, category, (float) (volume * m.crystalHitVolume.get()), (float) (pitch * m.crystalHitPitch.get()));
            }
            return;
        }
        instance.playSound(except, x, y, z, sound, category, volume, pitch);
    }

    @Inject(method = "canChangeIntoPose", at = @At("RETURN"), cancellable = true)
    private void allowStandingPose(EntityPose pose, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == mc.player && pose == EntityPose.STANDING && Modules.get().isActive(AntiCrawl.class)) {
            cir.setReturnValue(true);
        }
    }
}
