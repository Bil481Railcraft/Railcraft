/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2018
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/
package mods.railcraft.common.blocks.multi;

import mods.railcraft.common.blocks.RailcraftBlocks;
import mods.railcraft.common.fluids.FluidTools;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public enum TankDefinition {
    IRON {
        {
            tankBlocks.add(RailcraftBlocks.TANK_IRON_GAUGE);
            tankBlocks.add(RailcraftBlocks.TANK_IRON_WALL);
            tankBlocks.add(RailcraftBlocks.TANK_IRON_VALVE);
        }

        @Override
        public String getTitle() {
            return "gui.railcraft.tank.iron";
        }

        @Override
        public boolean isWallBlock(IBlockState state) {
            return RailcraftBlocks.TANK_IRON_WALL.isEqual(state);
        }

        @Override
        public float getResistance(@Nullable Entity exploder) {
            return 20F;
        }

        @Override
        public int getCapacityPerBlock() {
            return 16 * FluidTools.BUCKET_VOLUME;
        }
    },
    STEEL {
        {
            tankBlocks.add(RailcraftBlocks.TANK_STEEL_GAUGE);
            tankBlocks.add(RailcraftBlocks.TANK_STEEL_VALVE);
            tankBlocks.add(RailcraftBlocks.TANK_STEEL_WALL);
        }

        @Override
        public String getTitle() {
            return "gui.railcraft.tank.steel";
        }

        @Override
        public boolean isWallBlock(IBlockState state) {
            return RailcraftBlocks.TANK_STEEL_WALL.isEqual(state);
        }

        @Override
        public float getResistance(@Nullable Entity exploder) {
            return 25F;
        }

        @Override
        public int getCapacityPerBlock() {
            return 32 * FluidTools.BUCKET_VOLUME;
        }
    };

    protected final Set<RailcraftBlocks> tankBlocks = new HashSet<>();

    /**
     * Returns a localization entry for the title.
     *
     * @return A localization entry
     */
    public abstract String getTitle();

    public boolean isTankBlock(IBlockState state) {
        return tankBlocks.stream().anyMatch(b -> b.isEqual(state));
    }

    public abstract boolean isWallBlock(IBlockState meta);

    public abstract float getResistance(@Nullable Entity exploder);

    public abstract int getCapacityPerBlock();
}
