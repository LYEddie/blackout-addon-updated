package kassuk.addon.blackout.managers.impl;

import kassuk.addon.blackout.modules.impl.PingSpoof;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author OLEPOSSU
 */
public class PingSpoofManager {
    private final Queue<DelayedPacket> delayed = new ConcurrentLinkedQueue<>();

    public PingSpoofManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        var handler = mc.getNetworkHandler();
        if (handler == null) {
            delayed.clear();
            return;
        }

        ClientConnection connection = handler.getConnection();
        long now = System.currentTimeMillis();
        delayed.removeIf(d -> {
            if (d.connection != connection || !d.connection.isOpen()) return true;
            if (now < d.time) return false;
            d.connection.send(d.packet);
            return true;
        });
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        delayed.clear();
    }

    public void addKeepAlive(ClientConnection connection, long id) {
        add(connection, new KeepAliveC2SPacket(id));
    }

    public void addPong(ClientConnection connection, int id) {
        add(connection, new CommonPongC2SPacket(id));
    }

    private void add(ClientConnection connection, Packet<?> packet) {
        delayed.add(new DelayedPacket(connection, packet,
            System.currentTimeMillis() + Modules.get().get(PingSpoof.class).ping.get()));
    }

    private record DelayedPacket(ClientConnection connection, Packet<?> packet, long time) {}
}
