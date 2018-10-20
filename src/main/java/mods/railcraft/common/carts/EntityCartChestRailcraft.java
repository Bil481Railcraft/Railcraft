/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2018
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.carts;

import mods.railcraft.common.core.RailcraftConfig;
import mods.railcraft.common.fluids.FluidItemHelper;
import mods.railcraft.common.gui.EnumGui;
import mods.railcraft.common.util.chest.ChestLogic;
import mods.railcraft.common.util.misc.Game;
import mods.railcraft.common.util.misc.MiscTools;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public abstract class EntityCartChestRailcraft extends CartBaseContainer {
    private int clock = MiscTools.RANDOM.nextInt();
    protected final ChestLogic logic = createLogic();

    protected EntityCartChestRailcraft(World world) {
        super(world);
    }

    protected abstract ChestLogic createLogic();

    @Override
    public abstract IRailcraftCartContainer getCartType();

    @Override
    public abstract IBlockState getDefaultDisplayTile();

    protected abstract int getTickInterval();

    @Override
    public boolean doInteract(EntityPlayer player, EnumHand hand) {
        if (Game.isHost(world))
            player.displayGUIChest(this);
        return true;
    }

    @Override
    public int getDefaultDisplayTileOffset() {
        return 8;
    }

    @Override
    public boolean canBeRidden() {
        return false;
    }

    @Override
    public int getSizeInventory() {
        return 27;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return RailcraftConfig.chestAllowLiquids() || !FluidItemHelper.isContainer(stack);
    }

    @Override
    public boolean canPassItemRequests(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canAcceptPushedItem(EntityMinecart requester, ItemStack stack) {
        return true;
    }

    @Override
    public boolean canProvidePulledItem(EntityMinecart requester, ItemStack stack) {
        return true;
    }

    @Override
    public String getGuiID() {
        return "minecraft:chest";
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        clock++;

        if (Game.isHost(world) && clock % getTickInterval() == 0) {
            logic.update();
        }
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return new ContainerChest(playerInventory, this, playerIn);
    }

    @Override
    protected final EnumGui getGuiType() {
        throw new Error("Should not be called");
    }

}
