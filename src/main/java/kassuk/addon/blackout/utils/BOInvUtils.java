package kassuk.addon.blackout.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import kassuk.addon.blackout.managers.Managers;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * @author OLEPOSSU
 */

@SuppressWarnings("DataFlowIssue")
public class BOInvUtils {
    private static int[] slots;
    public static int pickSlot = -1;
    private static int pickHotbarSlot;
    private static int pickSyncId;

    public static boolean pickSwitch(int slot) {
        if (slot >= 0) {
            Managers.HOLDING.modifyStartTime = System.currentTimeMillis();
            if (mc.player == null || mc.gameMode == null) return false;
            pickSlot = SlotUtils.indexToId(slot);
            if (pickSlot < 0) return false;
            pickHotbarSlot = mc.player.getInventory().getSelectedSlot();
            pickSyncId = mc.player.containerMenu.containerId;
            mc.gameMode.handleInventoryMouseClick(pickSyncId, pickSlot, pickHotbarSlot, ClickType.SWAP, mc.player);

            return true;
        }
        return false;
    }
    public static void pickSwapBack() {
        if (pickSlot >= 0) {
            if (mc.player != null && mc.gameMode != null && mc.player.containerMenu.containerId == pickSyncId) {
                mc.gameMode.handleInventoryMouseClick(pickSyncId, pickSlot, pickHotbarSlot, ClickType.SWAP, mc.player);
            }
            pickSlot = -1;
        }
    }

    // Credits to rickyracuun
    public static boolean invSwitch(int slot) {
        if (slot >= 0) {
            AbstractContainerMenu handler = mc.player.containerMenu;
            Int2ObjectArrayMap<HashedStack> stack = new Int2ObjectArrayMap<>();
            stack.put(slot, hash(handler.getSlot(slot).getItem()));

            mc.getConnection().send(new ServerboundContainerClickPacket(handler.containerId,
                handler.getStateId(), (short) (Inventory.INVENTORY_SIZE + Managers.HOLDING.slot),
                (byte) slot, ClickType.SWAP, stack, hash(handler.getSlot(slot).getItem()))
            );
            ((IClientPlayerInteractionManager) mc.gameMode).meteor$syncSelected();
            slots = new int[]{slot, Managers.HOLDING.slot};
            return true;
        }
        return false;
    }

    public static void swapBack() {
        AbstractContainerMenu handler = mc.player.containerMenu;
        Int2ObjectArrayMap<HashedStack> stack = new Int2ObjectArrayMap<>();
        stack.put(slots[0], hash(handler.getSlot(slots[0]).getItem()));

        mc.getConnection().send(new ServerboundContainerClickPacket(handler.containerId,
            handler.getStateId(), (short) (Inventory.INVENTORY_SIZE + slots[1]),
            (byte) slots[0], ClickType.SWAP, stack, hash(handler.getSlot(slots[0]).getItem()))
        );
        ((IClientPlayerInteractionManager) mc.gameMode).meteor$syncSelected();
    }
    private static HashedStack hash(ItemStack stack) {
        return HashedStack.create(stack, mc.getConnection().decoratedHashOpsGenenerator());
    }
}
