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
import mods.railcraft.common.util.crafting.RockCrusherCraftingManager;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 *
 */
public final class BlockBlastFurnace extends BlockMultiBlockInventory {

    public static final PropertyInteger ICON = PropertyInteger.create("icon", 0, 2);

    public BlockBlastFurnace() {
        super(Material.ROCK);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, ICON);
    }

    @Override
    public TileMultiBlockInventory createTileEntity(World world, IBlockState state) {
        return new TileBlastFurnace();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public Class<TileBlastFurnace> getTileClass(IBlockState state) {
        return TileBlastFurnace.class;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Tuple<Integer, Integer> getTextureDimensions() {
        return new Tuple<>(3, 1);
    }

    @Override
    public void defineRecipes() {
        ItemStack stack = new ItemStack(this, 4);
        CraftingPlugin.addRecipe(stack,
                "MBM",
                "BPB",
                "MBM",
                'B', new ItemStack(Blocks.NETHER_BRICK),
                'M', new ItemStack(Blocks.SOUL_SAND),
                'P', Items.MAGMA_CREAM);
        RockCrusherCraftingManager.getInstance().createRecipeBuilder()
                .input(CraftingPlugin.getIngredient(this))
                .addOutput(new ItemStack(Blocks.NETHER_BRICK), 0.75f)
                .addOutput(new ItemStack(Blocks.SOUL_SAND), 0.75f)
                .addOutput(new ItemStack(Items.BLAZE_POWDER), 0.05f)
                .buildAndRegister();
    }
}
