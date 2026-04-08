package teenTech;

/*

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
*/

import battlecode.common.*;
 
/**
 * Rat king AI.
 *
 * Each turn (in priority order):
 *   1. Communicate — write king location and visible cat positions to the shared array
 *   2. Attack      — bite the highest-priority adjacent target
 *   3. Trap        — place a cat trap nearby during cooperation (up to the 10-trap cap)
 *   4. Spawn       — build a baby rat if we have enough cheese above our safety buffer
 *
 * Cheese budget:
 *   We keep CHEESE_BUFFER in reserve to cover rat-king upkeep (2/round) and future traps.
 *   Spawning only happens when global cheese exceeds (spawn cost + buffer).
 */
public class RatKing {
	public static void run1(RobotController rc)
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
 
    /** Cheese kept in reserve; spawning is deferred until we exceed this. */
    private static final int CHEESE_BUFFER = 200;
 
    /** Minimum cheese before we bother placing cat traps. */
    private static final int TRAP_THRESHOLD = 60;
 
    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------
 
    public static void run(RobotController rc) {
        while (true) {
            try {
                runTurn(rc);
            } catch (GameActionException e) {
                System.out.println("RatKing GAE: " + e);
            }
            Clock.yield();
        }
    }
 
    // -----------------------------------------------------------------------
    // Per-turn logic
    // -----------------------------------------------------------------------
 
    private static void runTurn(RobotController rc) throws GameActionException {
        // 1. Always broadcast location + cat sightings (no cooldown cost)
        Comms.writeKingLocation(rc);
        Comms.writeCatLocations(rc);
 
        // 2. Attack the best adjacent target (uses action cooldown)
        Combat.tryAttack(rc);
 
        // 3. Place a cat trap if we're in cooperation, within trap cap, and can afford it
        if (rc.isCooperation()) {
            tryPlaceCatTrap(rc);
        }
 
        // 4. Spawn a baby rat if affordable
        trySpawnRat(rc);
 
        rc.setIndicatorString("RatKing cheese=" + rc.getGlobalCheese()
                + " traps=" + rc.getNumberCatTraps());
    }
 
    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------
 
    /**
     * Places one cat trap in the nearest valid adjacent tile.
     * Cat traps deal 100 damage to cats and cost only 10 cheese — excellent value.
     * Limited to 10 per team; we only place in cooperation mode.
     */
    private static void tryPlaceCatTrap(RobotController rc) throws GameActionException {
        if (!rc.isActionReady())                   return;
        if (rc.getGlobalCheese() < TRAP_THRESHOLD) return;
        if (rc.getNumberCatTraps() >= 10)          return; // engine cap
 
        // Prefer tiles farther from the king so cats walk into them before reaching us
        MapLocation[] nearby = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 8);
        for (MapLocation loc : nearby) {
            if (rc.canPlaceCatTrap(loc)) {
                rc.placeCatTrap(loc);
                return;
            }
        }
    }
 
    /**
     * Spawns one baby rat at the first valid adjacent location.
     * Defers if we don't have enough cheese above the safety buffer.
     */
    private static void trySpawnRat(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;
 
        int cost   = rc.getCurrentRatCost();
        int cheese = rc.getGlobalCheese();
        if (cheese < cost + CHEESE_BUFFER) return;
 
        MapLocation[] nearby = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 8);
        for (MapLocation loc : nearby) {
            if (rc.canBuildRat(loc)) {
                rc.buildRat(loc);
                return;
            }
        }
    }
}
