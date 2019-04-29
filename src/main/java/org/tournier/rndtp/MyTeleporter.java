package org.tournier.rndtp;

// This code was written by Wicked1 (http://www.minecraftforge.net/forum/profile/26013-wicked1/)
// and copied from http://www.minecraftforge.net/forum/topic/14491-teleporting-a-player-to-a-new-dimension/?tab=comments#comment-89141

import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class MyTeleporter extends Teleporter
{
	private final WorldServer worldServerInstance;

	public MyTeleporter(WorldServer worldServer)
	{
		super(worldServer);
		this.worldServerInstance = worldServer;
	}

	@Override
	public void placeInPortal(Entity entity, double param2, double param3, double param4, float param5)
	{
		// dont do ANY portal junk, just grab a dummy block then SHOVE the player setPosition() at height
		int i = MathHelper.floor_double(entity.posX);
	    int j = MathHelper.floor_double(entity.posY);
	    int k = MathHelper.floor_double(entity.posZ);
	    this.worldServerInstance.getBlock(i, j, k); //dummy load to maybe gen chunk
	    int height = this.worldServerInstance.getHeightValue(i, k);
	    entity.setPosition( i, height, k );
	    return;
	}
}