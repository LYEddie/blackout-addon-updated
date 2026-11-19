package kassuk.addon.blackout.modules.impl;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.modules.BlackOutModule;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;

public class PortalGodMode extends BlackOutModule {
    public PortalGodMode() {super(BlackOut.BLACKOUT, "portal-god-mode", "Prevents taking damage while in portals");}
    @EventHandler
    private void onSend(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket) {
            event.cancel();
        }
    }
}
