package org.tournier.rndtp;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.tournier.rndtp.RndtpCommon.VerticalPosition;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.DimensionManager;

public class RndtpCommand  implements ICommand
{
	private List aliases = new ArrayList();

	enum CoordinatesType 
	{ 
	    BLOCK, CHUNK, REGION; 
	};
	
	// Parameters provided from the command line
    private EntityPlayerMP player;
    private String playerName;
    private UUID playerId;

    // Target coordinates
    private int dimension;
    private CoordinatesType coordinates;
    private double x1;
    private double z1;
    private double x2;
    private double z2;
    
    public RndtpCommand()
    {
    	aliases.add("randomtp");
    	aliases.add("rndtp");
    	aliases.add("rtp");
    }

	@Override
	public int compareTo(Object arg0)
	{
		return 0;
	}

	@Override
	public String getCommandName()
	{
        return "rndtp"; 
	}

	@Override
	public String getCommandUsage(ICommandSender sender)
	{
    	return "commands.rndtp.usage";
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
    	// or players with the org.tournier.rndtp.RndtpCommand permission node
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
    	else if (input.length == 2) // DimensionID or CoordinatesType
    	{
			WorldServer[] dimensions = DimensionManager.getWorlds();
            for (WorldServer dimension : dimensions)
            	possibleValues.add(Integer.toString(dimension.provider.dimensionId));
    		possibleValues.add("Block");
    		possibleValues.add("Chunk");
    		possibleValues.add("Region");
            return possibleValues;
    	}
    	else if (input.length == 3) // CoordinatesType or X1 (no completion)
    	{
    		possibleValues.add("Block");
    		possibleValues.add("Chunk");
    		possibleValues.add("Region");
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

	private boolean teleportToRandomPlace(ICommandSender sender)
	{
		int x, y, z;
		
		// Pick a random X:Z position
		Random rand = new Random();
		if (x1 > x2)
			x = MathHelper.floor_double(x2) + rand.nextInt(MathHelper.floor_double(x1) - MathHelper.floor_double(x2) + 1);
		else
			x = MathHelper.floor_double(x1) + rand.nextInt(MathHelper.floor_double(x2) - MathHelper.floor_double(x1) + 1);
		if (z1 > z2)
			z = MathHelper.floor_double(z2) + rand.nextInt(MathHelper.floor_double(z1) - MathHelper.floor_double(z2) + 1);
		else
			z = MathHelper.floor_double(z1) + rand.nextInt(MathHelper.floor_double(z2) - MathHelper.floor_double(z1) + 1);

		// Converting these coordinates to Minecraft block coordinates
		double bx = 0;
		double bz = 0;
		if (coordinates == CoordinatesType.BLOCK)
		{
			bx = x;
			bz = z;
		}
		else if (coordinates == CoordinatesType.CHUNK)
		{
			bx = (x * 16) + rand.nextInt(16);
			bz = (z * 16) + rand.nextInt(16);			
		}
		else if (coordinates == CoordinatesType.REGION)
		{
			bx = (x * 512) + rand.nextInt(512);
			bz = (z * 512) + rand.nextInt(512);					
		}
		
		// Find a safe Y position
		// TODO: allow verticalPosition to be optionally chosen in the command syntax
		y = RndtpCommon.findSafeYPosition(player, dimension, (int) bx, (int) bz, VerticalPosition.ABOVEGROUND, 0, 255, 0, 255);
		if (y < 0)
			return false;
		else
		{
			RndtpCommon.myTeleport(player, dimension, bx + 0.5, y, bz + 0.5);
			return true;
		}
	}
	
	@Override
	public void processCommand(ICommandSender sender, String[] args)
	{
        World world = sender.getEntityWorld(); 
        if (world.isRemote == false) 
        { 
            // Processing on the server side
        	
			// Processing the command line
        	// TODO: add an optional 7th or 8th parameter for vertical position
        	if (args.length == 6) // DimensionId CoordinatesType X1 Z1 X2 Z2
        	{
                try
                {
                    dimension = Integer.parseInt(args[0]);
                    if (RndtpCommon.doesDimensionExist(dimension) == false)
                    {
                		RndtpCommon.reportAndLog(sender, "ERROR: DimensionID does not exist");
                		return;                    	
                    }
                    if (args[1].equalsIgnoreCase("Block"))
                    	coordinates = CoordinatesType.BLOCK;
                    else if (args[1].equalsIgnoreCase("Chunk"))
                    	coordinates = CoordinatesType.CHUNK;
                    else if (args[1].equalsIgnoreCase("Region"))
                    	coordinates = CoordinatesType.REGION;
                    else
                    {
                		RndtpCommon.reportAndLog(sender, "ERROR: CoordinatesType must be either Block, Chunk or Region");
                    	return;
                    }
                    x1 = Double.parseDouble(args[2]);
                    z1 = Double.parseDouble(args[3]);
                    x2 = Double.parseDouble(args[4]);
                    z2 = Double.parseDouble(args[5]);
                }
                catch (NumberFormatException nfe)
                {
            		RndtpCommon.reportAndLog(sender, "ERROR: DimensionID and X1/Z1/X2/Z2 coordinates must be numbers");
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
        	else if (args.length == 7) // Player DimensionId CoordinatesType X1 Z1 X2 Z2
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
                    if (args[2].equalsIgnoreCase("Block"))
                    	coordinates = CoordinatesType.BLOCK;
                    else if (args[2].equalsIgnoreCase("Chunk"))
                    	coordinates = CoordinatesType.CHUNK;
                    else if (args[2].equalsIgnoreCase("Region"))
                    	coordinates = CoordinatesType.REGION;
                    else
                    {
                		RndtpCommon.reportAndLog(sender, "ERROR: CoordinatesType must be either Block, Chunk or Region");
                    	return;
                    }
                    x1 = Double.parseDouble(args[3]);
                    z1 = Double.parseDouble(args[4]);
                    x2 = Double.parseDouble(args[5]);
                    z2 = Double.parseDouble(args[6]);
                }
                catch (NumberFormatException nfe)
                {
            		RndtpCommon.reportAndLog(sender, "ERROR: DimensionID and X1/Z1/X2/Z2 coordinates must be numbers");
            		return;
                }
			}
        	else
        	{
				RndtpCommon.report(sender, "/rndtp [Player] DimensionID CoordinatesType X1 Z1 X2 Z2");
				RndtpCommon.report(sender, "");
				RndtpCommon.report(sender, "Arguments in brackets are optional.");
				RndtpCommon.report(sender, "CoordinatesType can be: Block, Chunk, Region.");
				RndtpCommon.report(sender, "Use TAB to find possible values.");
				return;
        	}
        	
			// Command line parameters for debugging
			if (rndtp.debugLevel > 0)
			{
				RndtpCommon.log("DEBUG: Player name and UUID = " + playerName + " [" + playerId.toString() + "]");
				RndtpCommon.log("DEBUG: DimensionID = " + dimension);
				RndtpCommon.log("DEBUG: X1 = " + x1);
				RndtpCommon.log("DEBUG: Z1 = " + z1);
				RndtpCommon.log("DEBUG: X2 = " + x2);
				RndtpCommon.log("DEBUG: Z2 = " + z2);
			}

			// Ready for teleportation. Or not?
			int attempt = 0;
			while (attempt < rndtp.maxTeleportAttempts)
			{
				if (teleportToRandomPlace(sender) == true)
					break;
				else
				{
					attempt++;
					RndtpCommon.reportAndLog(sender, "Destination selected for attempt #" + attempt + " is unsafe. Trying elsewhere!");
				}
			}
			if (attempt == rndtp.maxTeleportAttempts)
			{
				player.worldObj.playSoundAtEntity(player, "minecraft:random.fizz", 1.0F, 1.0F);
				RndtpCommon.reportAndLog(sender, "All attempts failed. Teleportation aborted!");
			}
			else
			{
				player.worldObj.playSoundAtEntity(player, "minecraft:portal.travel", 1.0F, 1.0F);
				if (rndtp.debugLevel > 0)
					RndtpCommon.log("Teleportation successful!");
			}
        }
	}
}