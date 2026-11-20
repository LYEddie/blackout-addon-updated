package kassuk.addon.blackout.utils.meteor;

import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.List;
import java.util.Locale;
import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.enums.RotationType;
import kassuk.addon.blackout.enums.SwingState;
import kassuk.addon.blackout.enums.SwingType;
import kassuk.addon.blackout.managers.impl.RotationManager;
import kassuk.addon.blackout.mixins.accessor.ClipContextAccessor;
import kassuk.addon.blackout.utils.*;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.IVisible;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.System;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class GlobalSettingsConfig extends System<GlobalSettingsConfig> {
    private static final GlobalSettingsConfig INSTANCE = new GlobalSettingsConfig();

    private final Settings facingSettings = new Settings();
    private final Settings rangeSettings = new Settings();
    private final Settings raytraceSettings = new Settings();
    private final Settings rotationSettings = new Settings();
    private final Settings serverSettings = new Settings();
    private final Settings swingSettings = new Settings();
    public final List<Section> sections = List.of(
        new Section("Facing", facingSettings),
        new Section("Range", rangeSettings),
        new Section("Raytrace", raytraceSettings),
        new Section("Rotate", rotationSettings),
        new Section("Server", serverSettings),
        new Section("Swing", swingSettings)
    );

    private GlobalSettingsConfig() {
        super("blackout-settings");
    }

    public static GlobalSettingsConfig get() {
        return INSTANCE;
    }

    @Override
    public void init() {
        sections.forEach(section -> section.settings.registerColorSettings(null));
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        for (Section section : sections) {
            tag.put(key(section), section.settings.toTag());
        }
        return tag;
    }

    @Override
    public GlobalSettingsConfig fromTag(CompoundTag tag) {
        for (Section section : sections) {
            if (tag.getCompound(key(section)).isPresent()) {
                section.settings.fromTag(tag.getCompoundOrEmpty(key(section)));
            }
        }
        return this;
    }

    @Override
    public void load(File folder) {
        File file = folder == null ? getFile() : new File(folder, getFile().getName());
        if (file.exists()) {
            super.load(folder);
            return;
        }

        // Import the former pseudo-modules once, before Meteor drops their entries.
        File legacy = new File(file.getParentFile(), "modules.nbt");
        if (!legacy.exists()) return;
        try {
            CompoundTag tag = NbtIo.read(legacy.toPath());
            if (tag == null) return;
            boolean migrated = false;
            for (Tag element : tag.getListOrEmpty("modules")) {
                if (!(element instanceof CompoundTag module)) continue;
                for (Section section : sections) {
                    if (module.getStringOr("name", "").equalsIgnoreCase(section.name)) {
                        section.settings.fromTag(module.getCompoundOrEmpty("settings"));
                        migrated = true;
                    }
                }
            }
            if (migrated) save(folder);
        } catch (IOException exception) {
            BlackOut.LOG.error("Could not migrate BlackOut global settings", exception);
        }
    }

    private static String key(Section section) {
        return section.name.toLowerCase(Locale.ROOT);
    }
    public record Section(String name, Settings settings) {}

    // Facing
    private final SettingGroup facingSgGeneral = facingSettings.getDefaultGroup();

    //  Place Ranges
    public final Setting<Boolean> facingStrictDir = facingSgGeneral.add(new BoolSetting.Builder()
        .name("strict-direction")
        .description("Doesn't place on faces which aren't in your direction.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Boolean> facingUnblocked = facingSgGeneral.add(new BoolSetting.Builder()
        .name("unblocked")
        .description("Doesn't place on faces that have block on them.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Boolean> facingAirPlace = facingSgGeneral.add(new BoolSetting.Builder()
        .name("air-place")
        .description("Allows placing blocks in air.")
        .defaultValue(false)
        .build()
    );
    public final Setting<MaxHeight> facingMaxHeight = facingSgGeneral.add(new EnumSetting.Builder<MaxHeight>()
        .name("max-height")
        .description("Doesn't place on top sides of blocks at max height. Old: 1.12, New: 1.17+")
        .defaultValue(MaxHeight.New)
        .build()
    );


    public PlaceData facingGetPlaceDataOR(BlockPos pos, Predicate<BlockPos> predicate, boolean ignoreContainers) {
        if (pos == null) {
            return new PlaceData(null, null, false);
        }
        Direction best = null;
        if (mc.level != null && mc.player != null) {
            if (facingAirPlace.get()) {
                return new PlaceData(pos, Direction.UP, true);
            } else {
                double cDist = -1;
                for (Direction dir : Direction.values()) {

                    // Doesn't place on top of max height
                    if (facingHeightCheck(pos.relative(dir))) {
                        continue;
                    }

                    // Checks if block is an entity (chests, shulkers)
                    if (ignoreContainers && mc.level.getBlockState(pos.relative(dir)).hasBlockEntity()) {
                        continue;
                    }

                    // Test if there is block in the side and if predicate is valid
                    if (!OLEPOSSUtils.solid(pos.relative(dir)) && (predicate != null && !predicate.test(pos.relative(dir)))) {
                        continue;
                    }

                    // Strict dir check (checks if face is on opposite side of the block to player)
                    if (facingStrictDir.get() && !OLEPOSSUtils.strictDir(pos.relative(dir), dir.getOpposite())) {
                        continue;
                    }

                    // Only accepts if closer than previous accepted direction
                    double facingDist = SettingUtils.placeRangeTo(pos.relative(dir));
                    if (facingDist >= 0 && (cDist < 0 || facingDist < cDist)) {
                        best = dir;
                        cDist = facingDist;
                    }
                }
            }
        }
        return best == null ? new PlaceData(null, null, false) : new PlaceData(pos.relative(best), best.getOpposite(), true);
    }

    public PlaceData facingGetPlaceDataAND(BlockPos pos, Predicate<Direction> predicate, Predicate<BlockPos> predicatePos, boolean ignoreContainers) {
        if (pos == null) {
            return new PlaceData(null, null, false);
        }
        Direction best = null;
        if (mc.level != null && mc.player != null) {
            if (facingAirPlace.get()) {
                return new PlaceData(pos, Direction.UP, true);
            } else {
                double cDist = -1;
                for (Direction dir : Direction.values()) {

                    // Doesn't place on top of max height
                    if (facingHeightCheck(pos.relative(dir))) {
                        continue;
                    }

                    // Checks if block is an entity (chests, shulkers)
                    if (ignoreContainers && mc.level.getBlockState(pos.relative(dir)).hasBlockEntity()) {
                        continue;
                    }

                    // Test if there is block in the side and if predicate is valid
                    if (!OLEPOSSUtils.solid(pos.relative(dir)) || (predicate != null && !predicate.test(dir)) || (predicatePos != null && !predicatePos.test(pos.relative(dir)))) {
                        continue;
                    }

                    // Strict dir check (checks if face is on opposite side of the block to player)
                    if (facingStrictDir.get() && !OLEPOSSUtils.strictDir(pos.relative(dir), dir.getOpposite())) {
                        continue;
                    }

                    // Only accepts if closer than previous accepted direction
                    double facingDist = SettingUtils.placeRangeTo(pos.relative(dir));
                    if (facingDist >= 0 && (cDist < 0 || facingDist < cDist)) {
                        best = dir;
                        cDist = facingDist;
                    }
                }
            }
        }
        return best == null ? new PlaceData(null, null, false) : new PlaceData(pos.relative(best), best.getOpposite(), true);
    }

    public PlaceData facingGetPlaceData(BlockPos pos, boolean ignoreContainers) {
        if (pos == null) {
            return new PlaceData(null, null, false);
        }
        Direction best = null;
        if (mc.level != null && mc.player != null) {
            if (facingAirPlace.get()) {
                return new PlaceData(pos, Direction.UP, true);
            } else {
                double cDist = -1;
                for (Direction dir : Direction.values()) {

                    // Doesn't place on top of max height
                    if (facingHeightCheck(pos.relative(dir))) {
                        continue;
                    }

                    // Checks if block is an entity (chests, shulkers)
                    if (ignoreContainers && mc.level.getBlockState(pos.relative(dir)).hasBlockEntity() ) {
                        continue;
                    }

                    // Test if there is block in the side and if predicate is valid
                    if (!OLEPOSSUtils.solid(pos.relative(dir))) {
                        continue;
                    }

                    // Strict dir check (checks if face is on opposite side of the block to player)
                    if (facingStrictDir.get() && !OLEPOSSUtils.strictDir(pos.relative(dir), dir.getOpposite())) {
                        continue;
                    }

                    // Only accepts if closer than previous accepted direction
                    double facingDist = SettingUtils.placeRangeTo(pos.relative(dir));
                    if (facingDist >= 0 && (cDist < 0 || facingDist < cDist)) {
                        best = dir;
                        cDist = facingDist;
                    }
                }
            }
        }
        return best == null ? new PlaceData(null, null, false) : new PlaceData(pos.relative(best), best.getOpposite(), true);
    }

    public Direction facingGetPlaceOnDirection(BlockPos pos) {
        if (pos == null) {
            return null;
        }
        Direction best = null;
        if (mc.level != null && mc.player != null) {
            double cDist = -1;
            for (Direction dir : Direction.values()) {

                // Doesn't place on top of max height
                if (facingHeightCheck(pos.relative(dir))) {
                    continue;
                }

                // Unblocked check (mostly for autocrystal placement facings)
                if (facingUnblocked.get() && !(facingGetBlock(pos.relative(dir)) instanceof AirBlock)) {
                    continue;
                }

                // Strict dir check (checks if face isnt on opposite side of the block to player)
                if (facingStrictDir.get() && !OLEPOSSUtils.strictDir(pos, dir)) {
                    continue;
                }

                // Only accepts if closer than last accepted direction
                double facingDist = facingDist(pos, dir);
                if (facingDist >= 0 && (cDist < 0 || facingDist < cDist)) {
                    best = dir;
                    cDist = facingDist;
                }
            }
        }
        return best;
    }

    private boolean facingHeightCheck(BlockPos pos) {
        return pos.getY() >= switch (facingMaxHeight.get()) {
            case Old -> 255;
            case New -> 319;
            case Disabled -> 1000; // im pretty sure 1000 is enough
        };
    }

    private double facingDist(BlockPos pos, Direction dir) {
        if (mc.player == null) {
            return 0;
        }

        Vec3 vec = new Vec3(pos.getX() + dir.getStepX() / 2f, pos.getY() + dir.getStepY() / 2f, pos.getZ() + dir.getStepZ() / 2f);
        Vec3 facingDist = mc.player.getEyePosition().add(-vec.x, -vec.y, -vec.z);

        return Math.sqrt(facingDist.x * facingDist.x + facingDist.y * facingDist.y + facingDist.z * facingDist.z);
    }

    private Block facingGetBlock(BlockPos pos) {
        return mc.level.getBlockState(pos).getBlock();
    }

    public enum MaxHeight {
        Old,
        New,
        Disabled
    }

    // Range
    private final SettingGroup rangeSgPlace = rangeSettings.createGroup("Placing");
    private final SettingGroup rangeSgAttack = rangeSettings.createGroup("Attacking");
    private final SettingGroup rangeSgMining = rangeSettings.createGroup("Mining");

    //  Place Ranges
    public final Setting<Double> rangePlaceRange = rangeSgPlace.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("Range for placing.")
        .defaultValue(5.2)
        .range(0, 6)
        .sliderRange(0, 6)
        .build()
    );
    public final Setting<Double> rangePlaceRangeWalls = rangeSgPlace.add(new DoubleSetting.Builder()
        .name("place-range-walls")
        .description("Range for placing behind blocks.")
        .defaultValue(5.2)
        .range(0, 6)
        .sliderRange(0, 6)
        .build()
    );
    private final Setting<FromMode> rangePlaceRangeFrom = rangeSgPlace.add(new EnumSetting.Builder<FromMode>()
        .name("place-range-from")
        .description("Where to calculate place ranges from.")
        .defaultValue(FromMode.Eyes)
        .build()
    );
    private final Setting<PlaceRangeMode> rangePlaceRangeMode = rangeSgPlace.add(new EnumSetting.Builder<PlaceRangeMode>()
        .name("place-range-mode")
        .description("Where to calculate place ranges from.")
        .defaultValue(PlaceRangeMode.NCP)
        .build()
    );
    private final Setting<Double> rangeBlockWidth = rangeSgPlace.add(new DoubleSetting.Builder()
        .name("block-width")
        .description("How wide should the box be for closest range.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 3)
        .visible(() -> rangePlaceRangeMode.get().equals(PlaceRangeMode.CustomBox))
        .build()
    );
    private final Setting<Double> rangeBlockHeight = rangeSgPlace.add(new DoubleSetting.Builder()
        .name("block-height")
        .description("How tall should the box be for closest range.")
        .defaultValue(2)
        .min(0)
        .sliderRange(0, 3)
        .visible(() -> rangePlaceRangeMode.get().equals(PlaceRangeMode.CustomBox))
        .build()
    );
    private final Setting<Double> rangePlaceHeight = rangeSgPlace.add(new DoubleSetting.Builder()
        .name("place-height")
        .description("The height to calculate ranges from.")
        .defaultValue(0.5)
        .sliderRange(0, 1)
        .visible(() -> rangePlaceRangeMode.get().equals(PlaceRangeMode.Height))
        .build()
    );

    //  Attack Ranges
    public final Setting<Double> rangeAttackRange = rangeSgAttack.add(new DoubleSetting.Builder()
        .name("attack-range")
        .description("Range for attacking entities.")
        .defaultValue(4.8)
        .range(0, 6)
        .sliderRange(0, 6)
        .build()
    );
    public final Setting<Double> rangeAttackRangeWalls = rangeSgAttack.add(new DoubleSetting.Builder()
        .name("attack-range-walls")
        .description("Range for attacking entities behind blocks.")
        .defaultValue(4.8)
        .range(0, 6)
        .sliderRange(0, 6)
        .build()
    );
    public final Setting<Boolean> rangeReduce = rangeSgAttack.add(new BoolSetting.Builder()
        .name("reduce")
        .description("Reduces range on every hit by reduce step until it reaches (range - reduce amount).")
        .defaultValue(false)
        .build()
    );
    public final Setting<Double> rangeReduceAmount = rangeSgAttack.add(new DoubleSetting.Builder()
        .name("reduce-amount")
        .description("Check description from 'Reduce' setting.")
        .defaultValue(0.8)
        .range(0, 6)
        .sliderRange(0, 6)
        .visible(rangeReduce::get)
        .build()
    );
    public final Setting<Double> rangeReduceStep = rangeSgAttack.add(new DoubleSetting.Builder()
        .name("reduce-step")
        .description("Check description from 'Reduce' setting.")
        .defaultValue(0.14)
        .range(0, 6)
        .sliderRange(0, 6)
        .visible(rangeReduce::get)
        .build()
    );
    private final Setting<FromMode> rangeAttackRangeFrom = rangeSgAttack.add(new EnumSetting.Builder<FromMode>()
        .name("attack-range-from")
        .description("Where to calculate ranges from.")
        .defaultValue(FromMode.Eyes)
        .build()
    );
    private final Setting<AttackRangeMode> rangeAttackRangeMode = rangeSgAttack.add(new EnumSetting.Builder<AttackRangeMode>()
        .name("attack-range-mode")
        .description("Where to calculate ranges from.")
        .defaultValue(AttackRangeMode.NCP)
        .build()
    );
    private final Setting<Double> rangeClosestAttackWidth = rangeSgAttack.add(new DoubleSetting.Builder()
        .name("closest-attack-width")
        .description("How wide should the box be for closest range.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 3)
        .visible(() -> rangeAttackRangeMode.get().equals(AttackRangeMode.CustomBox))
        .build()
    );
    private final Setting<Double> rangeClosestAttackHeight = rangeSgAttack.add(new DoubleSetting.Builder()
        .name("closest-attack-height")
        .description("How tall should the box be for closest range.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 3)
        .visible(() -> rangeAttackRangeMode.get().equals(AttackRangeMode.CustomBox))
        .build()
    );
    private final Setting<Double> rangeAttackHeight = rangeSgAttack.add(new DoubleSetting.Builder()
        .name("attack-height")
        .description("The height above feet to calculate ranges from.")
        .defaultValue(1)
        .sliderRange(-2, 2)
        .visible(() -> rangeAttackRangeMode.get().equals(AttackRangeMode.Height))
        .build()
    );

    //  Mining Ranges
    public final Setting<Double> rangeMiningRange = rangeSgMining.add(new DoubleSetting.Builder()
        .name("mining-range")
        .description("Range for mining blocks.")
        .defaultValue(4.8)
        .range(0, 6)
        .sliderRange(0, 6)
        .build()
    );
    public final Setting<Double> rangeMiningRangeWalls = rangeSgMining.add(new DoubleSetting.Builder()
        .name("mining-range-walls")
        .description("Range for mining blocks behind other blocks.")
        .defaultValue(4.8)
        .range(0, 6)
        .sliderRange(0, 6)
        .build()
    );
    private final Setting<FromMode> rangeMiningRangeFrom = rangeSgMining.add(new EnumSetting.Builder<FromMode>()
        .name("mining-range-from")
        .description("Where to calculate mining ranges from.")
        .defaultValue(FromMode.Eyes)
        .build()
    );
    private final Setting<MiningRangeMode> rangeMiningRangeMode = rangeSgMining.add(new EnumSetting.Builder<MiningRangeMode>()
        .name("mining-range-mode")
        .description("Where to calculate mining ranges from.")
        .defaultValue(MiningRangeMode.NCP)
        .build()
    );
    private final Setting<Double> rangeClosestMiningWidth = rangeSgMining.add(new DoubleSetting.Builder()
        .name("closest-mining-width")
        .description("How wide should the box be for closest range.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 3)
        .visible(() -> rangeMiningRangeMode.get().equals(MiningRangeMode.CustomBox))
        .build()
    );
    private final Setting<Double> rangeClosestMiningHeight = rangeSgMining.add(new DoubleSetting.Builder()
        .name("closest-mining-height")
        .description("How tall should the box be for closest range.")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 3)
        .visible(() -> rangeMiningRangeMode.get().equals(MiningRangeMode.CustomBox))
        .build()
    );
    private final Setting<Double> rangeMiningHeight = rangeSgMining.add(new DoubleSetting.Builder()
        .name("mining-height")
        .description("The height above block bottom to calculate ranges from.")
        .defaultValue(0.5)
        .sliderRange(0, 1)
        .visible(() -> rangeMiningRangeMode.get().equals(MiningRangeMode.Height))
        .build()
    );

    public double rangeRangeMulti = 0;

    // Place Range Checks
    public boolean rangeInPlaceRange(BlockPos pos, Vec3 from) {
        if (mc.player == null) {
            return false;
        }

        double dist = rangePlaceRangeTo(pos, from);
        return dist >= 0 && dist <= (SettingUtils.placeTrace(pos) ? rangePlaceRange.get() : rangePlaceRangeWalls.get());
    }

    public boolean rangeInPlaceRangeNoTrace(BlockPos pos, Vec3 from) {
        if (mc.player == null) {
            return false;
        }

        double dist = rangePlaceRangeTo(pos, from);
        return dist >= 0 && dist <= Math.max(rangePlaceRange.get(), rangePlaceRangeWalls.get());
    }

    public double rangePlaceRangeTo(BlockPos pos, Vec3 from) {
        AABB pBB = mc.player.getBoundingBox();
        if (from == null) {
            from = mc.player.getEyePosition();
            Vec3 pPos = mc.player.position();
            switch (rangePlaceRangeFrom.get()) {
                case Middle ->
                    ((IVec3d) from).meteor$set((pBB.minX + pBB.maxX) / 2, (pBB.minY + pBB.maxY) / 2, (pBB.minZ + pBB.maxZ) / 2);
                case Feet -> ((IVec3d) from).meteor$set(pPos.x, pPos.y, pPos.z);
            }
        }

        Vec3 feet = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        switch (rangePlaceRangeMode.get()) {
            case NCP -> {
                return rangeGetRange(from, feet.add(0, 0.5, 0));
            }
            case Height -> {
                return rangeGetRange(from, feet.add(0, rangePlaceHeight.get(), 0));
            }
            case Vanilla -> {
                return rangeGetRange(from, OLEPOSSUtils.getClosest(mc.player.getEyePosition(), feet, 1, 1));
            }
            case CustomBox -> {
                return rangeGetRange(from, OLEPOSSUtils.getClosest(mc.player.getEyePosition(), feet, rangeBlockWidth.get(), rangeBlockHeight.get()));
            }
        }
        return -1;
    }

    // Attack Range Chesks
    public boolean rangeInAttackRange(AABB bb, Vec3 from) {
        return rangeInAttackRange(bb, rangeGetFeet(bb), from);
    }

    public boolean rangeInAttackRange(AABB bb, Vec3 feet, Vec3 from) {
        if (mc.player == null) {
            return false;
        }

        if (SettingUtils.attackTrace(bb)) {
            return rangeAttackRangeTo(bb, feet, from, true) < rangeAttackRange.get();
        }
        return rangeAttackRangeTo(bb, feet, from, false) < rangeAttackRangeWalls.get();
    }

    public boolean rangeInAttackRangeNoTrace(AABB bb, Vec3 feet, Vec3 from) {
        if (mc.player == null) {
            return false;
        }

        return rangeAttackRangeTo(bb, feet, from, true) <= Math.max(rangeAttackRange.get(), rangeAttackRangeWalls.get());
    }

    public double rangeAttackRangeTo(AABB bb, Vec3 feet, Vec3 from, boolean countReduce) {
        AABB pBB = mc.player.getBoundingBox();
        if (from == null) {
            from = mc.player.getEyePosition();
            switch (rangeAttackRangeFrom.get()) {
                case Middle ->
                    ((IVec3d) from).meteor$set((pBB.minX + pBB.maxX) / 2, (pBB.minY + pBB.maxY) / 2, (pBB.minZ + pBB.maxZ) / 2);
                case Feet -> from = mc.player.position();
            }
        } else {
            switch (rangeAttackRangeFrom.get()) {
                case Middle ->
                    from = from.add(0, mc.player.getEyeHeight(mc.player.getPose()) / 2, 0);
                case Eyes ->
                    from = from.add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
            }
        }

        double dist = switch (rangeAttackRangeMode.get()) {
            case Height -> rangeGetRange(from, feet.add(0, rangeAttackHeight.get(), 0));
            case NCP -> rangeGetRange(from, new Vec3(feet.x, Math.min(Math.max(from.y(), bb.minY), bb.maxY), feet.z));

            case Vanilla ->
                rangeGetRange(from, OLEPOSSUtils.getClosest(mc.player.getEyePosition(), feet, Math.abs(bb.minX - bb.maxX), Math.abs(bb.minY - bb.maxY)));

            case Middle ->
                rangeGetRange(from, new Vec3((bb.minX + bb.maxX) / 2, (bb.minY + bb.maxY) / 2, (bb.minZ + bb.maxZ) / 2));

            case CustomBox ->
                rangeGetRange(from, OLEPOSSUtils.getClosest(mc.player.getEyePosition(), feet, Math.abs(bb.minX - bb.maxX) * rangeClosestAttackWidth.get(), Math.abs(bb.minY - bb.maxY) * rangeClosestAttackHeight.get()));

            case UpdatedNCP ->
                rangeGetRange(from, new Vec3(feet.x, Math.min(Math.max(from.y(), bb.minY), bb.maxY), feet.z)) - rangeGetDistFromCenter(bb, feet, from);
        };

        return dist * (countReduce && rangeReduce.get() ? rangeRangeMulti : 1);
    }

    public double rangeGetDistFromCenter(AABB bb, Vec3 feet, Vec3 from) {
        Vec3 startPos = new Vec3(feet.x, Math.min(Math.max(from.y(), bb.minY), bb.maxY), feet.z);
        Vec3 rangePos = new Vec3(feet.x, Math.min(Math.max(from.y(), bb.minY), bb.maxY), feet.z);

        double halfWidth = Math.abs(bb.minX - bb.maxX) / 2f;

        if (from.x == rangePos.x && from.z == rangePos.z) {
            return 0;
        }

        Vec3 dist = new Vec3(from.x - rangePos.x, 0, from.z - rangePos.z);

        if (rangeGetDistXZ(dist) < halfWidth * Math.sqrt(2)) {
            return 0;
        }

        if (dist.z() > 0.0) {
            ((IVec3d) rangePos).meteor$setXZ(rangePos.x, rangePos.z + halfWidth);
        } else if (dist.z() < 0.0) {
            ((IVec3d) rangePos).meteor$setXZ(rangePos.x, rangePos.z - halfWidth);
        } else if (dist.x() > 0.0) {
            ((IVec3d) rangePos).meteor$setXZ(rangePos.x + halfWidth, rangePos.z);
        } else ((IVec3d) rangePos).meteor$setXZ(rangePos.x - halfWidth, rangePos.z);


        Vec3 vec2 = rangePos.subtract(startPos);
        double angle = RotationUtils.radAngle(new Vec2((float) dist.x, (float) dist.z), new Vec2((float) vec2.x, (float) vec2.z));

        if (angle > Math.PI / 4) {
            angle = Math.PI / 2 - angle;
        }

        if (angle >= 0.0 && angle <= Math.PI / 4) {
            return halfWidth / Math.cos(angle);
        } else {
            return 0;
        }
    }

    private double rangeGetRange(Vec3 from, Vec3 to) {
        double x = Math.abs(from.x - to.x);
        double y = Math.abs(from.y - to.y);
        double z = Math.abs(from.z - to.z);

        return Math.sqrt(x * x + y * y + z * z);
    }

    private Vec3 rangeGetFeet(AABB bb) {
        return new Vec3((bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
    }

    // Mining Range Checks
    public boolean rangeInMineRange(BlockPos pos) {
        if (mc.player == null) {
            return false;
        }

        double dist = rangeMiningRangeTo(pos, null);
        return dist >= 0 && dist <= (SettingUtils.placeTrace(pos) ? rangeMiningRange.get() : rangeMiningRangeWalls.get());
    }

    public boolean rangeInMineRangeNoTrace(BlockPos pos) {
        if (mc.player == null) {
            return false;
        }

        double dist = rangeMiningRangeTo(pos, null);
        return dist >= 0 && dist <= Math.max(rangeMiningRange.get(), rangeMiningRangeWalls.get());
    }

    public double rangeMiningRangeTo(BlockPos pos, Vec3 from) {
        AABB pBB = mc.player.getBoundingBox();
        Vec3 pPos = mc.player.position();
        if (from == null) {
            from = mc.player.getEyePosition();
            switch (rangeMiningRangeFrom.get()) {
                case Middle ->
                    ((IVec3d) from).meteor$set((pBB.minX + pBB.maxX) / 2, (pBB.minY + pBB.maxY) / 2, (pBB.minX + pBB.maxX) / 2);
                case Feet -> ((IVec3d) from).meteor$set(pPos.x, pPos.y, pPos.z);
            }
        }

        Vec3 feet = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        switch (rangeMiningRangeMode.get()) {
            case NCP -> {
                return rangeGetRange(from, feet.add(0, 0.5, 0));
            }
            case Height -> {
                return rangeGetRange(from, feet.add(0, rangeMiningHeight.get(), 0));
            }
            case Vanilla -> {
                return rangeGetRange(from, OLEPOSSUtils.getClosest(mc.player.getEyePosition(), feet, 1, 1));
            }
            case CustomBox -> {
                return rangeGetRange(from, OLEPOSSUtils.getClosest(mc.player.getEyePosition(), feet, rangeClosestMiningWidth.get(), rangeClosestMiningHeight.get()));
            }
        }
        return -1;
    }

    private double rangeGetDistXZ(Vec3 vec) {
        return Math.sqrt(vec.x * vec.x + vec.z * vec.z);
    }

    public void rangeRegisterAttack(AABB bb) {
        if (rangeAttackRangeTo(bb, rangeGetFeet(bb), null, false) <= rangeAttackRange.get() - rangeReduceAmount.get()) {
            rangeRangeMulti = Math.min(rangeRangeMulti + rangeReduceStep.get(), 1);
        } else {
            rangeRangeMulti = Math.max(rangeRangeMulti - rangeReduceStep.get(), (rangeAttackRange.get() - rangeReduceStep.get() / rangeAttackRange.get()) / rangeAttackRange.get());
        }
    }

    public enum PlaceRangeMode {
        NCP,
        Height,
        Vanilla,
        CustomBox
    }

    public enum AttackRangeMode {
        NCP,
        UpdatedNCP,
        Height,
        Vanilla,
        Middle,
        CustomBox
    }

    public enum MiningRangeMode {
        NCP,
        Height,
        Vanilla,
        CustomBox
    }

    public enum FromMode {
        Eyes,
        Middle,
        Feet
    }

    // Raytrace
    private final SettingGroup raytraceSgPlace = raytraceSettings.createGroup("Placing");
    private final SettingGroup raytraceSgAttack = raytraceSettings.createGroup("Attacking");

    //--------------------Place-Settings--------------------//
    public final Setting<Boolean> raytracePlaceTrace = raytraceSgPlace.add(new BoolSetting.Builder()
        .name("place-traces")
        .description("Raytraces when placing.")
        .defaultValue(false)
        .build()
    );
    private final Setting<PlaceTraceMode> raytracePlaceMode = raytraceSgPlace.add(new EnumSetting.Builder<PlaceTraceMode>()
        .name("place-mode")
        .description("Place trace mode.")
        .defaultValue(PlaceTraceMode.SinglePoint)
        .visible(raytracePlaceTrace::get)
        .build()
    );
    private final Setting<Double> raytracePlaceHeight = raytraceSgPlace.add(new DoubleSetting.Builder()
        .name("place-height")
        .description("Raytraces to x blocks above the bottom.")
        .defaultValue(0.5)
        .sliderRange(-2, 2)
        .visible(() -> raytracePlaceMode.get() == PlaceTraceMode.SinglePoint && raytracePlaceTrace.get())
        .build()
    );
    private final Setting<Double> raytracePlaceHeight1 = raytraceSgPlace.add(new DoubleSetting.Builder()
        .name("place-height-1")
        .description("Raytraces to x blocks above the bottom.")
        .defaultValue(0.25)
        .sliderRange(-2, 1.5)
        .visible(() -> raytracePlaceMode.get() == PlaceTraceMode.DoublePoint && raytracePlaceTrace.get())
        .build()
    );
    private final Setting<Double> raytracePlaceHeight2 = raytraceSgPlace.add(new DoubleSetting.Builder()
        .name("place-height-2")
        .description("Raytraces to x blocks above the bottom.")
        .defaultValue(0.75)
        .sliderRange(-2, 2)
        .visible(() -> raytracePlaceMode.get() == PlaceTraceMode.DoublePoint && raytracePlaceTrace.get())
        .build()
    );
    private final Setting<Double> raytraceExposure = raytraceSgPlace.add(new DoubleSetting.Builder()
        .name("place-exposure")
        .description("How many % of the block should be seen.")
        .defaultValue(50)
        .range(0, 100)
        .sliderRange(0, 100)
        .visible(() -> raytracePlaceMode.get() == PlaceTraceMode.Exposure && raytracePlaceTrace.get())
        .build()
    );

    //--------------------Place-Settings--------------------//
    public final Setting<Boolean> raytraceAttackTrace = raytraceSgAttack.add(new BoolSetting.Builder()
        .name("attack-traces")
        .description("Raytraces when attacking.")
        .defaultValue(false)
        .build()
    );
    private final Setting<AttackTraceMode> raytraceAttackMode = raytraceSgAttack.add(new EnumSetting.Builder<AttackTraceMode>()
        .name("attack-mode")
        .description("Attack trace mode.")
        .defaultValue(AttackTraceMode.SinglePoint)
        .visible(raytraceAttackTrace::get)
        .build()
    );
    private final Setting<Double> raytraceAttackHeight = raytraceSgAttack.add(new DoubleSetting.Builder()
        .name("attack-height")
        .description("Raytraces to x blocks above the bottom.")
        .defaultValue(1.5)
        .sliderRange(-2, 2)
        .visible(() -> raytraceAttackMode.get().equals(AttackTraceMode.SinglePoint) && raytraceAttackTrace.get())
        .build()
    );
    private final Setting<Double> raytraceAttackHeight1 = raytraceSgAttack.add(new DoubleSetting.Builder()
        .name("attack-height-1")
        .description("Raytraces to x * hitbox height above the bottom.")
        .defaultValue(0.5)
        .sliderRange(-2, 2)
        .visible(() -> raytraceAttackMode.get().equals(AttackTraceMode.DoublePoint) && raytraceAttackTrace.get())
        .build()
    );
    private final Setting<Double> raytraceAttackHeight2 = raytraceSgAttack.add(new DoubleSetting.Builder()
        .name("attack-height-2")
        .description("Raytraces to x * hitbox height above the bottom.")
        .defaultValue(0.5)
        .sliderRange(-2, 2)
        .visible(() -> raytraceAttackMode.get().equals(AttackTraceMode.DoublePoint) && raytraceAttackTrace.get())
        .build()
    );
    private final Setting<Double> raytraceAttackExposure = raytraceSgAttack.add(new DoubleSetting.Builder()
        .name("attack-exposure")
        .description("How many % of the entity should be seen.")
        .defaultValue(50)
        .range(0, 100)
        .sliderRange(0, 100)
        .visible(() -> raytracePlaceMode.get() == PlaceTraceMode.Exposure && raytraceAttackTrace.get())
        .build()
    );

    public enum PlaceTraceMode {
        SinglePoint,
        DoublePoint,
        Sides,
        Exposure,
        Any
    }

    public enum AttackTraceMode {
        SinglePoint,
        DoublePoint,
        Exposure,
        Any
    }

    private final Vec3 raytraceVec = new Vec3(0, 0, 0);
    public ClipContext raytraceRaycastContext;
    public BlockHitResult raytraceResult;
    public int raytraceHit = 0;

    public boolean raytracePlaceTrace(BlockPos pos) {

        if (!raytracePlaceTrace.get()) {
            return true;
        }

        raytraceUpdateContext();

        switch (raytracePlaceMode.get()) {
            case SinglePoint -> {
                ((ClipContextAccessor) raytraceRaycastContext).blackoutAddon$setEnd(new Vec3(pos.getX() + 0.5, pos.getY() + raytracePlaceHeight.get(), pos.getZ() + 0.5));

                raytraceResult = BODamageUtils.raycast(raytraceRaycastContext);
                return raytraceResult.getBlockPos().equals(pos);
            }
            case DoublePoint -> {
                ((ClipContextAccessor) raytraceRaycastContext).blackoutAddon$setEnd(new Vec3(pos.getX() + 0.5, pos.getY() + raytracePlaceHeight1.get(), pos.getZ() + 0.5));

                raytraceResult = BODamageUtils.raycast(raytraceRaycastContext);
                if (raytraceResult.getBlockPos().equals(pos)) {
                    return true;
                }

                ((ClipContextAccessor) raytraceRaycastContext).blackoutAddon$setEnd(new Vec3(pos.getX() + 0.5, pos.getY() + raytracePlaceHeight2.get(), pos.getZ() + 0.5));

                raytraceResult = BODamageUtils.raycast(raytraceRaycastContext);
                return raytraceResult.getBlockPos().equals(pos);
            }
            case Sides -> {
                ((IVec3d) raytraceVec).meteor$set(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                for (Direction dir : Direction.values()) {
                    ((ClipContextAccessor) raytraceRaycastContext).blackoutAddon$setEnd(raytraceVec.add(dir.getStepX() / 2f, dir.getStepY() / 2f, dir.getStepZ() / 2f));

                    raytraceResult = BODamageUtils.raycast(raytraceRaycastContext);
                    if (raytraceResult.getBlockPos().equals(pos)) {
                        return true;
                    }
                }
            }
            case Exposure -> {
                ((IVec3d) raytraceVec).meteor$set(pos.getX(), pos.getY(), pos.getZ());

                raytraceHit = 0;
                for (int x = 0; x <= 2; x += 1) {
                    for (int y = 0; y <= 2; y += 1) {
                        for (int z = 0; z <= 2; z += 1) {
                            ((ClipContextAccessor) raytraceRaycastContext).blackoutAddon$setEnd(raytraceVec.add(0.1 + x * 0.4, 0.1 + y * 0.4, 0.1 + z * 0.4));

                            raytraceResult = BODamageUtils.raycast(raytraceRaycastContext);
                            if (raytraceResult.getBlockPos().equals(pos)) {
                                raytraceHit++;
                                if (raytraceHit >= raytraceExposure.get() / 100 * 27) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            case Any -> {
                ((IVec3d) raytraceVec).meteor$set(pos.getX(), pos.getY(), pos.getZ());

                raytraceHit = 0;
                for (int x = 0; x <= 2; x += 1) {
                    for (int y = 0; y <= 2; y += 1) {
                        for (int z = 0; z <= 2; z += 1) {
                            ((ClipContextAccessor) raytraceRaycastContext).blackoutAddon$setEnd(raytraceVec.add(0.1 + x * 0.4, 0.1 + y * 0.4, 0.1 + z * 0.4));

                            raytraceResult = BODamageUtils.raycast(raytraceRaycastContext);
                            if (raytraceResult.getBlockPos().equals(pos)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean raytraceAttackTrace(AABB box) {
        if (!raytraceAttackTrace.get()) {
            return true;
        }

        raytraceUpdateContext();

        switch (raytraceAttackMode.get()) {
            case SinglePoint -> {
                ((meteordevelopment.meteorclient.mixininterface.IRaycastContext) BODamageUtils.raycastContext).meteor$set(mc.player.getEyePosition(), new Vec3((box.minX + box.maxX) / 2f, box.minY + raytraceAttackHeight.get(), (box.minZ + box.maxZ) / 2f), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);

                return BODamageUtils.raycast(BODamageUtils.raycastContext).getType() != HitResult.Type.BLOCK;
            }
            case DoublePoint -> {
                ((meteordevelopment.meteorclient.mixininterface.IRaycastContext) BODamageUtils.raycastContext).meteor$set(mc.player.getEyePosition(), new Vec3((box.minX + box.maxX) / 2f, box.minY + raytraceAttackHeight1.get(), (box.minZ + box.maxZ) / 2f), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
                if (BODamageUtils.raycast(BODamageUtils.raycastContext).getType() != HitResult.Type.BLOCK) {
                    return true;
                }

                ((meteordevelopment.meteorclient.mixininterface.IRaycastContext) BODamageUtils.raycastContext).meteor$set(mc.player.getEyePosition(), new Vec3((box.minX + box.maxX) / 2f, box.minY + raytraceAttackHeight2.get(), (box.minZ + box.maxZ) / 2f), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player);
                return BODamageUtils.raycast(BODamageUtils.raycastContext).getType() != HitResult.Type.BLOCK;
            }
            case Exposure -> {
                ((IVec3d) raytraceVec).meteor$set(box.minX, box.minY, box.minZ);
                double xw = box.maxX - box.minX;
                double yh = box.maxY - box.minY;
                double zw = box.maxZ - box.minZ;

                raytraceHit = 0;
                for (int x = 0; x <= 2; x += 1) {
                    for (int y = 0; y <= 2; y += 1) {
                        for (int z = 0; z <= 2; z += 1) {
                            ((ClipContextAccessor) raytraceRaycastContext).blackoutAddon$setEnd(raytraceVec.add(Mth.lerp(x / 2f, 0.1, xw - 0.1), Mth.lerp(y / 2f, 0.0, yh - 0.1), Mth.lerp(z / 2f, 0.1, zw - 0.1)));

                            raytraceResult = BODamageUtils.raycast(raytraceRaycastContext);
                            if (raytraceResult.getType() != HitResult.Type.BLOCK) {
                                raytraceHit++;
                                if (raytraceHit >= raytraceAttackExposure.get() / 100 * 27) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            case Any -> {
                ((IVec3d) raytraceVec).meteor$set(box.minX, box.minY, box.minZ);
                double xw = box.maxX - box.minX;
                double yh = box.maxY - box.minY;
                double zw = box.maxZ - box.minZ;

                for (int x = 0; x <= 2; x += 1) {
                    for (int y = 0; y <= 2; y += 1) {
                        for (int z = 0; z <= 2; z += 1) {
                            ((ClipContextAccessor) raytraceRaycastContext).blackoutAddon$setEnd(raytraceVec.add(Mth.lerp(x / 2f, 0.1, xw - 0.1), Mth.lerp(y / 2f, 0.0, yh - 0.1), Mth.lerp(z / 2f, 0.1, zw - 0.1)));

                            raytraceResult = BODamageUtils.raycast(raytraceRaycastContext);
                            if (raytraceResult.getType() != HitResult.Type.BLOCK) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private void raytraceUpdateContext() {
        if (raytraceRaycastContext == null) {
            raytraceRaycastContext = new ClipContext(mc.player.getEyePosition(), null, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, mc.player);
        } else {
            ((ClipContextAccessor) raytraceRaycastContext).blackoutAddon$setStart(mc.player.getEyePosition());
        }
    }

    // Rotation
    private final SettingGroup rotationSgGeneral = rotationSettings.getDefaultGroup();
    private final SettingGroup rotationSgInteract = rotationSettings.createGroup("Interact");
    private final SettingGroup rotationSgBlockPlace = rotationSettings.createGroup("Block Place");
    private final SettingGroup rotationSgMining = rotationSettings.createGroup("Mining");
    private final SettingGroup rotationSgAttack = rotationSettings.createGroup("Attack");
    private final SettingGroup rotationSgUse = rotationSettings.createGroup("Use");

    //--------------------General--------------------//
    public final Setting<Boolean> rotationVanillaRotation = rotationSgGeneral.add(new BoolSetting.Builder()
        .name("vanilla-rotation")
        .description("Turns your head.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Double> rotationYawStep = rotationSgGeneral.add(new DoubleSetting.Builder()
        .name("yaw-step")
        .description("How many yaw degrees should be rotated each packet.")
        .defaultValue(90)
        .range(0, 180)
        .sliderRange(0, 180)
        .build()
    );
    public final Setting<Double> rotationPitchStep = rotationSgGeneral.add(new DoubleSetting.Builder()
        .name("pitch-step")
        .description("How many pitch degrees should be rotated each packet.")
        .defaultValue(45)
        .range(0, 180)
        .sliderRange(0, 180)
        .build()
    );
    public final Setting<Double> rotationYawRandomization = rotationSgGeneral.add(new DoubleSetting.Builder()
        .name("yaw-randomization")
        .description(".")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );
    public final Setting<Double> rotationPitchRandomization = rotationSgGeneral.add(new DoubleSetting.Builder()
        .name("pitch-randomization")
        .description(".")
        .defaultValue(1)
        .min(0)
        .sliderRange(0, 10)
        .build()
    );

    //--------------------Interact--------------------//
    private final Setting<Boolean> rotationInteractRotate = rotationRotateSetting("Interact", "interacting with a block", rotationSgInteract);
    public final Setting<Double> rotationInteractTime = rotationTimeSetting("Interact", rotationSgInteract);
    public final Setting<RotationCheckMode> rotationInteractMode = rotationModeSetting("Interact", rotationSgInteract);
    public final Setting<Double> rotationInteractYawAngle = rotationYawAngleSetting("Interact", rotationSgInteract, () -> rotationInteractMode.get() == RotationCheckMode.Angle);
    public final Setting<Double> rotationInteractPitchAngle = rotationPitchAngleSetting("Interact", rotationSgInteract, () -> rotationInteractMode.get() == RotationCheckMode.Angle);
    public final Setting<Integer> rotationInteractMemory = rotationMemorySetting("Interact", rotationSgInteract);

    //--------------------Block-Place--------------------//
    private final Setting<Boolean> rotationBlockRotate = rotationRotateSetting("Block Place", "placing a block", rotationSgBlockPlace);
    public final Setting<Double> rotationBlockTime = rotationTimeSetting("Block Place", rotationSgBlockPlace);
    public final Setting<RotationCheckMode> rotationBlockMode = rotationModeSetting("Block Place", rotationSgBlockPlace);
    public final Setting<Double> rotationBlockYawAngle = rotationYawAngleSetting("Block Place", rotationSgBlockPlace, () -> rotationBlockMode.get() == RotationCheckMode.Angle);
    public final Setting<Double> rotationBlockPitchAngle = rotationPitchAngleSetting("Block Place", rotationSgBlockPlace, () -> rotationBlockMode.get() == RotationCheckMode.Angle);
    public final Setting<Integer> rotationBlockMemory = rotationMemorySetting("Block Place", rotationSgBlockPlace);

    //--------------------Mining--------------------//
    private final Setting<Boolean> rotationMineRotate = rotationRotateSetting("Mining", "mining a block", rotationSgMining);
    public final Setting<Double> rotationMineTime = rotationTimeSetting("Mining", rotationSgMining);
    public final Setting<RotationCheckMode> rotationMineMode = rotationModeSetting("Mining", rotationSgMining);
    public final Setting<MiningRotMode> rotationMineTiming = rotationSgMining.add(new EnumSetting.Builder<MiningRotMode>().name("mining-rotate-timing").description(".").defaultValue(MiningRotMode.End).build());
    public final Setting<Double> rotationMineYawAngle = rotationYawAngleSetting("Mining", rotationSgMining, () -> rotationMineMode.get() == RotationCheckMode.Angle);
    public final Setting<Double> rotationMinePitchAngle = rotationPitchAngleSetting("Mining", rotationSgMining, () -> rotationMineMode.get() == RotationCheckMode.Angle);
    public final Setting<Integer> rotationMineMemory = rotationMemorySetting("Mining", rotationSgMining);

    //--------------------Attack--------------------//
    private final Setting<Boolean> rotationAttackRotate = rotationRotateSetting("Attack", "attacking an entity", rotationSgAttack);
    public final Setting<Double> rotationAttackTime = rotationTimeSetting("Attack", rotationSgAttack);
    public final Setting<RotationCheckMode> rotationAttackMode = rotationModeSetting("Attack", rotationSgAttack);
    public final Setting<Double> rotationAttackYawAngle = rotationYawAngleSetting("Attack", rotationSgAttack, () -> rotationAttackMode.get() == RotationCheckMode.Angle);
    public final Setting<Double> rotationAttackPitchAngle = rotationPitchAngleSetting("Attack", rotationSgAttack, () -> rotationAttackMode.get() == RotationCheckMode.Angle);
    public final Setting<Integer> rotationAttackMemory = rotationMemorySetting("Attack", rotationSgAttack);

    //--------------------Use--------------------//
    public final Setting<Double> rotationUseTime = rotationTimeSetting("Use", rotationSgUse);

    public enum MiningRotMode {
        Start,
        End,
        Double
    }

    public enum RotationCheckMode {
        Raytrace,
        StrictRaytrace,
        Angle
    }

    private Setting<Boolean> rotationRotateSetting(String type, String verb, SettingGroup sg) {
        return sg.add(new BoolSetting.Builder().name(type.toLowerCase(java.util.Locale.ROOT).replace(' ', '-') + "-rotate").description("Rotates when + " + verb).defaultValue(false).build());
    }

    private Setting<Double> rotationTimeSetting(String type, SettingGroup sg) {
        return sg.add(new DoubleSetting.Builder().name(type.toLowerCase(java.util.Locale.ROOT).replace(' ', '-') + "-rotation-time").description("Keeps the rotation for x seconds after ending.").defaultValue(0.5).min(0).sliderRange(0, 1).build());
    }

    private Setting<RotationCheckMode> rotationModeSetting(String type, SettingGroup sg) {
        return sg.add(new EnumSetting.Builder<RotationCheckMode>().name(type.toLowerCase(java.util.Locale.ROOT).replace(' ', '-') + "-rotation-mode").description(".").defaultValue(RotationCheckMode.Raytrace).build());
    }

    private Setting<Double> rotationYawAngleSetting(String type, SettingGroup sg, IVisible visible) {
        return sg.add(new DoubleSetting.Builder().name(type.toLowerCase(java.util.Locale.ROOT).replace(' ', '-') + "-yaw-angle").description("Accepts rotation if yaw angle to target is under this.").defaultValue(90).range(0, 180).sliderRange(0, 180).visible(visible).build());
    }

    private Setting<Double> rotationPitchAngleSetting(String type, SettingGroup sg, IVisible visible) {
        return sg.add(new DoubleSetting.Builder().name(type.toLowerCase(java.util.Locale.ROOT).replace(' ', '-') + "-pitch-angle").description("Accepts rotation if pitch angle to target is under this.").defaultValue(45).range(0, 180).sliderRange(0, 180).visible(visible).build());
    }

    private Setting<Integer> rotationMemorySetting(String type, SettingGroup sg) {
        return sg.add(new IntSetting.Builder().name(type.toLowerCase(java.util.Locale.ROOT).replace(' ', '-') + "-memory").description("Accepts rotation if looked at it x packets earlier.").defaultValue(1).range(1, 20).sliderRange(1, 20).build());
    }

    public final Vec3 rotationVec = new Vec3(0, 0, 0);

    public boolean rotationRotationCheck(AABB box, RotationType type) {
        List<RotationManager.Rotation> history = RotationManager.history;
        if (box == null) return false;

        switch (rotationMode(type)) {
            case Raytrace -> {
                for (int r = 0; r < rotationMemory(type); r++) {
                    if (history.size() <= r) break;

                    RotationManager.Rotation rot = history.get(r);

                    if (rotationRaytraceCheck(rot.vec(), rot.yaw(), rot.pitch(), box)) return true;
                }
            }
            case StrictRaytrace -> {
                for (int r = 0; r < rotationMemory(type); r++) {
                    if (history.size() <= r) break;

                    RotationManager.Rotation rot = history.get(r);

                    double range = 7;

                    Vec3 end = new Vec3(
                        range * Math.cos(Math.toRadians(rot.yaw() + 90)) * Math.abs(Math.cos(Math.toRadians(rot.pitch()))),
                        range * -Math.sin(Math.toRadians(rot.pitch())),
                        range * Math.sin(Math.toRadians(rot.yaw() + 90)) * Math.abs(Math.cos(Math.toRadians(rot.pitch()))))
                        .add(rot.vec());

                    if (NCPRaytracer.raytrace(rot.vec(), end, box)) return true;
                }
            }
            case Angle -> {
                for (int r = 0; r < rotationMemory(type); r++) {
                    if (history.size() <= r) break;
                    RotationManager.Rotation rot = history.get(r);

                    if (rotationAngleCheck(rot.vec(), rot.yaw(), rot.pitch(), box, type)) return true;
                }
            }
        }

        return false;
    }

    public boolean rotationShouldRotate(RotationType type) {
        return switch (type) {
            case Interact -> rotationInteractRotate.get();
            case BlockPlace -> rotationBlockRotate.get();
            case Attacking -> rotationAttackRotate.get();
            case Mining -> rotationMineRotate.get();
            default -> true;
        };
    }

    public RotationCheckMode rotationMode(RotationType type) {
        return switch (type) {
            case Interact -> rotationInteractMode.get();
            case BlockPlace -> rotationBlockMode.get();
            case Attacking -> rotationAttackMode.get();
            case Mining -> rotationMineMode.get();
            default -> null;
        };
    }

    public double rotationTime(RotationType type) {
        return switch (type) {
            case Interact -> rotationInteractTime.get();
            case BlockPlace -> rotationBlockTime.get();
            case Attacking -> rotationAttackTime.get();
            case Mining -> rotationMineTime.get();
            case Use -> rotationUseTime.get();
            case Other -> 1.0;
        };
    }

    public int rotationMemory(RotationType type) {
        return switch (type) {
            case Interact -> rotationInteractMemory.get();
            case BlockPlace -> rotationBlockMemory.get();
            case Attacking -> rotationAttackMemory.get();
            case Mining -> rotationMineMemory.get();
            default -> 1;
        };
    }

    public double rotationYawStep(RotationType type) {
        return switch (type) {
            case Other, Use -> 42069;
            default -> rotationYawStep.get() + (Math.random() - 0.5) * 2 * rotationYawRandomization.get();
        };
    }

    public double rotationPitchStep(RotationType type) {
        return switch (type) {
            case Other, Use -> 42069;
            default -> rotationPitchStep.get() + (Math.random() - 0.5) * 2 * rotationPitchRandomization.get();
        };
    }

    public double rotationYawAngle(RotationType type) {
        return switch (type) {
            case Interact -> rotationInteractYawAngle.get();
            case BlockPlace -> rotationBlockYawAngle.get();
            case Attacking -> rotationAttackYawAngle.get();
            case Mining -> rotationMineYawAngle.get();
            default -> 0.0;
        };
    }

    public double rotationPitchAngle(RotationType type) {
        return switch (type) {
            case Interact -> rotationInteractPitchAngle.get();
            case BlockPlace -> rotationBlockPitchAngle.get();
            case Attacking -> rotationAttackPitchAngle.get();
            case Mining -> rotationMinePitchAngle.get();
            default -> 0.0;
        };
    }

    public boolean rotationAngleCheck(Vec3 pos, double y, double p, AABB box, RotationType type) {
        return RotationUtils.yawAngle(y, RotationUtils.getYaw(pos, box.getCenter())) <= rotationYawAngle(type) && Math.abs(p - RotationUtils.getPitch(pos, box.getCenter())) <= rotationPitchAngle(type);
    }

    public boolean rotationRaytraceCheck(Vec3 pos, double y, double p, AABB box) {
        double range = pos.distanceTo(OLEPOSSUtils.getMiddle(box)) + 3;

        Vec3 end = new Vec3(range * Math.cos(Math.toRadians(y + 90)) * Math.abs(Math.cos(Math.toRadians(p))),
            range * -Math.sin(Math.toRadians(p)),
            range * Math.sin(Math.toRadians(y + 90)) * Math.abs(Math.cos(Math.toRadians(p)))).add(pos);

        for (float i = 0; i < 1; i += 0.01) {
            if (box.contains(pos.x + (end.x - pos.x) * i, pos.y + (end.y - pos.y) * i, pos.z + (end.z - pos.z) * i)) return true;
        }
        return false;
    }

    private double rotationLerp(double from, double to, double delta) {
        return from + (to - from) * delta;
    }

    public boolean rotationValidForCheck(BlockPos pos, BlockState state) {
        if (state.isSolid()) return true;
        if (state.getBlock() instanceof LiquidBlock) return false;
        if (state.getBlock() instanceof StairBlock) return false;
        if (state.hasBlockEntity()) return false;

        return state.isCollisionShapeFullBlock(mc.level, pos);
    }

    public boolean rotationEndMineRot() {
        if (!rotationMineRotate.get()) return false;
        return rotationMineTiming.get() == MiningRotMode.End || rotationMineTiming.get() == MiningRotMode.Double;
    }

    public boolean rotationStartMineRot() {
        if (!rotationMineRotate.get()) return false;
        return rotationMineTiming.get() == MiningRotMode.Start || rotationMineTiming.get() == MiningRotMode.Double;
    }

    // Server
    private final SettingGroup serverSgGeneral = serverSettings.getDefaultGroup();

    public final Setting<Boolean> serverCc = serverSgGeneral.add(new BoolSetting.Builder()
        .name("cc-hitboxes")
        .description("Newly placed crystals require 1 block tall space without entity hitboxes.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Boolean> serverOldVerCrystals = serverSgGeneral.add(new BoolSetting.Builder()
        .name("1-12-2-crystals")
        .description("Requires 2 block tall space to place crystals.")
        .defaultValue(false)
        .build()
    );
    public final Setting<Boolean> serverOldVerDamage = serverSgGeneral.add(new BoolSetting.Builder()
        .name("1-12-2-damage")
        .description("Calculates damages in old way.")
        .defaultValue(false)
        .build()
    );

    // Swing
    private final SettingGroup swingSgInteract = swingSettings.createGroup("Interact");
    private final SettingGroup swingSgBlockPlace = swingSettings.createGroup("Block Place");
    private final SettingGroup swingSgMining = swingSettings.createGroup("Mining");
    private final SettingGroup swingSgAttack = swingSettings.createGroup("Attack");
    private final SettingGroup swingSgUse = swingSettings.createGroup("Use");

    public final Setting<Boolean> swingInteract = swingSgInteract.add(new BoolSetting.Builder()
        .name("interact-swing")
        .description("Swings your hand when you interact with a block.")
        .defaultValue(true)
        .build()
    );
    public final Setting<SwingState> swingInteractState = swingSgInteract.add(new EnumSetting.Builder<SwingState>()
        .name("interact-state")
        .description("Should we swing our hand before or after the action.")
        .defaultValue(SwingState.Post)
        .visible(swingInteract::get)
        .build()
    );
    public final Setting<Boolean> swingBlockPlace = swingSgBlockPlace.add(new BoolSetting.Builder()
        .name("block-place-swing")
        .description("Swings your hand when you interact with a block.")
        .defaultValue(true)
        .build()
    );
    public final Setting<SwingState> swingBlockPlaceState = swingSgBlockPlace.add(new EnumSetting.Builder<SwingState>()
        .name("block-place-state")
        .description("Should we swing our hand before or after the action.")
        .defaultValue(SwingState.Post)
        .visible(swingBlockPlace::get)
        .build()
    );
    public final Setting<MiningSwingState> swingMining = swingSgMining.add(new EnumSetting.Builder<MiningSwingState>()
        .name("mining-swing")
        .description("Swings your hand when you place a crystal.")
        .defaultValue(MiningSwingState.Double)
        .build()
    );
    public final Setting<Boolean> swingAttack = swingSgAttack.add(new BoolSetting.Builder()
        .name("attack-swing")
        .description("Swings your hand when you attack any entity.")
        .defaultValue(true)
        .build()
    );
    public final Setting<SwingState> swingAttackState = swingSgAttack.add(new EnumSetting.Builder<SwingState>()
        .name("attack-state")
        .description("Should we swing our hand before or after the action.")
        .defaultValue(SwingState.Post)
        .visible(swingAttack::get)
        .build()
    );
    public final Setting<Boolean> swingUse = swingSgUse.add(new BoolSetting.Builder()
        .name("use-swing")
        .description("Swings your hand when using an item. NCP doesn't check this.")
        .defaultValue(true)
        .build()
    );
    public final Setting<SwingState> swingUseState = swingSgUse.add(new EnumSetting.Builder<SwingState>()
        .name("using-state")
        .description("Should we swing our hand before or after the action.")
        .defaultValue(SwingState.Post)
        .visible(swingUse::get)
        .build()
    );

    public void swingSwing(SwingState state, SwingType type, InteractionHand hand) {
        if (mc.player == null) {
            return;
        }
        if (!state.equals(swingGetState(type))) {
            return;
        }

        switch (type) {
            case Interact -> swingSwing(swingInteract.get(), hand);
            case Placing -> swingSwing(swingBlockPlace.get(), hand);
            case Attacking -> swingSwing(swingAttack.get(), hand);
            case Using -> swingSwing(swingUse.get(), hand);
        }
    }

    public void swingMineSwing(MiningSwingState state) {
        switch (state) {
            case Start -> {
                if (swingMining.get() != MiningSwingState.Start) {
                    return;
                }
            }
            case End -> {
                if (swingMining.get() != MiningSwingState.End) {
                    return;
                }
            }
            case Disabled -> {
                return;
            }
        }
        if (mc.player == null) {
            return;
        }

        swingSwing(true, InteractionHand.MAIN_HAND);
    }

    private SwingState swingGetState(SwingType type) {
        return switch (type) {
            case Interact -> swingInteractState.get();
            case Mining -> SwingState.Post;
            case Placing -> swingBlockPlaceState.get();
            case Attacking -> swingAttackState.get();
            case Using -> swingUseState.get();
        };
    }

    private void swingSwing(boolean shouldSwing, InteractionHand hand) {
        if (mc.player == null) {
            return;
        }

        if (shouldSwing) mc.player.connection.send(new ServerboundSwingPacket(hand));
    }

    public enum MiningSwingState {
        Disabled,
        Start,
        End,
        Double
    }
}
