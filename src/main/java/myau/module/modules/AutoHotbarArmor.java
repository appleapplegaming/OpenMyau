package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class AutoHotbarArmor extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean wasUsePressed = false;

    public AutoHotbarArmor() {
        super("AutoHotbarArmor", false);
    }

    private int convertSlotIndex(int slot) {
        if (slot >= 36) {
            return 8 - (slot - 36);
        } else {
            return slot <= 8 ? slot + 36 : slot;
        }
    }

    private void clickSlot(int windowId, int slotId, int mouseButtonClicked, int mode) {
        mc.playerController.windowClick(windowId, slotId, mouseButtonClicked, mode, mc.thePlayer);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() != EventType.PRE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        boolean usePressed = mc.gameSettings.keyBindUseItem.isKeyDown();

        if (usePressed && !wasUsePressed) {
            tryEquipHeldArmor();
        }

        wasUsePressed = usePressed;
    }

    private void tryEquipHeldArmor() {
        int hotbarSlot = mc.thePlayer.inventory.currentItem;
        ItemStack held = mc.thePlayer.inventory.getStackInSlot(hotbarSlot);

        if (held == null) return;
        if (!(held.getItem() instanceof ItemArmor)) return;

        ItemArmor armor = (ItemArmor) held.getItem();

        // 39=boots, 38=leggings, 37=chest, 36=helmet in player inventory indexing style
        int targetArmorSlot = 39 - armor.armorType;

        // only equip if target slot is empty
        if (mc.thePlayer.inventory.getStackInSlot(targetArmorSlot) != null) {
            return;
        }

        // shift-click the held armor piece from hotbar into its armor slot
        clickSlot(
                mc.thePlayer.inventoryContainer.windowId,
                convertSlotIndex(hotbarSlot),
                0,
                1
        );
    }
}