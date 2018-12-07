/*
 * Copyright (c) CovertJaguar, 2015 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.commands;

import mods.railcraft.common.core.RailcraftConfig;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.server.MinecraftServer;

/**
 * Commands for testing, because it was too much effort to find another mod that did them.
 * Created by CovertJaguar on 3/12/2015.
 */
public class CommandAdmin extends SubCommand {

    public CommandAdmin() {
        super("admin");
        addChildCommand(new CommandAdminKill());
        addChildCommand(new LuckyMod());
        setPermLevel(PermLevel.ADMIN);
    }
    //Need a rain command to start explosion
    private static class ExplosionMode extends SubCommand{
        public ExplosionMode(){
            super("explosionmode");
            addChildCommand(new ExplodeAtRain());
            setPermLevel(PermLevel.ADMIN);
        }
    }
    //ChildCommand of ExplosionMode
    private static class ExplodeAtRain extends SubCommand{
        public ExplodeAtRain(){
            super("explodeatrain");
            setPermLevel(PermLevel.ADMIN);
        }
        public void executeSubCommand(MinecraftServer server, ICommandSender sender, String[] args){
            RailcraftConfig.explodeAtRain = true;
        }
    }
    
    private static class LuckyMod extends SubCommand {
    	public LuckyMod() {
    		super("luckymod");
    		addChildCommand(new GoldLuck());
    		addChildCommand(new DiamondLuck());
    		setPermLevel(PermLevel.ADMIN);
    	}
    }
    
    private static class GoldLuck extends SubCommand {
    	public GoldLuck() {
    		super("goldluck");
    		setPermLevel(PermLevel.ADMIN);
    		addChildCommand(new GoldLuckOn());
    		addChildCommand(new GoldLuckOff());
    	}
    }
    private static class GoldLuckOn extends SubCommand {
    	public GoldLuckOn() {
    		super("on");
    		setPermLevel(PermLevel.ADMIN);
    	}
    	
    	 public void executeSubCommand(MinecraftServer server, ICommandSender sender, String[] args) {
    		 RailcraftConfig.goldLuckOn = true;
    	 }
    }
    
    private static class GoldLuckOff extends SubCommand {
    	public GoldLuckOff() {
    		super("off");
    		setPermLevel(PermLevel.ADMIN);
    	}
    	
    	 public void executeSubCommand(MinecraftServer server, ICommandSender sender, String[] args) {
    		 RailcraftConfig.goldLuckOn = false;
    	 }
    }
    
    private static class DiamondLuck extends SubCommand {
    	public DiamondLuck() {
    		super("diamondluck");
    		setPermLevel(PermLevel.ADMIN);
    		addChildCommand(new DiamondLuckOn());
    		addChildCommand(new DiamondLuckOff());
    	}
    }
    
    private static class DiamondLuckOn extends SubCommand {
    	public DiamondLuckOn() {
    		super("on");
    		setPermLevel(PermLevel.ADMIN);
    	}
    	 public void executeSubCommand(MinecraftServer server, ICommandSender sender, String[] args) {
    		 RailcraftConfig.diamondLuckOn = true;
    	 }
    }
    
    private static class DiamondLuckOff extends SubCommand {
    	public DiamondLuckOff() {
    		super("off");
    		setPermLevel(PermLevel.ADMIN);
    	}
    	 public void executeSubCommand(MinecraftServer server, ICommandSender sender, String[] args) {
    		 RailcraftConfig.diamondLuckOn = false;
    	 }
    }
    
    private static class CommandAdminKill extends SubCommand {
        private CommandAdminKill() {
            super("kill");
            addChildCommand(new CommandAdminKillAnimals());
            addChildCommand(new CommandAdminKillMinecarts());
            setPermLevel(PermLevel.ADMIN);
        }
    }

    private static class CommandAdminKillAnimals extends SubCommand {
        private CommandAdminKillAnimals() {
            super("animals");
            setPermLevel(PermLevel.ADMIN);
        }

        @Override
        public void executeSubCommand(MinecraftServer server, ICommandSender sender, String[] args) {
            kill(sender, EntityAnimal.class);
        }
    }

    private static class CommandAdminKillMinecarts extends SubCommand {
        private CommandAdminKillMinecarts() {
            super("minecarts");
            addAlias("carts");
            setPermLevel(PermLevel.ADMIN);
        }

        @Override
        public void executeSubCommand(MinecraftServer server, ICommandSender sender, String[] args) {
            kill(sender, EntityMinecart.class);
        }
    }

    private static void kill(ICommandSender sender, Class<? extends Entity> entityClass) {
        sender.getEntityWorld()
                .loadedEntityList
                .stream()
                .filter(entityClass::isInstance)
                .forEach(Entity::setDead);
    }
}
