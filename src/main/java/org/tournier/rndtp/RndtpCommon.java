package org.tournier.rndtp;

import java.util.Iterator;
import java.util.List;

import org.tournier.rndtp.RndtpCommon.VerticalPosition;

import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.DimensionManager;

public class RndtpCommon
{
	enum VerticalPosition 
	{ 
	    TOPDOWN, BOTTOMUP, ABOVEGROUND, UNDERGROUND, RANGE; 
	};
	
    public static void log(String message)
    {
    	System.out.println("rndtp: " + message);
    }
    
    public static void report(ICommandSender sender, String message)
    {
		sender.addChatMessage(new ChatComponentText(message));
    }
    
    public static void reportAndLog(ICommandSender sender, String message)
    {
    	log(message);
		sender.addChatMessage(new ChatComponentText(message));
    }
    
    public static EntityPlayerMP getPlayerByName(String name)
    {
        List<EntityPlayerMP> allPlayers = MinecraftServer.getServer().getConfigurationManager().playerEntityList;
        for (EntityPlayerMP currentPlayer : allPlayers) 
        {
        	if (currentPlayer.getDisplayName().equalsIgnoreCase(name))
        		return currentPlayer;
        }
        return null;
    }
 
    public static boolean checkPermission(ICommandSender sender)
    {
    	// Command is allowed for OP players (not necessarily in creative mode)
    	// or when a SinglePlayer world has cheats enabled
    	String currentPlayerName = sender.getCommandSenderName();
    	EntityPlayerMP currentPlayer = getPlayerByName(currentPlayerName);
    	if (currentPlayer == null)
    	{
			if (rndtp.debugLevel > 0)
				RndtpCommon.log("DEBUG: player we are checking permission for is null!");
			return true;
    	}
    	else
    		return MinecraftServer.getServer().getConfigurationManager().func_152596_g(currentPlayer.getGameProfile());
    }
    
    public static boolean isBlockSafeToStandUpon(World targetWorld, Block blockBelow, int x, int y, int z)
    {
		String blockBelowName = Block.blockRegistry.getNameForObject(blockBelow);
		int blockBelowMetadata = targetWorld.getBlockMetadata(x, y, z);

		if (   blockBelow.equals(Blocks.air)
			|| blockBelow.equals(Blocks.lava)
			|| blockBelow.equals(Blocks.flowing_lava)
			|| blockBelow.equals(Blocks.fire)
			|| blockBelow.equals(Blocks.water)
			|| blockBelow.equals(Blocks.flowing_water)
			|| blockBelow.equals(Blocks.web)
			|| blockBelow.equals(Blocks.cactus)
		   )
		{
			if (rndtp.debugLevel > 1)
				log("DEBUG: Unsafe block = " + blockBelowName + ":" + blockBelowMetadata);
			return false;
		}
		else
		{
			for(Iterator i = rndtp.unsafeBlocks.iterator(); i.hasNext();)
			{
				String unsafeBlock = ((String)i.next());
				String[] parts = unsafeBlock.split(":");
				if (parts.length == 2) // mod:block_name
				{
			        if (unsafeBlock.equalsIgnoreCase(blockBelowName))
					{
						if (rndtp.debugLevel > 1)
							log("DEBUG: Unsafe block = " + blockBelowName + ":" + blockBelowMetadata);
						return false;
					}
				}
				else if (parts.length == 3) // mod:block_name:data_value
				{
					String unsafeBlockName = parts[0] + ":" + parts[1];
			        if (unsafeBlockName.equalsIgnoreCase(blockBelowName) && parts[2].equals(Integer.toString(blockBelowMetadata)))
					{
						if (rndtp.debugLevel > 1)
							log("DEBUG: Unsafe block = " + blockBelowName + ":" + blockBelowMetadata);
						return false;
					}
				}
				else
				{
					if (rndtp.debugLevel > 0)
						log("ERROR: Bad number of colons in config file entry = " + unsafeBlock);
				}
			}
			if (rndtp.debugLevel > 1)
				log("DEBUG: Safe block = " + blockBelowName + ":" + blockBelowMetadata);
		    return true;
		}
    }
    
