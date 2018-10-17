/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2018
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.blocks.single;

import mods.railcraft.common.blocks.BlockEntityDelegate;
import mods.railcraft.common.blocks.charge.IChargeBlock;
import mods.railcraft.common.items.ItemCharge;
import mods.railcraft.common.items.ItemDust;
import mods.railcraft.common.items.RailcraftItems;
import mods.railcraft.common.plugins.color.ColorPlugin;
import mods.railcraft.common.plugins.color.EnumColor;
import mods.railcraft.common.plugins.forge.CraftingPlugin;
import mods.railcraft.common.plugins.forge.WorldPlugin;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BlockForceTrackEmitter extends BlockEntityDelegate implements IChargeBlock, ColorPlugin.IColoredBlock {

    public static final int DEFAULT_SHADE = EnumColor.LIGHT_BLUE.getHexColor();
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final ChargeDef CHARGE_DEF = new ChargeDef(ConnectType.BLOCK, 0.025);

    public BlockForceTrackEmitter() {
        super(Material.IRON);
        setSoundType(SoundType.METAL);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, POWERED, FACING);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    @Override
    public Class<? extends TileEntity> getTileClass(IBlockState state) {
        return TileForceTrackEmitter.class;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileForceTrackEmitter();
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public @Nullable ChargeDef getChargeDef(IBlockState state, IBlockAccess world, BlockPos pos) {
        return CHARGE_DEF;
    }

    @Override
    public boolean recolorBlock(World world, BlockPos pos, EnumFacing side, EnumDyeColor color) {
        TileEntity tile = WorldPlugin.getBlockTile(world, pos);
        if (tile instanceof TileForceTrackEmitter) {
            TileForceTrackEmitter emitter = (TileForceTrackEmitter) tile;
            int rcColor = EnumColor.fromDye(color).getHexColor();
            if (rcColor != emitter.getColor()) {
                emitter.setColor(rcColor);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public IBlockColor colorHandler() {
        return (state, worldIn, pos, tintIndex) -> {
            if (tintIndex != 1) {
                return ColorPlugin.NONE_MULTIPLIER;
            }
            if (worldIn != null && pos != null) {
                return WorldPlugin.getTileEntity(worldIn, pos, TileForceTrackEmitter.class).map(TileForceTrackEmitter::getColor)
                        .orElse(DEFAULT_SHADE);
            }
            return DEFAULT_SHADE;
        };
    }

    @Override
    public void defineRecipes() {
        CraftingPlugin.addRecipe(new ItemStack(this),
                "PIP",
                "RBR",
                "PIP",
                'P', "plateTin",
                'R', RailcraftItems.CHARGE, ItemCharge.EnumCharge.COIL,
                'I', RailcraftItems.DUST, ItemDust.EnumDust.ENDER,
                'B', "blockDiamond");
    }

    private ItemStack getItem(IBlockAccess world, BlockPos pos) {
        int color = DEFAULT_SHADE;
        TileEntity tile = WorldPlugin.getBlockTile(world, pos);
        if (tile instanceof TileForceTrackEmitter) {
            color = ((TileForceTrackEmitter) tile).getColor();
        }
        return ItemForceTrackEmitter.setColor(new ItemStack(this), color);
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return getItem(world, pos);
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        drops.add(getItem(world, pos));
    }
}
