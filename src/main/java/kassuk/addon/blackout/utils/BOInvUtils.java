package kassuk.addon.blackout.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import kassuk.addon.blackout.managers.Managers;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import meteordevelopment.meteorclient.utils.player.SlotUtils;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

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
            if (mc.player == null || mc.interactionManager == null) return false;
            pickSlot = SlotUtils.indexToId(slot);
            if (pickSlot < 0) return false;
            pickHotbarSlot = mc.player.getInventory().selectedSlot;
            pickSyncId = mc.player.currentScreenHandler.syncId;
            mc.interactionManager.clickSlot(pickSyncId, pickSlot, pickHotbarSlot, SlotActionType.SWAP, mc.player);

            return true;
        }
        return false;
    }
    public static void pickSwapBack() {
        if (pickSlot >= 0) {
            if (mc.player != null && mc.interactionManager != null && mc.player.currentScreenHandler.syncId == pickSyncId) {
                mc.interactionManager.clickSlot(pickSyncId, pickSlot, pickHotbarSlot, SlotActionType.SWAP, mc.player);
            }
            pickSlot = -1;
        }
    }

    // Credits to rickyracuun
    public static boolean invSwitch(int slot) {
        if (slot >= 0) {
            ScreenHandler handler = mc.player.currentScreenHandler;
            Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
            stack.put(slot, handler.getSlot(slot).getStack());

            mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId,
                handler.getRevision(), PlayerInventory.MAIN_SIZE + Managers.HOLDING.slot,
                slot, SlotActionType.SWAP, handler.getSlot(slot).getStack(), stack)
            );
            ((IClientPlayerInteractionManager) mc.interactionManager).meteor$syncSelected();
            slots = new int[]{slot, Managers.HOLDING.slot};
            return true;
        }
        return false;
    }

    public static void swapBack() {
        ScreenHandler handler = mc.player.currentScreenHandler;
        Int2ObjectArrayMap<ItemStack> stack = new Int2ObjectArrayMap<>();
        stack.put(slots[0], handler.getSlot(slots[0]).getStack());

        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId,
            handler.getRevision(), PlayerInventory.MAIN_SIZE + slots[1],
            slots[0], SlotActionType.SWAP, handler.getSlot(slots[0]).getStack().copy(), stack)
        );
        ((IClientPlayerInteractionManager) mc.interactionManager).meteor$syncSelected();
    }
}
