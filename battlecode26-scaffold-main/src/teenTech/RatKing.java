package theVibers1;

import battlecode.common.*;

/**
 *
 * Each turn (in priority order):
 *   1. Communicate — write king location and cat positions to the shared array
 *   2. Attack      — bite the highest-priority adjacent target
 *   3. Dig         — remove the closest dirt tile within build range to clear paths
 *   4. Spawn       — build a baby rat if we have enough cheese and are under the rat cap
 *
 */
public class RatKing {

    /** Cheese kept in reserve; spawning is deferred until we exceed this. */
    private static final int CHEESE_BUFFER = 50;
    /** Minimum cheese before we bother placing cat traps. */


    // -----------------------------------------------------------------------
    // actual function
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
    	//if(rc.canSenseNearbyRobots())
    	if(rc.getRoundNum() > 100) {
    		trySpawnDirt(rc);
    	}
        Combat.tryDigDirt(rc);          // 1. Dig closest dirt tile to clear paths
        
        if(rc.getRoundNum() < 30) {
        	Comms.writeKingLocation(rc);    // 2. Broadcast location
    	}
        if (rc.getCurrentRatCost() < 67) {
        	trySpawnRat(rc);                // 3. Spawn a baby rat if affordable and under cap
        }
        Combat.tryAttack(rc);           // 4. Attack best adjacent target
        
        rc.setIndicatorString("RatKing cheese=" + rc.getGlobalCheese()
                + " traps=" + rc.getNumberCatTraps());
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------



    /**
     * Spawns one baby rat at the first valid adjacent location.
     * Defers if we don't have enough cheese above the safety buffer,
     * or if we already have 20 rats on map
     */
    
    private static void trySpawnRat(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;
        if (rc.getCurrentRatCost() > 55) {
        	return;
        }

        MapLocation[] nearby = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 8);
        for (MapLocation loc : nearby) {
            if (rc.canBuildRat(loc) && rc.getAllCheese() > CHEESE_BUFFER) {
                rc.buildRat(loc);
                return;
            }
        }
    }
    private static void trySpawnDirt(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;

        MapLocation[] nearby = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 8);
        for (MapLocation loc : nearby) {
            if (rc.canPlaceDirt(loc) && rc.getDirt() > 0) {
                rc.placeDirt(loc);
                return;
            }
        }
    }
}
