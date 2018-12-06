/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.carts;

import java.util.ArrayList;

import mods.railcraft.common.core.RailcraftConfig;
import mods.railcraft.common.core.RailcraftConstants;
import mods.railcraft.common.plugins.forge.CraftingPlugin;
import mods.railcraft.common.plugins.forge.OreDictPlugin;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ItemBoreHeadDiamond extends ItemBoreHead {

    public static final ResourceLocation TEXTURE = new ResourceLocation(RailcraftConstants.CART_TEXTURE_FOLDER + "tunnel_bore_diamond.png");

    public ItemBoreHeadDiamond() {
        setMaxDamage(6000);
        RailcraftConfig.numsForDiamond = new ArrayList<>();
        while(RailcraftConfig.numsForDiamond.size() != RailcraftConfig.diamondLuck) {
            int random = (int)(Math.random() * 1000 + 1);
            if(!RailcraftConfig.numsForDiamond.contains(random))
                RailcraftConfig.numsForDiamond.add(random);
        }
    }

    @Override
    public void defineRecipes() {
        CraftingPlugin.addRecipe(new ItemStack(this),
                "III",
                "IDI",
                "III",
                'I', "ingotSteel",
                'D', "blockDiamond");
    }

    @Override
    public ResourceLocation getBoreTexture() {
        return TEXTURE;
    }

    @Override
    public int getHarvestLevel() {
        return 3;
    }

    @Override
    public float getDigModifier() {
        return 1.4f;
    }

    @Override
    public int getItemEnchantability(ItemStack stack) {
        return ToolMaterial.DIAMOND.getEnchantability();
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return OreDictPlugin.isOreType("blockDiamond", repair);
    }
}
