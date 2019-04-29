package org.tournier.rndtp;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

public class PretpCommand implements ICommand
{
	private List aliases = new ArrayList();
	
	// Parameters provided from the command line
    private EntityPlayerMP player;
    private String playerName;
    private UUID playerId;

    // Target coordinates
    private int dimension;
    private double x;
    private double y;
    private double z;
    
    public PretpCommand()
    {
    	aliases.add("precisiontp");
    	aliases.add("prectp");
    	aliases.add("pretp");
    	aliases.add("ptp");
    }

	@Override
	public int compareTo(Object arg0)
	{
		return 0;
	}

	@Override
	public String getCommandName()
	{
        return "pretp"; 
	}

	@Override
	public String getCommandUsage(ICommandSender sender)
	{
    	return "commands.pretp.usage";
	}

	@Override
	public List getCommandAliases()
	{
        return this.aliases;
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender)
	{
		// Never used...
    	return RndtpCommon.checkPermission(sender);
	}

    public int getRequiredPermissionLevel()
    {
    	// Command is allowed for Operators
    	// or players with the org.tournier.rndtp.PretpCommand permission node
        return 2;
    }

	@Override
	public List addTabCompletionOptions(ICommandSender sender, String[] input)
	{
		ArrayList<String> possibleValues = new ArrayList();

    	if (input.length == 1) // Player OR DimensionID
    	{
    		possibleValues.addAll(CommandBase.getListOfStringsMatchingLastWord(input, MinecraftServer.getServer().getAllUsernames()));
			WorldServer[] dimensions = DimensionManager.getWorlds();
            for (WorldServer dimension : dimensions)
            	possibleValues.add(Integer.toString(dimension.provider.dimensionId));
            return possibleValues;
    	}
    	else if (input.length == 2) // DimensionID or X (no completion)
    	{
			WorldServer[] dimensions = DimensionManager.getWorlds();
            for (WorldServer dimension : dimensions)
            	possibleValues.add(Integer.toString(dimension.provider.dimensionId));
            return possibleValues;
    	}
    	else
    		return null;
	}

	@Override
	public boolean isUsernameIndex(String[] arg, int i)
	{
		// If there is a username it'll be in arg #1, but not always...
		return i == 1; 
	}
	
	@Override
	public void processCommand(ICommandSender sender, String[] args)
	{
        World world = sender.getEntityWorld(); 
        if (world.isRemote == false) 
        { 
            // Processing on the server side
        	
			// Processing the command line
        	if (args.length == 4)
        	{
                try
                {
                    dimension = Integer.parseInt(args[0]);
                    if (RndtpCommon.doesDimensionExist(dimension) == false)
                    {
                		RndtpCommon.reportAndLog(sender, "ERROR: DimensionID does not exist");
                		return;                    	
                    }
                    x = Double.parseDouble(args[1]);
                    y = Double.parseDouble(args[2]);
                    z = Double.parseDouble(args[3]);
                }
                catch (NumberFormatException nfe)
                {
            		RndtpCommon.reportAndLog(sender, "ERROR: DimensionID and X/Y/Z coordinates must be numbers");
            		return;
                }
                
		    	playerName = sender.getCommandSenderName();
		    	player = RndtpCommon.getPlayerByName(playerName);
		    	if (player != null)
	        		playerId = player.getUniqueID();
		    	else
		    	{
		    		RndtpCommon.reportAndLog(sender, "ERROR: " + playerName + " has or was disconnected");
			    	return;	    		
		    	}
        	}
        	else if (args.length == 5)
			{
            	player = RndtpCommon.getPlayerByName(args[0]);
            	if (player != null)
            	{
            		playerName = args[0];
            		playerId = player.getUniqueID();
            	}
            	else
            	{
            		RndtpCommon.reportAndLog(sender, "ERROR: player is not connected");
            		return;
                }
        		
                try
                {
                    dimension = Integer.parseInt(args[1]);
                    if (RndtpCommon.doesDimensionExist(dimension) == false)
                    {
                		RndtpCommon.reportAndLog(sender, "ERROR: DimensionID does not exist");
                		return;                    	
                    }
                    x = Double.parseDouble(args[2]);
                    y = Double.parseDouble(args[3]);
                    z = Double.parseDouble(args[4]);
                }
                catch (NumberFormatException nfe)
                {
            		RndtpCommon.reportAndLog(sender, "ERROR: DimensionID and X/Y/Z coordinates must be numbers");
            		return;
                }
			}
        	else
        	{
				RndtpCommon.report(sender, "/pretp [Player] DimensionID X Y Z");
				RndtpCommon.report(sender, "");
				RndtpCommon.report(sender, "Arguments in brackets are optional.");
				RndtpCommon.report(sender, "Use TAB to find possible values.");
				return;
        	}
        	
			// Command line parameters for debugging
			if (rndtp.debugLevel > 0)
			{
				RndtpCommon.log("DEBUG: Player name and UUID = " + playerName + " [" + playerId.toString() + "]");
				RndtpCommon.log("DEBUG: DimensionID = " + dimension);
				RndtpCommon.log("DEBUG: X = " + x);
				RndtpCommon.log("DEBUG: Y = " + y);
				RndtpCommon.log("DEBUG: Z = " + z);
			}

			// Would the player be in a safe location?
			// TODO: Don't assume that the player has a 2 blocks height
			WorldServer ws = player.mcServer.worldServerForDimension(dimension);
			Block blockHead = ws.getBlock((int) x, (int) y + 1, (int) z);
			Block blockFeet = ws.getBlock((int) x, (int) y, (int) z);
			Block blockBelow = ws.getBlock((int) x, (int) y - 1, (int) z);
			if (rndtp.debugLevel > 1)
			{
				RndtpCommon.log("Head level block at y(" + (y + 1) + ")=" + Block.blockRegistry.getNameForObject(blockHead));
				RndtpCommon.log("Feet level block at y(" + y + ")=" + Block.blockRegistry.getNameForObject(blockFeet));
				RndtpCommon.log("Underfeet block at y(" + (y - 1) + ")=" + Block.blockRegistry.getNameForObject(blockBelow));
			}
			if (blockHead.equals(Blocks.air) && blockFeet.equals(Blocks.air) && RndtpCommon.isBlockSafeToStandUpon(ws, blockBelow, (int) x, (int) y - 1, (int) z) == true)
			{
				RndtpCommon.myTeleport(player, dimension, x + 0.5, y, z + 0.5);
				player.worldObj.playSoundAtEntity(player, "minecraft:portal.travel", 1.0F, 1.0F);
				if (rndtp.debugLevel > 0)
					RndtpCommon.log("Teleportation successful!");
			}	
			else
			{
				player.worldObj.playSoundAtEntity(player, "minecraft:random.fizz", 1.0F, 1.0F);
				RndtpCommon.reportAndLog(sender, "Destination is unsafe. Teleportation aborted!");
			}
        }
	}
}