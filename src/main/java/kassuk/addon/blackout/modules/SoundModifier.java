package kassuk.addon.blackout.modules;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.BlackOutModule;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;

/**
 * @author OLEPOSSU
 */

public class SoundModifier extends BlackOutModule {
    public SoundModifier() {
        super(BlackOut.BLACKOUT, "sound-modifier", "Modifies sounds to make crystal pvp less horrible for ears.");
    }

    private final SettingGroup sgCrystal = settings.createGroup("Crystal");

    public final Setting<Boolean> crystalHits = sgCrystal.add(new BoolSetting.Builder()
        .name("crystal-hit-sound")
        .description("Allows hit sounds when attacking end crystal.")
        .defaultValue(true)
        .build()
    );
    public final Setting<Double> crystalHitVolume = sgCrystal.add(new DoubleSetting.Builder()
        .name("crystal-hit-volume")
        .description("Multiplies crystal hit volumes.")
        .defaultValue(1)
        .sliderRange(0, 10)
        .visible(crystalHits::get)
        .build()
    );
    public final Setting<Double> crystalHitPitch = sgCrystal.add(new DoubleSetting.Builder()
        .name("crystal-hit-pitch")
        .description("Multiplies pitch of crystal hit sounds.")
        .defaultValue(1)
        .sliderRange(0, 10)
        .visible(crystalHits::get)
        .build()
    );
    public final Setting<Boolean> expSound = sgCrystal.add(new BoolSetting.Builder()
        .name("explosion-sound")
        .description("Allows explosion sounds")
        .defaultValue(true)
        .build()
    );
    public final Setting<Double> explosionVolume = sgCrystal.add(new DoubleSetting.Builder()
        .name("explosion-volume")
        .description("Multiplies explosion volumes.")
        .defaultValue(1)
        .sliderRange(0, 10)
        .visible(expSound::get)
        .build()
    );
    public final Setting<Double> explosionPitch = sgCrystal.add(new DoubleSetting.Builder()
        .name("explosion-pitch")
        .description("Multiplies pitch of explosions sounds.")
        .defaultValue(1)
        .sliderRange(0, 10)
        .visible(expSound::get)
        .build()
    );
}
