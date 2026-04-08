package teenTech;

import battlecode.common.*;

public class RatKing {
	public static void run(RobotController rc)
	{
		while (true)
		{
			try {
				tryToSpawn(rc);	

			}
			catch (GameActionException e){
				System.out.println(e);
			}
			Clock.yield();
		}
	}

	public static void tryToSpawn(RobotController rc) throws GameActionException
	{
		//pick a location to spawn
		// -- get the list of possible spawn locations
		// -- go with the first one on the list that is available
		MapLocation[] spawnLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 8);

		for (MapLocation loc: spawnLocs) {
			//spawn a rat at a chosen location
			if (rc.canBuildRat(loc))
			{
				rc.buildRat(loc);
				break;
			}
		}
	}
}
