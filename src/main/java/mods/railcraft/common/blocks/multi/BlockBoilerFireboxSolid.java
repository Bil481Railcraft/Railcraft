/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2018
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.blocks.multi;

import mods.railcraft.common.plugins.forge.CraftingPlugin;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Tuple;
import net.minecraft.world.World;

/**
 *
 */
public final class BlockBoilerFireboxSolid extends BlockBoilerFirebox {

    public BlockBoilerFireboxSolid() {
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    public TileMultiBlock createTileEntity(World world, IBlockState state) {
        return new TileBoilerFireboxSolid();
    }

    @Override
    public Tuple<Integer, Integer> getTextureDimensions() {
        return new Tuple<>(3, 1);
    }

    @Override
    public Class<? extends TileEntity> getTileClass(IBlockState state) {
        return TileBoilerFireboxSolid.class;
    }

    @Override
    public void defineRecipes() {
        ItemStack stack = new ItemStack(this);
        CraftingPlugin.addRecipe(stack,
                "BBB",
                "BCB",
                "BFB",
                'B', Items.NETHERBRICK,
                'C', Items.FIRE_CHARGE,
                'F', Blocks.FURNACE
        );
    }
}