    public static boolean doesDimensionExist(int targetDimension)
    {
        WorldServer[] dimensions = DimensionManager.getWorlds();
        for (WorldServer dimension : dimensions)
        {
        	if (dimension.provider.dimensionId == targetDimension)
        		return true;
        }
        return false;
    }

    public static int findSafeYPosition(EntityPlayerMP player, int targetDimension, int targetX, int targetZ, VerticalPosition verticalPosition, int verticalMinimum, int verticalMaximum, int configVerticalMinimum, int configVerticalMaximum)
    {
		if (rndtp.debugLevel > 0)
			log("DEBUG: Destination: Dim = " + targetDimension + " / X = " + targetX + " / Y = not yet selected / Z = " + targetZ);

    	int y = -1;
		// TODO: Don't assume that the player has a 2 blocks height
		int playerHeight = 2;
		Block blockBelow, blockFeet, blockHead;

		WorldServer ws = player.mcServer.worldServerForDimension(targetDimension);
		if (verticalPosition == VerticalPosition.ABOVEGROUND)
		{
			y = ws.getTopSolidOrLiquidBlock(targetX, targetZ);
			
			if (y < configVerticalMinimum || y > configVerticalMaximum)
				return -1;
			
			blockBelow = ws.getBlock(targetX, y - 1, targetZ);
			if (RndtpCommon.isBlockSafeToStandUpon(ws, blockBelow, targetX, y - 1, targetZ) == false)
				return -1;

			// Despite its name, the getTopSolidOrLiquidBlock method sometimes places the player at the bottom of water!
			blockHead = ws.getBlock(targetX, y + 1, targetZ);
			if (blockHead.equals(Blocks.water))
				return -1;
		}
		else if (verticalPosition == VerticalPosition.TOPDOWN || (verticalPosition == VerticalPosition.RANGE && verticalMinimum > verticalMaximum))
		{
			// Try to place the player in the first safe place starting from the top of the world
			// When the destination is not yet generated, there's usually no risk of placing the player above a tree
			// TODO: Do not place the player upon a tree in already generated chunks
			int yMin;
			if  (verticalPosition == VerticalPosition.TOPDOWN)
			{
				y = player.worldObj.getHeight() - playerHeight;
				if (y > configVerticalMaximum)
					y = configVerticalMaximum;
				yMin = 1;
				if (yMin < configVerticalMinimum)
					yMin = configVerticalMinimum;
			}
			else
			{
				y = verticalMinimum;
				yMin = verticalMaximum;
			}
			while (y >= yMin)
			{
				blockFeet = ws.getBlock(targetX, y, targetZ);
				if (blockFeet.equals(Blocks.air))
				{
					blockHead = ws.getBlock(targetX, y + 1, targetZ);
					if (blockHead.equals(Blocks.air))
					{
						blockBelow = ws.getBlock(targetX, y - 1, targetZ);
						if (blockBelow.equals(Blocks.air))
							y--;
						else if (RndtpCommon.isBlockSafeToStandUpon(ws, blockBelow, targetX, y - 1, targetZ) == false)
							y -= (playerHeight + 1);
						else
							break;
					}
					else
						y -= (playerHeight - 1);
				}
				else
					y -= playerHeight;
			}
			if (y < yMin)
				return -1;
		}
		else if (verticalPosition == VerticalPosition.BOTTOMUP || (verticalPosition == VerticalPosition.RANGE && verticalMinimum <= verticalMaximum))
		{
			// Try to place the player in the first safe place starting from the bottom of the world
			int yMax;
			if  (verticalPosition == VerticalPosition.BOTTOMUP)
			{
				y = 1;
				if (y < configVerticalMinimum)
					y = configVerticalMinimum;
				yMax = ws.getHeight() - playerHeight;
				if (yMax > configVerticalMaximum)
					yMax = configVerticalMaximum;
			}
			else
			{
				y = verticalMinimum;
				yMax = verticalMaximum;
			}
			while (y <= yMax)
			{
				blockFeet = ws.getBlock(targetX, y, targetZ);
				if (blockFeet.equals(Blocks.air))
				{
					blockHead = ws.getBlock(targetX, y + 1, targetZ);
					if (blockHead.equals(Blocks.air))
					{
						blockBelow = ws.getBlock(targetX, y - 1, targetZ);
						if (RndtpCommon.isBlockSafeToStandUpon(ws, blockBelow, targetX, y - 1, targetZ) == false)
							y += (playerHeight + 1);
						else
							break;
					}
					else
						y += 2;
				}
				else
					y++;
			}
			if (y > yMax)
				return -1;	            	            	
		}
		else if (verticalPosition == VerticalPosition.UNDERGROUND)
		{
			// Try to place the player in the first safe place starting from the bottom of the world
			// up to the ground level
			y = 1;
			if (y < configVerticalMinimum)
				y = configVerticalMinimum;
			int yMax = ws.getTopSolidOrLiquidBlock(targetX, targetZ);
			if (yMax > configVerticalMaximum)
				yMax = configVerticalMaximum;
			while (y < yMax)
			{
				blockFeet = ws.getBlock(targetX, y, targetZ);
				if (blockFeet.equals(Blocks.air))
				{
					blockHead = ws.getBlock(targetX, y + 1, targetZ);
					if (blockHead.equals(Blocks.air))
					{
						blockBelow = ws.getBlock(targetX, y - 1, targetZ);
						if (RndtpCommon.isBlockSafeToStandUpon(ws, blockBelow, targetX, y - 1, targetZ) == false)
							y += (playerHeight + 1);
						else
							break;
					}
					else
						y += 2;
				}
				else
					y++;
			}
			if (y >= yMax)
				return -1;	            	            	
		}

		if (rndtp.debugLevel > 0)
			log("DEBUG: Destination: Dim = " + targetDimension + " / X = " + targetX + " / Y = " + y + " / Z = " + targetZ);
		
		return y;
    }
    
