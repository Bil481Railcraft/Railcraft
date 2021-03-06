/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2018
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.blocks.multi;

import mods.railcraft.api.charge.Charge;
import mods.railcraft.api.charge.IBatteryBlock;
import mods.railcraft.common.items.ItemCharge;
import mods.railcraft.common.items.Metal;
import mods.railcraft.common.items.RailcraftItems;
import mods.railcraft.common.plugins.forge.CraftingPlugin;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 *
 */
public final class BlockFluxTransformer extends BlockMultiBlockCharge {

    public static final ChargeSpec CHARGE_DEF = new ChargeSpec(ConnectType.BLOCK, 0.5,
            new IBatteryBlock.Spec(IBatteryBlock.State.DISABLED, 500, 500, 0.65));

    public BlockFluxTransformer() {
        super(Material.IRON);
        setSoundType(SoundType.METAL);
        setTickRandomly(true);
        setHarvestLevel("pickaxe", 1);
    }

    @Override
    public void randomDisplayTick(IBlockState state, World worldIn, BlockPos pos, Random rand) {
        Charge.effects().throwSparks(state, worldIn, pos, rand, 50);
    }

    @Override
    public ChargeSpec getChargeDef(Charge network, IBlockState state, IBlockAccess world, BlockPos pos) {
        switch (network) {
            case distribution:
                return CHARGE_DEF;
            default:
                return null;
        }
    }

    @Override
    public TileMultiBlock createTileEntity(World world, IBlockState state) {
        return new TileFluxTransformer();
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public Class<TileFluxTransformer> getTileClass(IBlockState state) {
        return TileFluxTransformer.class;
    }

    @Override
    public Tuple<Integer, Integer> getTextureDimensions() {
        return new Tuple<>(1, 1);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        super.breakBlock(worldIn, pos, state);
        deregisterNode(worldIn, pos);
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        return Charge.distribution.network(worldIn).access(pos).getComparatorOutput();
    }

    @Override
    public void defineRecipes() {
        ItemStack stack = new ItemStack(this, 2);
        CraftingPlugin.addRecipe(stack,
                "CGC",
                "GRG",
                "CTC",
                'G', RailcraftItems.PLATE, Metal.GOLD,
                'C', RailcraftItems.CHARGE, ItemCharge.EnumCharge.SPOOL_SMALL,
                'T', RailcraftItems.CHARGE, ItemCharge.EnumCharge.TERMINAL,
                'R', "blockRedstone");
    }
}
