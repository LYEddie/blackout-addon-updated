package kassuk.addon.blackout.modules.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.modules.BlackOutModule;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.render.FogShape;

/**
 * @author OLEPOSSU
 */

public class Fog extends BlackOutModule {
    public Fog() {
        super(BlackOut.BLACKOUT, "fog", "Customizable fog.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<FogShape> shape = sgGeneral.add(new EnumSetting.Builder<FogShape>()
        .name("shape")
        .description("Fog shape.")
        .defaultValue(FogShape.SPHERE)
        .build()
    );
    public final Setting<Double> distance = sgGeneral.add(new DoubleSetting.Builder()
        .name("distance")
        .description("How far away should the fog start rendering.")
        .defaultValue(25)
        .min(0)
        .sliderRange(0, 100)
        .build()
    );
    public final Setting<Integer> fading = sgGeneral.add(new IntSetting.Builder()
        .name("fading")
        .description("How smoothly should the fog fade.")
        .defaultValue(25)
        .min(0)
        .sliderRange(0, 1000)
        .build()
    );
    public final Setting<Double> thickness = sgGeneral.add(new DoubleSetting.Builder()
        .name("thickness")
        .description(".")
        .defaultValue(10)
        .range(1, 100)
        .sliderRange(1, 100)
        .build()
    );
    public final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Color of the fog.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );

    public void modifyFog() {
        SettingColor fogColor = color.get();
        float alpha = (float) Math.min(1, fogColor.a / 255.0 * thickness.get() / 100.0);
        RenderSystem.setShaderFogColor(fogColor.r / 255f, fogColor.g / 255f, fogColor.b / 255f, alpha);
        RenderSystem.setShaderFogStart((float) (distance.get() * 1f));
        RenderSystem.setShaderFogEnd((float) (distance.get() + fading.get()));
        RenderSystem.setShaderFogShape(shape.get());
    }
}
