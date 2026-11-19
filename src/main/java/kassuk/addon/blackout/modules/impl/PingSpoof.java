package kassuk.addon.blackout.modules.impl;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.modules.BlackOutModule;
import meteordevelopment.meteorclient.settings.*;

/**
 * @author OLEPOSSU
 */

public class PingSpoof extends BlackOutModule {
    public PingSpoof() {
        super(BlackOut.BLACKOUT, "ping-spoof", "Increases your ping.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> keepAlive = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-alive")
        .description("Delays keep alive packets.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> pong = sgGeneral.add(new BoolSetting.Builder()
        .name("pong")
        .description("Delays pong packets.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Integer> ping = sgGeneral.add(new IntSetting.Builder()
        .name("bonus-ping")
        .description("Increases your ping by this much.")
        .defaultValue(69)
        .min(0)
        .sliderRange(0, 1000)
        .build()
    );
}
