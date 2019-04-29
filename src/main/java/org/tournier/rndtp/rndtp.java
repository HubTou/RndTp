package org.tournier.rndtp;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

// Server side mod
@Mod(modid = rndtp.MODID, version = rndtp.VERSION, acceptableRemoteVersions = "*")
public class rndtp
{
    public static final String MODID = "rndtp";
    public static final String VERSION = "1.0";

    // Parameters provided in the generic configuration file
	public static int debugLevel;
	public static int maxTeleportAttempts;
	public static List unsafeBlocks = new ArrayList();
	
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        System.out.println("rndtp: mod initialization");
        
    	// Reading generic config file
		Configuration genericCfg = new Configuration(new File("config/rndtp/rndtp.cfg"));
		try
		{
			genericCfg.load();

			Property debugLevelProp = genericCfg.get(Configuration.CATEGORY_GENERAL, "DebugLevel", "0", "Values: 0 = None, 1 = Normal, 2+ = Verbose");
			debugLevel = debugLevelProp.getInt();
			Property maxTeleportAttemptsProp = genericCfg.get(Configuration.CATEGORY_GENERAL, "MaxTeleportAttempts", "5", "Values: 3 to 10 should be good");
			maxTeleportAttempts = maxTeleportAttemptsProp.getInt();
			Property unsafeBlocksProp = genericCfg.get(Configuration.CATEGORY_GENERAL, "UnsafeBlocks", new String[]
			{
				"lotr:tile.coralReef",
				"lotr:tile.hearth",
				"lotr:tile.marshLights",
				"lotr:tile.mobSpawner",
				"lotr:tile.morgulFlower",
				"lotr:tile.mordorThorn",
				"lotr:tile.quagmire",
				"lotr:tile.rhunFire",
				"lotr:tile.rhunFireJar",
				"lotr:tile.stalactite",
				"lotr:tile.stalactiteIce",
				"lotr:tile.stalactiteObsidian",
				"lotr:tile.termiteMound:0",			// example of blockname:datavalue
				"lotr:tile.webUngoliant"
			}, "List of unsafe blocks to stand upon");
			unsafeBlocks = Arrays.asList(unsafeBlocksProp.getStringList());

			if (debugLevel < 0)
			{
				RndtpCommon.log("ERROR: debugLevel value in the configuration file must be >= 0");
		        return;				
			}
			if (maxTeleportAttempts < 1)
			{
				RndtpCommon.log("ERROR: maxTeleportAttempts value in the configuration file must be >= 1");
		        return;				
			}
		}
		catch(Exception ex)
		{
			// I assume that type invalid values will trigger this...
			RndtpCommon.log("ERROR: configuration file \"config/rndtp/rndtp.cfg\" error");
	        ex.printStackTrace();
	        return;
		}
    	finally
    	{
    		if (genericCfg.hasChanged())
    			genericCfg.save();
    	}

		if (debugLevel > 0)
		{
			RndtpCommon.log("DEBUG: DebugLevel = " + debugLevel);
			RndtpCommon.log("DEBUG: MaxTeleportAttempts = " + String.valueOf(maxTeleportAttempts));
			RndtpCommon.log("DEBUG: UnsafeBlocks = " + unsafeBlocks);
		}
    }
 
    @EventHandler
    public void serverLoad(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new PretpCommand());
        event.registerServerCommand(new RndtpCommand());
        event.registerServerCommand(new GeotpCommand());
    }
}