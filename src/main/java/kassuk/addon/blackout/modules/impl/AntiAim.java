package kassuk.addon.blackout.modules.impl;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.modules.BlackOutModule;
import kassuk.addon.blackout.enums.RotationType;
import kassuk.addon.blackout.managers.Managers;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * @author OLEPOSSU
 */

public class AntiAim extends BlackOutModule {
    public AntiAim() {
        super(BlackOut.BLACKOUT, "anti-aim", "Funi conter stik module.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgIgnore = settings.createGroup("Ignore");

    //--------------------General--------------------//
    private final Setting<Modes> mode = sgGeneral.add(new EnumSetting.Builder<Modes>()
        .name("mode")
        .description(".")
        .defaultValue(Modes.Custom)
        .build()
    );
    private final Setting<Double> enemyRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("enemy-range")
        .description("Looks at players in the range.")
        .defaultValue(20)
        .range(0, 1000)
        .sliderMin(0)
        .visible(() -> mode.get().equals(Modes.Enemy))
        .sliderMax(1000)
        .build()
    );
    private final Setting<Double> spinSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("spin-speed")
        .description("How many degrees should be turned every tick.")
        .defaultValue(5)
        .min(0)
        .sliderMin(0)
        .visible(() -> mode.get().equals(Modes.Spin))
        .sliderMax(100)
        .build()
    );
    private final Setting<Boolean> rYaw = sgGeneral.add(new BoolSetting.Builder()
        .name("random-yaw")
        .description("Sets yaw to random value.")
        .defaultValue(true)
        .visible(() -> mode.get().equals(Modes.CSGO))
        .build()
    );
    private final Setting<Boolean> rPitch = sgGeneral.add(new BoolSetting.Builder()
        .name("random-pitch")
        .description("Sets pitch to random value.")
        .defaultValue(false)
        .visible(() -> mode.get().equals(Modes.CSGO))
        .build()
    );
    private final Setting<Integer> csgoPitch = sgGeneral.add(new IntSetting.Builder()
        .name("cs-pitch")
        .description("Sets pitch to this")
        .defaultValue(90)
        .range(-90, 90)
        .sliderMin(-90)
        .visible(() -> mode.get().equals(Modes.CSGO) && !rPitch.get())
        .sliderMax(90)
        .build()
    );
    private final Setting<Double> csDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("csgo-delay")
        .description("Tick delay between csgo rotation update.")
        .defaultValue(5)
        .range(0, 100)
        .sliderMin(0)
        .visible(() -> mode.get().equals(Modes.CSGO))
        .sliderMax(100)
        .build()
    );
    private final Setting<Integer> yaw = sgGeneral.add(new IntSetting.Builder()
        .name("yaw")
        .description("Sets yaw to this")
        .defaultValue(0)
        .range(-180, 180)
        .sliderMin(-180)
        .visible(() -> mode.get().equals(Modes.Custom))
        .sliderMax(180)
        .build()
    );
    private final Setting<Integer> pitch = sgGeneral.add(new IntSetting.Builder()
        .name("pitch")
        .description("Sets pitch to this")
        .defaultValue(90)
        .range(-90, 90)
        .sliderMin(-90)
        .sliderMax(90)
        .visible(() -> mode.get().equals(Modes.Custom))
        .build()
    );
    private final Setting<Boolean> bowMode = sgGeneral.add(new BoolSetting.Builder()
        .name("look-up-with-bow")
        .description("Looks up while holding a bow.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> encMode = sgGeneral.add(new BoolSetting.Builder()
        .name("look-down-with-exp")
        .description("Looks down while holding experience bottles.")
        .defaultValue(true)
        .build()
    );

    //--------------------Ignore--------------------//
    private final Setting<Boolean> iYaw = sgIgnore.add(new BoolSetting.Builder()
        .name("ignore-yaw")
        .description("Doesn't change yaw when holding specific items.")
        .defaultValue(true)
        .build()
    );
    private final Setting<List<Item>> yItems = sgIgnore.add(new ItemListSetting.Builder()
        .name("ignore-yaw-items")
        .description("Ignores yaw rotations when holding these items.")
        .defaultValue(Items.ENDER_PEARL, Items.BOW, Items.EXPERIENCE_BOTTLE)
        .build()
    );
    private final Setting<Boolean> iPitch = sgIgnore.add(new BoolSetting.Builder()
        .name("ignore-pitch")
        .description("Doesn't change pitch when holding specific items.")
        .defaultValue(true)
        .build()
    );
    private final Setting<List<Item>> pItems = sgIgnore.add(new ItemListSetting.Builder()
        .name("ignore-pitch-items")
        .description("Ignores pitch rotations when holding these items.")
        .defaultValue(Items.ENDER_PEARL, Items.BOW, Items.EXPERIENCE_BOTTLE)
        .build()
    );

    private final Random r = new Random();
    private double spinYaw;
    private double csTick = 0;
    private double csYaw;
    private double csPitch;

    @Override
    public void onActivate() {
        super.onActivate();
        spinYaw = 0;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onTick(TickEvent.Pre event) {
        if (mc.player != null && mc.world != null) {
            if (mode.get() == Modes.CSGO) {
                if (csTick <= 0) {
                    csTick += csDelay.get();
                    csYaw = rYaw.get() ? r.nextInt(-180, 180) : mc.player.getYaw();
                    csPitch = rPitch.get() ? r.nextInt(-90, 90) : csgoPitch.get();
                } else {
                    csTick--;
                }
            }

            Item item = mc.player.getMainHandStack().getItem();
            boolean ignoreYaw = yItems.get().contains(item) && iYaw.get();
            boolean ignorePitch = pItems.get().contains(item) && iPitch.get();

            double y = ignoreYaw ? mc.player.getYaw() :
                switch (mode.get()) {
                    case Enemy -> closestYaw();
                    case Spin -> getSpinYaw();
                    case CSGO -> csYaw;
                    case Custom -> yaw.get();
                };

            double p = item == Items.EXPERIENCE_BOTTLE && encMode.get() ? 90 :
                item == Items.BOW && bowMode.get() ? -90 :
                    ignorePitch ? mc.player.getPitch() :
                        switch (mode.get()) {
                            case Enemy -> closestPitch();
                            case Spin -> 0.0;
                            case CSGO -> csPitch;
                            case Custom -> pitch.get();
                        };

            Managers.ROTATION.start(y, p, priority, RotationType.Other, Objects.hash(name + "look"));
        }
    }

    @Override
    public String getInfoString() {
        return mode.get().name();
    }

    private double closestYaw() {
        PlayerEntity closest = getClosest();

        if (closest != null) {
            return Rotations.getYaw(closest);
        }
        return mc.player.getYaw();
    }

    private double closestPitch() {
        PlayerEntity closest = getClosest();

        if (closest != null) {
            return Rotations.getPitch(closest);
        }
        return mc.player.getPitch();
    }

    private double getSpinYaw() {
        spinYaw += spinSpeed.get();

        return spinYaw;
    }

    private PlayerEntity getClosest() {
        PlayerEntity closest = null;
        for (PlayerEntity pl : mc.world.getPlayers()) {
            if (pl == mc.player) continue;

            if (Friends.get().isFriend(pl)) continue;

            if (closest == null) closest = pl;

            double distance = mc.player.getEntityPos().distanceTo(pl.getEntityPos());

            if (distance > enemyRange.get()) continue;

            if (distance < closest.getEntityPos().distanceTo(mc.player.getEntityPos())) {
                closest = pl;
            }
        }
        return closest;
    }

    public enum Modes {
        Enemy,
        Spin,
        CSGO,
        Custom
    }
}
