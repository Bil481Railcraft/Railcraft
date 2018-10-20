/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2018
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.util.chest;

import net.minecraft.inventory.IInventory;
import net.minecraft.world.World;

/**
 *
 */
public abstract class ChestLogic extends AbstractLogic {

    protected IInventory inventory;

    ChestLogic(World world, IInventory inventory) {
        super(world);
        this.inventory = inventory;
    }
}
