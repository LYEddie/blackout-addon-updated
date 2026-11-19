package kassuk.addon.blackout.modules.impl;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.modules.BlackOutModule;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * @author OLEPOSSU
 */

public class FeetESP extends BlackOutModule {
    public FeetESP() {
        super(BlackOut.BLACKOUT, "feet-esp", "No, it doesn't show you pictures of feet.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> friend = sgGeneral.add(new BoolSetting.Builder()
        .name("friend")
        .description("Renders friends' feet.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> other = sgGeneral.add(new BoolSetting.Builder()
        .name("other")
        .description("Renders other players' feet.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> self = sgGeneral.add(new BoolSetting.Builder()
        .name("self")
        .description("Renders own feet.")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Which parts of feet should be rendered")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of the feet outlines.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Color of the feet sides.")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .build()
    );
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Renders feet inside this range.")
        .defaultValue(25)
        .min(0)
        .sliderRange(0, 25)
        .build()
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        mc.world.getPlayers().forEach(player -> {
            if (player.distanceTo(mc.player) > range.get()) return;

            if (!friend.get() && Friends.get().isFriend(player)) return;
            if (!other.get() && player != mc.player && !Friends.get().isFriend(player)) return;
            if (!self.get() && mc.player == player) return;

            float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
            render(event, new Vec3d(
                MathHelper.lerp(tickDelta, player.prevX, player.getX()),
                MathHelper.lerp(tickDelta, player.prevY, player.getY()),
                MathHelper.lerp(tickDelta, player.prevZ, player.getZ())
            ));
        });
    }

    private void render(Render3DEvent event, Vec3d vec) {
        event.renderer.sideHorizontal(vec.x - 0.3, vec.y, vec.z - 0.3, vec.x + 0.3, vec.z + 0.3, sideColor.get(), lineColor.get(), shapeMode.get());
    }
}