    public static void myTeleport(EntityPlayerMP player, int targetDimension, double targetX, double targetY, double targetZ)
    {
        player.mountEntity((Entity) null);
    	if (player.worldObj.provider.dimensionId == targetDimension)
    	{
    		// Normal (intra-dimension) teleportation
    		if (rndtp.debugLevel > 1)
    			log("Normal teleportation (same dimension)");
    		
            player.setPositionAndUpdate(targetX, targetY, targetZ);
    	}
//   	// TODO: this method for detecting that the player is located in a spawn protected area doesn't work!
//   	// isBlockProtected() source code always return false
//    	else if (player.mcServer.isBlockProtected(player.getEntityWorld(), MathHelper.floor_double(player.posX), MathHelper.floor_double(player.posY - 1), MathHelper.floor_double(player.posZ), player) == true)
//    	{
//    		// Teleportation with portal creation at the origin (unless if located in a protected area) but no invisible mobs at the destination
//    		if (rndtp.debugLevel > 0)
//    			log("Interdimensional teleportation (method 1)");
//
//    		player.travelToDimension(targetDimension);
//    		player.setPositionAndUpdate(targetX, targetY, targetZ);
//    	}
    	else
    	{	
    		// Teleportation with no portal creation at the origin but (possible) invisible mobs at the destination
    		if (rndtp.debugLevel > 1)
    			log("Interdimensional teleportation (method 2)");

    		WorldServer targetWorldServer = player.mcServer.worldServerForDimension(targetDimension);
			MyTeleporter teleporter = new MyTeleporter(targetWorldServer);
			player.mcServer.getConfigurationManager().transferPlayerToDimension(player, targetDimension, teleporter);
			player.setPositionAndUpdate(targetX, targetY, targetZ);
    	}
    }
}