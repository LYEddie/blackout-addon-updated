package kassuk.addon.blackout.mixins.accessor;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClipContext.class)
public interface ClipContextAccessor {
    @Mutable
    @Accessor("from")
    void blackoutAddon$setStart(Vec3 start);

    @Mutable
    @Accessor("to")
    void blackoutAddon$setEnd(Vec3 end);
}
