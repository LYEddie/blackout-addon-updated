package kassuk.addon.blackout.utils;

import kassuk.addon.blackout.enums.RotationType;
import kassuk.addon.blackout.enums.SwingState;
import kassuk.addon.blackout.enums.SwingType;
import kassuk.addon.blackout.utils.meteor.GlobalSettingsConfig;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

/**
 * @author OLEPOSSU
 */

public class SettingUtils {
    private static final GlobalSettingsConfig config = GlobalSettingsConfig.get();

    //  Range
    public static void registerAttack(Box bb) {config.rangeRegisterAttack(bb);}
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
    public static boolean inPlaceRange(BlockPos pos, Vec3d from) {
        return config.rangeInPlaceRange(pos, from);
    }
    public static boolean inPlaceRangeNoTrace(BlockPos pos) {
        return config.rangeInPlaceRangeNoTrace(pos, null);
    }
    public static boolean inPlaceRangeNoTrace(BlockPos pos, Vec3d from) {
        return config.rangeInPlaceRangeNoTrace(pos, from);
    }
    public static boolean inAttackRange(Box bb) {
        return config.rangeInAttackRange(bb, null);
    }
    public static boolean inAttackRange(Box bb, Vec3d from) {
        return config.rangeInAttackRange(bb, from);
    }
    public static double mineRangeTo(BlockPos pos) {return config.rangeMiningRangeTo(pos, null);}
    public static boolean inMineRange(BlockPos pos) {
        return config.rangeInMineRange(pos);
    }
    public static boolean inMineRangeNoTrace(BlockPos pos) {
        return config.rangeInMineRangeNoTrace(pos);
    }
    public static boolean inAttackRangeNoTrace(Box bb, double eyeHeight, Vec3d feet) {return config.rangeInAttackRangeNoTrace(bb, feet, null);}
    public static boolean inAttackRangeNoTrace(Box bb, double eyeHeight, Vec3d feet, Vec3d from) {return config.rangeInAttackRangeNoTrace(bb, feet, from);}
    public static double attackRangeTo(Box bb, Vec3d feet) {return config.rangeAttackRangeTo(bb, feet, null, true);}


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
    public static boolean rotationCheck(Box box, RotationType type) {return config.rotationRotationCheck(box, type);}

    //  Swing
    public static void swing(SwingState state, SwingType type, Hand hand) {
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
    public static boolean attackTrace(Box bb) {
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
