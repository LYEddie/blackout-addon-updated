package kassuk.addon.blackout.managers.impl;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/**
 * @author OLEPOSSU
 */

public class OnGroundManager {

    private boolean onGround;
    private boolean horizontalCollision;

    public OnGroundManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
        this.onGround = false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPacket(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket) {
            onGround = ((PlayerMoveC2SPacket) event.packet).isOnGround();
            horizontalCollision = ((PlayerMoveC2SPacket) event.packet).horizontalCollision();
        }
    }

    public boolean isOnGround() {
        return onGround;
    }

    public boolean hasHorizontalCollision() {
        return horizontalCollision;
    }
}



