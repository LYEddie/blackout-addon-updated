package kassuk.addon.blackout.managers.impl;

import kassuk.addon.blackout.modules.impl.PingSpoof;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
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
        var handler = mc.getConnection();
        if (handler == null) {
            delayed.clear();
            return;
        }

        Connection connection = handler.getConnection();
        long now = System.currentTimeMillis();
        delayed.removeIf(d -> {
            if (d.connection != connection || !d.connection.isConnected()) return true;
            if (now < d.time) return false;
            d.connection.send(d.packet);
            return true;
        });
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        delayed.clear();
    }

    public void addKeepAlive(Connection connection, long id) {
        add(connection, new ServerboundKeepAlivePacket(id));
    }

    public void addPong(Connection connection, int id) {
        add(connection, new ServerboundPongPacket(id));
    }

    private void add(Connection connection, Packet<?> packet) {
        delayed.add(new DelayedPacket(connection, packet,
            System.currentTimeMillis() + Modules.get().get(PingSpoof.class).ping.get()));
    }

    private record DelayedPacket(Connection connection, Packet<?> packet, long time) {}
}
