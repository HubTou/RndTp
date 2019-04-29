# About RndTp
A Minecraft Forge 1.7.10 server-side mod that you can use for randomly teleporting players into specific areas.

It's designed for server operators to be used in server signs / pressure plates / command blocks in a server spawn in order to spread players on their chosen faction territory.

It contains 3 commands :
* pretp (PrecisionTeleport) is a mvtp (Multiverse-Core) / mwtp (ForgeEssentials) like command that can teleport a player at a specific location in a chosen dimension. It automatically aborts if the teleportation is unsafe.
* rndtp (RandomTeleport) will teleport a player at a random location within the rectangle defined by 2 sets of (block/chunk/region) X/Z coordinates. It will automatically adjust the Y coordinate to a safe above ground location
* geotp (GeographicalTeleport), the most interesting one, will teleport a player at a random location chosen in a list of (block/chunk/region/custom) sets of X/Z coordinates stored in directories/files. Again it will automatically adjust the Y coordinate to a safe place (more on this later). 

While not specifically dedicated to the Lord of the Rings mod (LOTRmod), this mod has a companion set of directories/files enabling you to teleport a player from the Overworld to the Middle-Earth dimension in predefined areas such as specific LOTR mod biomes / factions territories / waypoints / places of interest / etc. Or to the Utumno dimension at a chosen level/layer.

Furthermore, you can choose to teleport the player above or underground (for races like dwarves...), in bottom up or top down direction (the latest being useful for teleporting into the Nether or Utumno dimensions).

Unsafe blocks from other mods can be specified in the config file, and directories/files sets for other fixed map mods can be produced...

# Compilation

Just type "gradlew build" in a correctly set up Minecraft Forge development environment.

# Installation / Configuration / Usage

Please check the mod's wiki at https://lotr-minecraft-mod-exiles.fandom.com/wiki/RndTp_mod.

# Versions and changelog

1.00    2019-04-29

        Initial public release

# Caveats

Developed with Java 1.8 (recompile if your server is running under a lower version) and tested only on Minecraft 1.7.10 / Forge  10.13.4.1614 with a Thermos server.

# Limits & Known bugs

* If you make a TopDown or AboveGround teleportation in an already generated chunk, you might end up on top of a tree (doesn't happen in ungenerated chunks as trees are generated later)

* If you make a UnderGround or BottomUp teleportation, you risk getting some falling blocks (gravel, sand, etc.) or fluids (water, lava) on your head. Be sure to react quickly if you hear the sound of these events...

* In some rare cases of AboveGround teleportation, you could end up under ground if the LOTR mod decides to build a structure just above you... 

# Further development plans

None apart from eventual bug corrections.

Peculiarly I have no plan to port this to newer Minecraft versions as I will only use it for the LOTR mod.

# License

This open source software is distributed under a BSD license (see the "License" file for details).

# Credits

This mod includes a snippet of code from Wicked1 (http://www.minecraftforge.net/forum/profile/26013-wicked1/ copied from http://www.minecraftforge.net/forum/topic/14491-teleporting-a-player-to-a-new-dimension/?tab=comments#comment-89141

# Author

Hubert Tournier

29 April 2019
