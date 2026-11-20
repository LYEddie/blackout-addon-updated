package kassuk.addon.blackout.modules.impl;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.modules.BlackOutModule;
import kassuk.addon.blackout.managers.Managers;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * @author OLEPOSSU
 */

public class StrictNoSlow extends BlackOutModule {
    public StrictNoSlow() {
        super(BlackOut.BLACKOUT, "strict-no-slow", "Should only be used on very strict servers. Requires any other noslow to work.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> onlyGap = sgGeneral.add(new BoolSetting.Builder()
        .name("only-gapples")
        .description("Only sends packets when eating gapples.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> single = sgGeneral.add(new BoolSetting.Builder()
        .name("single-packet")
        .description("Only sends 1 switch packet after starting to eat. Works on most servers that require this module.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Tick delay between switch packets.")
        .defaultValue(0)
        .min(0)
        .sliderRange(0, 20)
        .visible(() -> !single.get())
        .build()
    );

    private int timer = 0;

    @EventHandler
    private void onSend(PacketEvent.Sent event) {
        if (mc.player != null && event.packet instanceof ServerboundUseItemPacket packet) {
            if (shouldSend(packet.getHand() == InteractionHand.MAIN_HAND ? Managers.HOLDING.getStack() : mc.player.getOffhandItem())) {
                send();
                timer = 0;
            }
        }
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        if (mc.player == null || !mc.player.isUsingItem() || !shouldSend(mc.player.getUseItem())) {
            timer = 0;
            return;
        }
        timer++;
        if (timer > delay.get() && !single.get()) {
            send();
            timer = 0;
        }
    }

    private void send() {
        sendPacket(new ServerboundSetCarriedItemPacket(Managers.HOLDING.slot));
    }

    private boolean shouldSend(ItemStack stack) {
        return mc.player != null && stack != null && !stack.isEmpty()
            && (!onlyGap.get() || stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(Items.GOLDEN_APPLE));
    }
}
