package kassuk.addon.blackout.hud;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.utils.RoundedRenderer2D;
import com.mojang.blaze3d.systems.RenderSystem;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import net.minecraft.client.gui.DrawContext;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.*;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author KassuK
 * @author OLEPOSSU
 */

public class TargetHud extends HudElement {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("What mode to use for the TargetHud.")
        .defaultValue(Mode.Blackout)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale to render at")
        .defaultValue(1)
        .range(0, 5)
        .sliderRange(0, 5)
        .build()
    );
    private final Setting<SettingColor> bgColor = sgGeneral.add(new ColorSetting.Builder()
        .name("background-color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(0, 0, 0, 200))
        .build()
    );
    private final Setting<SettingColor> textColor = sgGeneral.add(new ColorSetting.Builder()
        .name("text-color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );
    private final Setting<SettingColor> healthColor = sgGeneral.add(new ColorSetting.Builder()
        .name("health-color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );
    private final Setting<SettingColor> absorptionColor = sgGeneral.add(new ColorSetting.Builder()
        .name("absorption-color")
        .description(BlackOut.COLOR)
        .defaultValue(new SettingColor(255, 255, 0, 255))
        .build()
    );
    private final Setting<Double> damageTilt = sgGeneral.add(new DoubleSetting.Builder()
        .name("damage-tilt")
        .description("How many degrees should the box be rotated when enemy takes damage.")
        .defaultValue(10)
        .min(0)
        .sliderRange(0, 45)
        .build()
    );

    public static final HudElementInfo<TargetHud> INFO = new HudElementInfo<>(BlackOut.HUD_BLACKOUT, "TargetHud", "A target hud the fuck you thinkin bruv.", TargetHud::new);

    public TargetHud() {
        super(INFO);
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    private AbstractClientPlayerEntity target;
    private String renderName = null;
    private Identifier renderSkin = null;
    private float renderHealth;
    private float renderPing;

    private double scaleProgress = 0;
    private long damageTime;

    private UUID lastTarget = null;
    private float lastHp = 0;
    private boolean popped = false;

    private final Map<AbstractClientPlayerEntity, Integer> tog = new HashMap<>();

    @EventHandler(priority = 10000)
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) {
            return;
        }

        List<AbstractClientPlayerEntity> toRemove = new ArrayList<>();

        for (Map.Entry<AbstractClientPlayerEntity, Integer> entry : tog.entrySet()) {
            if (mc.world.getPlayers().contains(entry.getKey()) && !entry.getKey().isSpectator() && entry.getKey().getHealth() > 0) {
                continue;
            }

            toRemove.add(entry.getKey());
        }

        toRemove.forEach(tog::remove);

        mc.world.getPlayers().forEach(player -> {
            if (player.isOnGround()) {
                if (tog.containsKey(player)) {
                    tog.replace(player, tog.get(player) + 1);
                } else {
                    tog.put(player, 1);
                }
            }
        });

        if (target != null) {
            if (target.getUuid().equals(lastTarget)) {
                float diff = Math.max(lastHp - target.getHealth() - target.getAbsorptionAmount(), 0);

                if (diff > 1) {
                    damageTime = System.currentTimeMillis();
                }
            }
            lastTarget = target.getUuid();
            lastHp = popped ? 0 : target.getHealth() + target.getAbsorptionAmount();
            popped = false;
        } else {
            lastTarget = null;
            lastHp = 0;
            damageTime = 0;
        }
    }

    @EventHandler(priority = 10000)
    private void onReceive(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) {return;}

        if (packet.getStatus() != 35) {return;}

        Entity entity = packet.getEntity(mc.world);

        if (entity instanceof PlayerEntity player && player == target) {
            popped = true;
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        boolean blackout = mode.get() == Mode.Blackout;
        int width = blackout ? 200 : mode.get() == Mode.ExhibitionOld ? 240 : 190;
        int height = blackout ? 100 : 60;
        setSize(width * scale.get(), height * scale.get());
        updateTarget();
        if (renderName == null || mc.player == null || scale.get() <= 0 || (!blackout && target == null)) return;

        double animation = 1;
        float tilt = 0;
        if (blackout) {
            scaleProgress = MathHelper.clamp(scaleProgress + (target == null ? -renderer.delta : renderer.delta), 0, 1);
            animation = scaleProgress * scaleProgress * scaleProgress;
            tilt = (float) (Math.max(0, 500 - System.currentTimeMillis() + damageTime) / 500f * damageTilt.get());
        }
        if (animation <= 0) return;

        // Flush HUD batches at transform boundaries to isolate this element's animation.
        DrawContext context = renderer.drawContext;
        renderer.end();
        context.draw();
        var matrices = RenderSystem.getModelViewStack();
        matrices.pushMatrix();
        matrices.translate((float) (x + (1 - animation) * getWidth() / 2),
            (float) (y + (1 - animation) * getHeight() / 2), 0);
        matrices.scale((float) (animation * scale.get()), (float) (animation * scale.get()), 1);
        matrices.rotateZ((float) Math.toRadians(tilt));
        RenderSystem.applyModelViewMatrix();
        renderer.begin(context);
        try {
            switch (mode.get()) {
                case Blackout -> renderBlackout(renderer, width, height);
                case ExhibitionOld -> renderExhibitionOld(renderer, width, height);
                case Exhibition -> renderExhibition(renderer, width, height);
            }
        } finally {
            try {
                renderer.end();
                context.draw();
            } finally {
                matrices.popMatrix();
                RenderSystem.applyModelViewMatrix();
                renderer.begin(context);
            }
        }
    }

    private void renderBlackout(HudRenderer renderer, int width, int height) {
        RoundedRenderer2D.quadRounded(Renderer2D.COLOR, 0, 0, width, height, bgColor.get(), 15, true);
        drawFace(renderer);
        renderer.text(renderName, 60, 20, textColor.get(), false, fontScale(0.5));
        renderer.text(Math.round(renderPing) + "ms", 60, 30, textColor.get(), false, fontScale(0.5));
        String health = String.valueOf(Math.round(renderHealth * 10) / 10f);
        renderer.text(health, 20, 81 - renderer.textHeight(false, fontScale(0.5)) / 2, textColor.get(), false, fontScale(0.5));
        float barAnimation = MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true) / 10, lastHp, renderHealth);
        double barStart = Math.max(renderer.textWidth(health, false, fontScale(0.5)), renderer.textWidth("36.0", false, fontScale(0.5))) + 28;
        if (barAnimation > 0) {
            RoundedRenderer2D.quadRounded(Renderer2D.COLOR, barStart - 3, 77,
                MathHelper.clamp(barAnimation / 20, 0, 1) * (width - 30 - barStart) + 6,
                8, healthColor.get(), 3, true);
        }
        if (barAnimation > 20) {
            RoundedRenderer2D.quadRounded(Renderer2D.COLOR, barStart - 3, 77,
                MathHelper.clamp((barAnimation - 20) / 16, 0, 1) * (width - 30 - barStart) + 6,
                8, absorptionColor.get(), 3, true);
        }
    }

    private void renderExhibitionOld(HudRenderer renderer, int width, int height) {
        renderer.quad(0, 0, width, height, bgColor.get());
        renderer.quad(1, 1, 58, 58, new Color(102, 102, 102, 255));
        drawFace(renderer);
        renderer.text(renderName, 66, 4, textColor.get(), false, fontScale(1));
        renderer.text(Math.round(renderHealth * 10) / 10f + " Dist: " + Math.round(mc.player.distanceTo(target) * 10) / 10f,
            66, 35 - renderer.textHeight(false, fontScale(0.5)) / 2, textColor.get(), false, fontScale(0.5));
        int progress = (int) Math.ceil(MathHelper.clamp(renderHealth, 0, 20));
        for (int i = 0; i < 10 && progress > 0; i++, progress -= 2) {
            renderer.quad(66 + i * 16, 22, 6 * Math.min(progress, 2), 6, new Color(204, 204, 0, 255));
        }
        renderer.text("Yaw: " + Math.round(target.getYaw() * 10) / 10f + " Pitch: " + Math.round(target.getPitch() * 10) / 10f
                + " BodyYaw: " + Math.round(target.getBodyYaw() * 10) / 10f,
            66, 45 - renderer.textHeight(false, fontScale(0.5)) / 2, textColor.get(), false, fontScale(0.5));
        renderer.text("TOG: " + tog.getOrDefault(target, 0) + " HURT: " + (float) target.hurtTime + " TE: " + target.age,
            66, 55 - renderer.textHeight(false, fontScale(0.5)) / 2, textColor.get(), false, fontScale(0.5));
    }

    private void renderExhibition(HudRenderer renderer, int width, int height) {
        renderer.quad(-2, -2, width + 4, height + 4, new Color(52, 52, 52, 255));
        renderer.quad(-1, -1, width + 2, height + 2, new Color(32, 32, 32, 255));
        renderer.quad(0, 0, width, height, new Color(52, 52, 52, 255));
        renderer.text(renderName, 61.5, 3, textColor.get(), false, fontScale(0.75));
        renderer.text(Math.round(renderHealth * 10) / 10f + " Dist: " + Math.round(mc.player.distanceTo(target) * 10) / 10f,
            62.25, (40 - renderer.textHeight(false, fontScale(0.5)) / 2) * 0.75, textColor.get(), false, fontScale(0.375));
        int progress = (int) Math.ceil(MathHelper.clamp(renderHealth, 0, 20));
        for (int i = 0; i < 10 && progress > 0; i++, progress -= 2) {
            renderer.quad(61.5 + i * 12, 18, 4.5 * Math.min(progress, 2), 4.5, new Color(204, 204, 0, 255));
        }
        // DrawContext items follow the batched backgrounds and text.
        for (int i = 0; i < 4; i++) {
            ItemStack item = target.getInventory().armor.get(i);
            int itemX = (int) (((3 - i) * 20 + 42) * 1.35f);
            renderer.post(() -> renderer.item(item, itemX, 34, 1.35f, false));
        }
        ItemStack item = target.getMainHandStack();
        renderer.post(() -> renderer.item(item, 165, 34, 1.35f, false));
    }

    private double fontScale(double vanillaScale) {
        // Meteor scales custom fonts both when choosing their resolution and drawing.
        return Hud.get().hasCustomFont() ? Math.sqrt(vanillaScale) : vanillaScale;
    }

    private void drawFace(HudRenderer renderer) {
        Identifier skin = renderSkin;
        renderer.post(() -> PlayerSkinDrawer.draw(renderer.drawContext, skin, 20, 18, 32, false, false));
    }


    private void updateTarget() {
        target = null;
        if (mc.world == null || mc.player == null) {return;}

        AbstractClientPlayerEntity closest = null;
        double distance = Double.MAX_VALUE;

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {continue;}
            if (Friends.get().isFriend(player)) {continue;}

            double d = mc.player.distanceTo(player);

            if (d < distance) {
                closest = player;
                distance = d;
            }
        }

        target = closest;
        if (target == null && isInEditor()) {
            target = mc.player;
        }

        if (target != null) {
            renderName = target.getName().getString();
            renderSkin = target.getSkinTextures().texture();
            renderHealth = target.getHealth() + target.getAbsorptionAmount();

            PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(target.getUuid());
            renderPing = playerListEntry == null ? -1 : playerListEntry.getLatency();
        }
    }
    public enum Mode {
        Blackout,
        ExhibitionOld,
        Exhibition
    }
}
