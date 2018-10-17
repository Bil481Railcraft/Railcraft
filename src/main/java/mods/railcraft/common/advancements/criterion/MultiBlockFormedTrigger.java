/*------------------------------------------------------------------------------
 Copyright (c) CovertJaguar, 2011-2018
 http://railcraft.info

 This code is the property of CovertJaguar
 and may only be used with explicit written
 permission unless otherwise specified on the
 license page at http://railcraft.info/wiki/info:license.
 -----------------------------------------------------------------------------*/

package mods.railcraft.common.advancements.criterion;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import mods.railcraft.api.core.RailcraftConstantsAPI;
import mods.railcraft.common.advancements.criterion.MultiBlockFormedTrigger.Instance;
import mods.railcraft.common.blocks.multi.IMultiBlockTile;
import mods.railcraft.common.events.MultiBlockEvent;
import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 *
 */
final class MultiBlockFormedTrigger extends BaseTrigger<Instance> {

    static final ResourceLocation ID = RailcraftConstantsAPI.locationOf("multiblock_formed");

    MultiBlockFormedTrigger() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public Instance deserializeInstance(JsonObject json, JsonDeserializationContext context) {
        Class<? extends TileEntity> type;
        if (json.has("type")) {
            String id = JsonUtils.getString(json, "type");
            type = TileEntity.REGISTRY.getObject(new ResourceLocation(id));
        } else {
            type = null;
        }

        return new Instance(type);
    }

    @SubscribeEvent
    public void onMultiBlockForm(MultiBlockEvent.Form event) {
        IMultiBlockTile tile = event.getMultiBlock();
        GameProfile owner = tile.getOwner();
        EntityPlayerMP player = requireNonNull(FMLCommonHandler.instance().getMinecraftServerInstance()).getPlayerList().getPlayerByUUID(owner.getId());
        if (player == null) {
            return;
        }
        PlayerAdvancements advancements = player.getAdvancements();
        Collection<Listener<Instance>> done = manager.get(advancements).stream()
                .filter(listener -> listener.getCriterionInstance().matches(tile))
                .collect(Collectors.toList());
        for (Listener<Instance> listener : done) {
            listener.grantCriterion(advancements);
        }
    }

    static final class Instance implements ICriterionInstance {

        final @Nullable Class<? extends TileEntity> clazz;

        Instance(@Nullable Class<? extends TileEntity> type) {
            this.clazz = type;
        }

        // FIXME: This used to use the master class, but I removed that function and probably broke it. - CovertJaguar
        boolean matches(IMultiBlockTile tile) {
            return clazz == null || tile.getClass() == clazz;
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }
    }
}
