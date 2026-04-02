package myau.module.modules;

import myau.module.Module;
import myau.property.properties.BooleanProperty;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class HitDiamond extends Module {


    public HitDiamond() {
        super("HitDiamond", false);
    }

    public boolean hasTooMuchDiamondArmor(EntityLivingBase entity) {
        int diamondPieces = 0;
        for (int i = 1; i <= 4; i++) {
            ItemStack armorSlot = entity.getEquipmentInSlot(i);
            if (armorSlot != null && armorSlot.getItem() instanceof ItemArmor) {
                ItemArmor armor = (ItemArmor) armorSlot.getItem();
                if (armor.getArmorMaterial() == ItemArmor.ArmorMaterial.DIAMOND) {
                    diamondPieces++;
                }
            }
        }
        return diamondPieces > 1;
    }
}
