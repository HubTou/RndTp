package org.tournier.rndtp;

import org.tournier.rndtp.RndtpCommon;
import org.tournier.rndtp.RndtpCommon.VerticalPosition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.*;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class GeotpCommand implements ICommand
{ 
	private List aliases = new ArrayList();
	
	// Parameters provided from the command line
    private EntityPlayerMP player;
    private String playerName;
    private UUID playerId;
    private String coordinatesFilename;
    private BufferedReader coordinatesFile;
    private String configurationFilename;
    private File configurationFile;
    private VerticalPosition verticalPosition;
    private int verticalMinimum;
    private int verticalMaximum;
 
    // Parameters provided in the specific configuration file
    private int dimension;
    private String coordinatesType;
    private int blocksWidth;
    private double xOffset;
    private double zOffset;
    private int minimalY;
    private int maximalY;
    
    public GeotpCommand()
    {
    	aliases.add("geotp");
    	aliases.add("gtp");
    }
    
    @Override 
    public int compareTo(Object o)
    {
        return 0;
    } 

    @Override 
    public String getCommandName() 
    { 
        return "geotp"; 
    } 

    @Override         
    public String getCommandUsage(ICommandSender sender) 
    {
    	return "commands.geotp.usage";
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
    	// or players with the org.tournier.rndtp.GeotpCommand permission node
        return 2;
    }

    private ArrayList<String> getAllDirectories(String dirName, String beginning)
    {
    	ArrayList<String> allDirectories = new ArrayList();
    	
		File directory = new File(dirName);
		if (! directory.exists() || ! directory.isDirectory())
			return null;

		File[] item = directory.listFiles();
		if (item == null)
			return null;
		else
        	for (int i = 0; i < item.length; i++)
        		if (item[i].isDirectory() == true && item[i].getName().startsWith(beginning))
        			allDirectories.add(item[i].getName());
		
		return allDirectories;	
    }

    private ArrayList<String> getAllDirectoryFiles(String dirName, String beginning)
    {
    	ArrayList<String> allDirectoryFiles = new ArrayList();

		File directory = new File(dirName);
		if (! directory.exists() || ! directory.isDirectory())
			return null;
    	String lastDirectory = dirName.substring(dirName.lastIndexOf("/") + 1); 

		File[] item = directory.listFiles();
		if (item == null)
			return null;
		else
	       	for (int i = 0; i < item.length; i++)
	       		if (item[i].isFile() == true && ! item[i].getName().equals("rndtp.cfg") && item[i].getName().startsWith(beginning))
	       			allDirectoryFiles.add(lastDirectory + ":" + item[i].getName());
		
		return allDirectoryFiles;
    }
       
    @Override  
    public List addTabCompletionOptions(ICommandSender sender, String[] input) 
    {
    	if (input.length == 1) // Player OR Directory:File
    	{
			ArrayList<String> possibleValues = new ArrayList();
			String[] parts;
			char lastCharacter = ' ';

			possibleValues.addAll(CommandBase.getListOfStringsMatchingLastWord(input, MinecraftServer.getServer().getAllUsernames()));

			parts = input[0].split(":");
			if (input[0].length() > 0)
				lastCharacter = input[0].charAt(input[0].length() - 1);
			if (parts.length == 1 && lastCharacter != ':')
				possibleValues.addAll(getAllDirectories("config/rndtp", parts[0]));
			else if (parts.length == 1 && lastCharacter == ':')
				possibleValues.addAll(getAllDirectoryFiles("config/rndtp/" + parts[0], ""));
			else if (parts.length == 2)
				possibleValues.addAll(getAllDirectoryFiles("config/rndtp/" + parts[0], parts[1]));
			else
				return null;

			return possibleValues;
    	}
    	else if (input.length == 2) 
    	{
    		EntityPlayerMP playerInfo = RndtpCommon.getPlayerByName(input[0]);
    		if (playerInfo == null) // Directory:File VerticalPosition
        		return CommandBase.getListOfStringsMatchingLastWord(input, new String[] {"AboveGround", "AG", "BottomUp", "BU", "TD", "TopDown", "UG", "UnderGround"});
    		else // Player Directory:File
    		{
    			String[] parts;
    			char lastCharacter = ' ';

    			parts = input[1].split(":");
    			if (input[1].length() > 0)
    				lastCharacter = input[1].charAt(input[1].length() - 1);
    			if (parts.length == 1 && lastCharacter != ':')
    				return getAllDirectories("config/rndtp", parts[0]);
    			else if (parts.length == 1 && lastCharacter == ':')
    				return getAllDirectoryFiles("config/rndtp/" + parts[0], "");
    			else if (parts.length == 2)
    				return getAllDirectoryFiles("config/rndtp/" + parts[0], parts[1]);
    			else
    				return null;
    		}
    	}
    	else if (input.length == 3) // VerticalPosition
    		return CommandBase.getListOfStringsMatchingLastWord(input, new String[] {"AboveGround", "AG", "BottomUp", "BU", "TD", "TopDown", "UG", "UnderGround"});
    	else
    		return null;
    } 

    @Override 
    public boolean isUsernameIndex(String[] input, int i) 
    { 
    	// If there is a username it'll be in arg #1, but not always...
    	return i == 1; 
    } 

    private boolean testAndSetIfArgIsADirectoryFile(ICommandSender sender, String arg, int argNumber)
    {
		String[] parts = arg.split(":");
		if (parts.length != 2)
		{
			RndtpCommon.reportAndLog(sender, "ERROR: argument " + argNumber + " must be of the form Directory:File");
			return false;
		}
		
		File directory = new File("config/rndtp/" + parts[0]);
		if (! directory.exists())
		{
			RndtpCommon.reportAndLog(sender, "ERROR: Directory part of argument " + argNumber + " does not exist");
			return false;
		}
		if (! directory.isDirectory())
		{
			RndtpCommon.reportAndLog(sender, "ERROR: Directory part of argument " + argNumber + " is not a directory");
			return false;		
		}

		File file = new File("config/rndtp/" + parts[0] + "/" + parts[1]);
		if (! file.exists())
		{
			RndtpCommon.reportAndLog(sender, "ERROR: File part of argument " + argNumber + " does not exist");
			return false;
		}
		if (! file.isFile())
		{
			RndtpCommon.reportAndLog(sender, "ERROR: File part of argument " + argNumber + " is not a file");
			return false;	
		}

		coordinatesFilename = "config/rndtp/" + parts[0] + "/" + parts[1];
		configurationFilename = "config/rndtp/" + parts[0] + "/rndtp.cfg";
		return true;
    }
    
    private boolean testAndSetIfArgIsAVerticalPosition(ICommandSender sender, String arg, int argNumber)
    {
    	Pattern pattern = Pattern.compile("([0-9][0-9]*)-([0-9][0-9]*)");
        Matcher matcher = pattern.matcher(arg);

    	if (arg.equalsIgnoreCase("AboveGround") || arg.equalsIgnoreCase("AG"))
    	{
    		verticalPosition = VerticalPosition.ABOVEGROUND;
        	return true;   		
    	}
    	else if (arg.equalsIgnoreCase("UnderGround") || arg.equalsIgnoreCase("UG"))
    	{
    		verticalPosition = VerticalPosition.UNDERGROUND;
        	return true;   		
    	}
    	else if (arg.equalsIgnoreCase("TopDown") || arg.equalsIgnoreCase("TD"))
    	{
    		verticalPosition = VerticalPosition.TOPDOWN;
        	return true; 		
    	}
    	else if (arg.equalsIgnoreCase("BottomUp") || arg.equalsIgnoreCase("BU"))
    	{
    		verticalPosition = VerticalPosition.BOTTOMUP;
        	return true;   		
    	}
    	else if (matcher.matches())
    	{
    		verticalPosition = VerticalPosition.RANGE;
    		verticalMinimum = Integer.valueOf(matcher.group(1));
    		verticalMaximum = Integer.valueOf(matcher.group(2));
    		return true;
    	}
    	else
    		return false;
    }

    private boolean testAndSetIfArgIsAPlayer(ICommandSender sender, String arg, int argNumber)
    {
    	player = RndtpCommon.getPlayerByName(arg);
    	if (player != null)
    	{
    		playerName = arg;
    		playerId = player.getUniqueID();
    		return true;
    	}
    	else
    	{
    		RndtpCommon.reportAndLog(sender, "ERROR: argument " + argNumber + " is not a connected player name");
        	return false;
        }
    }

    private boolean teleportToRandomPlace(ICommandSender sender, int numberOfLines)
    {
		BufferedReader coordinatesFile;
		String line = null;
		try
    	{
			// Picking a random line
			Random rand = new Random();
			int lineSelected = rand.nextInt(numberOfLines) + 1;
			
			// Reading that line
			coordinatesFile = new BufferedReader(new FileReader(coordinatesFilename));
			int lineNumber = 0;
			while (lineNumber != lineSelected)
			{
				line = coordinatesFile.readLine();
				lineNumber++;
			}
			coordinatesFile.close();
			if (rndtp.debugLevel > 0)
				RndtpCommon.log("DEBUG: Line " + lineSelected + " has been randomly selected and contains \"" + line + "\"");
    	}
		catch (IOException e)
		{
			RndtpCommon.reportAndLog(sender, "ERROR: issue while reading the coordinates file");
			e.printStackTrace();
			return false;
		}

		// Extracting the X and Z coordinates
		String[] parts = line.split(":");
		if (parts.length != 2)
		{
			RndtpCommon.reportAndLog(sender, "ERROR: the line selected in the coordinates file is not of the X:Z form");		
			return false;
		}
		int x = Integer.valueOf(parts[0]);
		int z = Integer.valueOf(parts[1]);
		
		// Converting these coordinates to Minecraft block coordinates
		Random rand = new Random();
		double bx;
		double bz;
		if (coordinatesType.equalsIgnoreCase("Block"))
		{
			bx = x;
			bz = z;
		}
		else if (coordinatesType.equalsIgnoreCase("Chunk"))
		{
			bx = (x * 16) + rand.nextInt(16);
			bz = (z * 16) + rand.nextInt(16);			
		}
		else if (coordinatesType.equalsIgnoreCase("Region"))
		{
			bx = (x * 512) + rand.nextInt(512);
			bz = (z * 512) + rand.nextInt(512);					
		}
		else if (coordinatesType.equalsIgnoreCase("LotMapPixel"))
		{
			bx = (( x - 809.5 ) * 128) + rand.nextInt(128);
			bz = (( z - 729.5 ) * 128) + rand.nextInt(128);			
		}
		else // if (coordinatesType.equalsIgnoreCase("Custom"))
		{
			bx = (( x + xOffset ) * blocksWidth) + rand.nextInt(blocksWidth);
			bz = (( z + zOffset ) * blocksWidth) + rand.nextInt(blocksWidth);						
		}

		// Find a safe Y position
		int y = RndtpCommon.findSafeYPosition(player, dimension, (int) bx, (int) bz, verticalPosition, verticalMinimum, verticalMaximum, minimalY, maximalY);
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
        	if (args.length == 1 || args.length == 2 || args.length == 3 )
			{
				switch (args.length)
				{
				case 1: // Directory:File
					if (testAndSetIfArgIsADirectoryFile(sender, args[0], 1) == false)
						return;
					else
					{
				    	playerName = sender.getCommandSenderName();
				    	player = RndtpCommon.getPlayerByName(playerName);
				    	if (player != null)
				    	{
			        		playerId = player.getUniqueID();
							verticalPosition = VerticalPosition.ABOVEGROUND;
				    	}
				    	else
				    	{
				    		RndtpCommon.reportAndLog(sender, "ERROR: " + playerName + " has or was disconnected");
					    	return;	    		
				    	}
					}
					break;

				case 2: // Directory:File VerticalPosition OR Player Directory:File
					if (testAndSetIfArgIsAVerticalPosition(sender, args[1], 2) == true)
					{
						if (testAndSetIfArgIsADirectoryFile(sender, args[0], 1) == false)
							return;
						else
						{
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
					}
					else // Player Directory:File
					{
						if ((testAndSetIfArgIsAPlayer(sender, args[0], 1) == false) || (testAndSetIfArgIsADirectoryFile(sender, args[1], 2) == false))
							return;
						else
							verticalPosition = VerticalPosition.ABOVEGROUND;
					}
					break;

				case 3: // Player Directory:File VerticalPosition
					if (testAndSetIfArgIsAPlayer(sender, args[0], 1) == false)
						return;
					if (testAndSetIfArgIsADirectoryFile(sender, args[1], 2) == false)
						return;
					if (testAndSetIfArgIsAVerticalPosition(sender, args[2], 3) == false)
					{
						RndtpCommon.reportAndLog(sender, "ERROR: argument " + 3 + " must be a vertical position");
						return;
					}									
					break;
				}
			
				// Command line parameters for debugging
				if (rndtp.debugLevel > 0)
				{
					RndtpCommon.log("DEBUG: Player name and UUID = " + playerName + " [" + playerId.toString() + "]");
					RndtpCommon.log("DEBUG: Path to coordinates file = " + coordinatesFilename);
					RndtpCommon.log("DEBUG: Vertical position = " + String.valueOf(verticalPosition));
					if (verticalPosition == VerticalPosition.RANGE)
					{
						RndtpCommon.log("DEBUG: Minimum vertical position = " + verticalMinimum);
						RndtpCommon.log("DEBUG: Maximum vertical position = " + verticalMaximum);						
					}
				}

				// Reading config file for selected destination
				Configuration SpecificCfg = new Configuration(new File(configurationFilename));
				try
				{
					SpecificCfg.load();

					Property dimensionProp = SpecificCfg.get(Configuration.CATEGORY_GENERAL, "Dimension", "0", "Dimension ID");
					dimension = dimensionProp.getInt();
					Property coordinatesTypeProp = SpecificCfg.get(Configuration.CATEGORY_GENERAL, "CoordinatesType", "Region", "Type of coordinates (Block, Chunk, Region, LotrMapPixel, Custom)");
					coordinatesType = coordinatesTypeProp.getString();
					Property blocksWidthProp = SpecificCfg.get(Configuration.CATEGORY_GENERAL, "BlocksWidth", "512", "Width in blocks of a coordinate unit (default = 512)");
					blocksWidth = blocksWidthProp.getInt();
					Property xOffsetProp = SpecificCfg.get(Configuration.CATEGORY_GENERAL, "XOffset", "0.0", "Is there a X offset compared to Minecraft coordinates (0 = no)");
					xOffset = xOffsetProp.getDouble();
					Property zOffsetProp = SpecificCfg.get(Configuration.CATEGORY_GENERAL, "ZOffset", "0.0", "Is there a Z offset compared to Minecraft coordinates (0 = no)");
					zOffset = zOffsetProp.getDouble();				
					Property yMinProp = SpecificCfg.get(Configuration.CATEGORY_GENERAL, "YMin", "0", "Is there a Y maximum altitude?");
					minimalY = yMinProp.getInt();
					Property yMaxProp = SpecificCfg.get(Configuration.CATEGORY_GENERAL, "YMax", "255", "Is there a Y maximum altitude?");
					maximalY = yMaxProp.getInt();

                    if (RndtpCommon.doesDimensionExist(dimension) == false)
                    {
                		RndtpCommon.reportAndLog(sender, "ERROR: dimension value in the specific configuration file does not exist");
                		return;                    	
                    }
                    // coordinatesType is checked later
                    if (blocksWidth < 1)
                    {
                		RndtpCommon.reportAndLog(sender, "ERROR: blocksWidth value in the specific configuration file must be >= 1");
                		return;                    	
                    }
                    if (minimalY < 0 || minimalY > 255)
                    {
                		RndtpCommon.reportAndLog(sender, "ERROR: yMin value in the specific configuration file must be [0-255]");
                		return;                    	
                    }
                    if (maximalY < 0 || maximalY > 255)
                    {
                		RndtpCommon.reportAndLog(sender, "ERROR: yMax value in the specific configuration file must be [0-255]");
                		return;                    	
                    }
				}
				catch(Exception ex)
				{
					// I assume that type invalid values will trigger this...
					RndtpCommon.reportAndLog(sender, "rndtp: ERROR: Specific configuration file \"" + configurationFilename + "\" error");
					ex.printStackTrace();
					return;
				}
		    	finally
		    	{
		    		if (SpecificCfg.hasChanged())
		    			SpecificCfg.save();
		    	}
				
				// Config file parameters for debugging
				if (rndtp.debugLevel > 0)
				{
					RndtpCommon.log("DEBUG: Dimension = " + String.valueOf(dimension));
					RndtpCommon.log("DEBUG: CoordinatesType = " + coordinatesType);
					RndtpCommon.log("DEBUG: BlocksWidth = " + String.valueOf(blocksWidth));
					RndtpCommon.log("DEBUG: XOffset = " + String.valueOf(xOffset));
					RndtpCommon.log("DEBUG: ZOffset = " + String.valueOf(zOffset));
				}

				BufferedReader coordinatesFile;
				int numberOfLines = 0;
				String line;
				try
		    	{
					// Counting the number of lines in the coordinates file
					// It would have been quicker to just pick a random byte in the file and backward read the associated line
					// but it would have favored spots with long coordinates against short ones, and I want equal distribution chances
					coordinatesFile = new BufferedReader(new FileReader(coordinatesFilename));
					while ((line = coordinatesFile.readLine()) != null)
					    numberOfLines++;
					coordinatesFile.close();
					if (rndtp.debugLevel > 0)
						RndtpCommon.log("DEBUG: " + coordinatesFilename + " has " + numberOfLines + " lines");
		    	}
				catch (IOException e)
				{
					RndtpCommon.reportAndLog(sender, "ERROR: issue while reading the coordinates file");
					e.printStackTrace();
					return;
				}

				// Ready for teleportation. Or not?
				int attempt = 0;
				int maxAttempts = rndtp.maxTeleportAttempts;
				if (numberOfLines == 1 && blocksWidth == 1)
					maxAttempts = 1;
				while (attempt < maxAttempts)
				{
					if (teleportToRandomPlace(sender, numberOfLines) == true)
						break;
					else
					{
						attempt++;
						if (verticalPosition == VerticalPosition.UNDERGROUND)
							RndtpCommon.reportAndLog(sender, "Destination selected for attempt #" + attempt + " has no underground cavity. Trying elsewhere!");
						else
							RndtpCommon.reportAndLog(sender, "Destination selected for attempt #" + attempt + " is unsafe. Trying elsewhere!");
					}
				}
				if (attempt == maxAttempts)
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
	        else
	        {
	        	RndtpCommon.report(sender, "/geotp [Player] Directory:File [VerticalPosition]");
	        	RndtpCommon.report(sender, "");
	        	RndtpCommon.report(sender, "Arguments in brackets are optional.");
	        	RndtpCommon.report(sender, "VerticalPosition can be:");
	        	RndtpCommon.report(sender, "   AboveGround|AG (by default), UnderGround|UG, TopDown|TD|Y2-Y1, BottomUp|BU|Y1-Y2.");
	        	RndtpCommon.report(sender, "Use TAB to find possible values.");
	        }
        } 
    } 
}