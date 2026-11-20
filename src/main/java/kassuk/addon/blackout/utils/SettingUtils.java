package kassuk.addon.blackout.utils;

import kassuk.addon.blackout.enums.RotationType;
import kassuk.addon.blackout.enums.SwingState;
import kassuk.addon.blackout.enums.SwingType;
import kassuk.addon.blackout.utils.meteor.GlobalSettingsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.function.Predicate;

/**
 * @author OLEPOSSU
 */

public class SettingUtils {
    private static final GlobalSettingsConfig config = GlobalSettingsConfig.get();

    //  Range
    public static void registerAttack(AABB bb) {config.rangeRegisterAttack(bb);}
    public static double getPlaceRange() {
        return config.rangePlaceRange.get();
    }
    public static double getPlaceWallsRange() {
        return config.rangePlaceRangeWalls.get();
    }
    public static double getAttackRange() {
        return config.rangeAttackRange.get();
    }
    public static double getAttackWallsRange() {
        return config.rangeAttackRangeWalls.get();
    }
    public static double getMineRange() {
        return config.rangeMiningRange.get();
    }
    public static double getMineWallsRange() {
        return config.rangeMiningRangeWalls.get();
    }
    public static double placeRangeTo(BlockPos pos) {return config.rangePlaceRangeTo(pos, null);}
    public static boolean inPlaceRange(BlockPos pos) {return config.rangeInPlaceRange(pos, null);}
    public static boolean inPlaceRange(BlockPos pos, Vec3 from) {
        return config.rangeInPlaceRange(pos, from);
    }
    public static boolean inPlaceRangeNoTrace(BlockPos pos) {
        return config.rangeInPlaceRangeNoTrace(pos, null);
    }
    public static boolean inPlaceRangeNoTrace(BlockPos pos, Vec3 from) {
        return config.rangeInPlaceRangeNoTrace(pos, from);
    }
    public static boolean inAttackRange(AABB bb) {
        return config.rangeInAttackRange(bb, null);
    }
    public static boolean inAttackRange(AABB bb, Vec3 from) {
        return config.rangeInAttackRange(bb, from);
    }
    public static double mineRangeTo(BlockPos pos) {return config.rangeMiningRangeTo(pos, null);}
    public static boolean inMineRange(BlockPos pos) {
        return config.rangeInMineRange(pos);
    }
    public static boolean inMineRangeNoTrace(BlockPos pos) {
        return config.rangeInMineRangeNoTrace(pos);
    }
    public static boolean inAttackRangeNoTrace(AABB bb, double eyeHeight, Vec3 feet) {return config.rangeInAttackRangeNoTrace(bb, feet, null);}
    public static boolean inAttackRangeNoTrace(AABB bb, double eyeHeight, Vec3 feet, Vec3 from) {return config.rangeInAttackRangeNoTrace(bb, feet, from);}
    public static double attackRangeTo(AABB bb, Vec3 feet) {return config.rangeAttackRangeTo(bb, feet, null, true);}


    //  Rotate
    public static boolean startMineRot() {
        return config.rotationStartMineRot();
    }
    public static boolean endMineRot() {
        return config.rotationEndMineRot();
    }
    public static boolean shouldVanillaRotate() {return config.rotationVanillaRotation.get();}
    public static boolean shouldRotate(RotationType type) {
        return config.rotationShouldRotate(type);
    }
    public static boolean rotationCheck(AABB box, RotationType type) {return config.rotationRotationCheck(box, type);}

    //  Swing
    public static void swing(SwingState state, SwingType type, InteractionHand hand) {
        config.swingSwing(state, type, hand);
    }
    public static void mineSwing(GlobalSettingsConfig.MiningSwingState state) {
        config.swingMineSwing(state);
    }

    //  Facing
    public static PlaceData getPlaceData(BlockPos pos) {return config.facingGetPlaceData(pos, true);}
    public static PlaceData getPlaceDataANDDir(BlockPos pos, Predicate<Direction> predicate) {return config.facingGetPlaceDataAND(pos, predicate, null, true);}
    public static PlaceData getPlaceDataANDPos(BlockPos pos, Predicate<BlockPos> predicate) {return config.facingGetPlaceDataAND(pos, null, predicate,true);}
    public static PlaceData getPlaceDataAND(BlockPos pos, Predicate<Direction> predicateDir, Predicate<BlockPos> predicate) {return config.facingGetPlaceDataAND(pos, predicateDir, predicate,true);}
    public static PlaceData getPlaceDataOR(BlockPos pos, Predicate<BlockPos> predicate) {return config.facingGetPlaceDataOR(pos, predicate, true);}
    public static PlaceData getPlaceData(BlockPos pos, boolean ignoreContainers) {return config.facingGetPlaceData(pos, ignoreContainers);}
    public static PlaceData getPlaceDataANDDir(BlockPos pos, Predicate<Direction> predicate, boolean ignoreContainers) {return config.facingGetPlaceDataAND(pos, predicate, null, ignoreContainers);}
    public static PlaceData getPlaceDataANDPos(BlockPos pos, Predicate<BlockPos> predicate, boolean ignoreContainers) {return config.facingGetPlaceDataAND(pos, null, predicate, ignoreContainers);}
    public static PlaceData getPlaceDataAND(BlockPos pos, Predicate<Direction> predicateDir, Predicate<BlockPos> predicate, boolean ignoreContainers) {return config.facingGetPlaceDataAND(pos, predicateDir, predicate, ignoreContainers);}
    public static PlaceData getPlaceDataOR(BlockPos pos, Predicate<BlockPos> predicate, boolean ignoreContainers) {return config.facingGetPlaceDataOR(pos, predicate, ignoreContainers);}
    public static Direction getPlaceOnDirection(BlockPos pos) {
        return config.facingGetPlaceOnDirection(pos);
    }

    //  Raytracing
    public static boolean shouldPlaceTrace() {
        return config.raytracePlaceTrace.get();
    }
    public static boolean shouldAttackTrace() {return config.raytraceAttackTrace.get();}
    public static boolean placeTrace(BlockPos pos) {
        return !shouldPlaceTrace() || config.raytracePlaceTrace(pos);
    }
    public static boolean attackTrace(AABB bb) {
        return !shouldAttackTrace() || config.raytraceAttackTrace(bb);
    }

    // Server
    public static boolean oldDamage() {
        return config.serverOldVerDamage.get();
    }
    public static boolean oldCrystals() {
        return config.serverOldVerCrystals.get();
    }
    public static boolean cc() {
        return config.serverCc.get();
    }
}
