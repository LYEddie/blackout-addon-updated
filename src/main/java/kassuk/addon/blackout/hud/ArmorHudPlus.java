package kassuk.addon.blackout.hud;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.OLEPOSSUtils;
import kassuk.addon.blackout.utils.RoundedRenderer2D;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.world.item.ItemStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author OLEPOSSU
 */

public class ArmorHudPlus extends HudElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale to render at.")
        .defaultValue(1)
        .range(0, 5)
        .sliderRange(0, 5)
        .build()
    );
    private final Setting<Integer> rounding = sgGeneral.add(new IntSetting.Builder()
        .name("rounding")
        .description("How rounded should the background be.")
        .defaultValue(50)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );
    private final Setting<Boolean> bg = sgGeneral.add(new BoolSetting.Builder()
        .name("background")
        .description("Renders a background behind armor pieces.")
        .defaultValue(false)
        .build()
    );
    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(0, 0, 0, 150))
        .build()
    );
    private final Setting<SettingColor> durColor = sgGeneral.add(new ColorSetting.Builder()
        .name("durability-color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );
    private final Setting<DurMode> durMode = sgGeneral.add(new EnumSetting.Builder<DurMode>()
        .name("durability-mode")
        .description("Where should durability be rendered at.")
        .defaultValue(DurMode.Bottom)
        .build()
    );

    public static final HudElementInfo<ArmorHudPlus> INFO = new HudElementInfo<>(BlackOut.HUD_BLACKOUT, "ArmorHud+", "A target hud the fuck you thinkin bruv.", ArmorHudPlus::new);

    public ArmorHudPlus() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (mc.player == null) return;
        double renderScale = scale.get() * 2;
        setSize(100 * renderScale, 28 * renderScale);
        if (renderScale <= 0) return;

        if (bg.get()) {
            RoundedRenderer2D.quadRounded(Renderer2D.COLOR, x, y, getWidth(), getHeight(),
                bgColor.get(), rounding.get() * 0.14 * renderScale, true);
        }
        for (int i = 3; i >= 0; i--) {
            ItemStack item = OLEPOSSUtils.armorStack(mc.player, i);
            int itemX = (int) (x + (i * 20 + 12) * renderScale);
            int itemY = (int) (y + (durMode.get() == DurMode.Top ? 10 : 0) * renderScale);
            renderer.post(() -> renderer.item(item, itemX, itemY, (float) renderScale, false));
            if (item.isEmpty()) continue;
            String durability = String.valueOf(Math.round(100 - (float) item.getDamageValue() / item.getMaxDamage() * 100f));
            double vanillaScale = renderScale / 2;
            double textScale = Hud.get().hasCustomFont() ? Math.sqrt(vanillaScale) : vanillaScale;
            renderer.text(durability,
                x + (i * 20 + 20) * renderScale - renderer.textWidth(durability, false, textScale) / 2,
                y + (durMode.get() == DurMode.Top ? 3 : 17) * renderScale,
                durColor.get(), false, textScale);
        }
    }


    public enum DurMode {
        Top, // 3, 10
        Bottom // 0, 17
    }
}
