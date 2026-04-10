package theVibers1;

import battlecode.common.*;

/**
 * State machine:
 *   FORAGE  — cheese is visible; navigate to it and pick it up
 *   DELIVER — carrying raw cheese; navigate to the rat king and transfer it
 *   EXPLORE — go look randomly
 *
 * Every turn
 *   1. Attack the best adjacent threat
 *   2. Place a cat trap adjacent to any visible cat
 *   3. Dig any adjacent dirt tile 
 *   4. Pick up any cheese sitting in an adjacent tile
 *   5. Update state and move  
 */
public class BabyRat {

    enum State { FORAGE, DELIVER, EXPLORE }

    private static State       state         = State.EXPLORE;
    private static MapLocation cheeseTarget  = null;
    private static MapLocation exploreTarget = null;
    private static int PlacedRatTraps		 = 0;
    
    public static void run(RobotController rc) {
        while (true) {
            try {
                runTurn(rc);
            } catch (GameActionException e) {
                System.out.println("BabyRat GAE: " + e);
            }
            Clock.yield();
        }
    }

    private static void runTurn(RobotController rc) throws GameActionException {
    	
        Combat.tryAttack(rc);           // 1. Attack threats
        tryPlaceCatTrapNearCat(rc);     // 2. Trap any visible cat
        Combat.tryDigDirt(rc);          // 3. Dig adjacent dirt
        pickUpAdjacentCheese(rc);       // 4. Collect nearby cheese
        updateState(rc);                // 5. Decide state
        switch (state) {
            case FORAGE:  runForage(rc);  break;
            case DELIVER: runDeliver(rc); break;
            case EXPLORE: runExplore(rc); break;
        }
        rc.setIndicatorString(state + " raw=" + rc.getRawCheese());
    }

    // -----------------------------------------------------------------------
    // State transitions
    // -----------------------------------------------------------------------

    private static void updateState(RobotController rc) throws GameActionException {
    	
        if (rc.getRawCheese() > 0) {
            state = State.DELIVER;
            return;
        }
        
        MapLocation nearest = findNearestVisibleCheese(rc);
        
        
        if (nearest != null) {
            cheeseTarget = nearest;
            state = State.FORAGE;
        } else {
            state = State.EXPLORE;
        }
        
    }

    // -----------------------------------------------------------------------
    // State behaviors
    // -----------------------------------------------------------------------

    private static void runForage(RobotController rc) throws GameActionException {
        if (cheeseTarget == null) { state = State.EXPLORE; return; }
        Movement.moveTowards(rc, cheeseTarget);
    }

    private static void runDeliver(RobotController rc) throws GameActionException {
        MapLocation kingLoc = Comms.readKingLocation(rc);
        if (kingLoc == null) { state = State.EXPLORE; return; }
        
        int raw = rc.getRawCheese();
        if (raw > 0 && rc.canTransferCheese(kingLoc, raw)) {
            rc.transferCheese(kingLoc, raw);
            state = State.EXPLORE;
            return;
        }
        Movement.moveTowards(rc, kingLoc);
    }

    /**
     * Even-ID rats head to the opposite corner of the map.
     * Odd-ID rats head to one of the four corners based on their ID,
     * spreading the team across the whole map.
     */
    private static void runExplore(RobotController rc) throws GameActionException {
        if (exploreTarget == null || rc.getLocation().equals(exploreTarget)) {
            int w = rc.getMapWidth();
            int h = rc.getMapHeight();
            if (rc.getID() % 2 == 0) {
                // Opposite corner
                exploreTarget = new MapLocation(w - 1 - rc.getLocation().x,
                                                h - 1 - rc.getLocation().y);
            } else {
                // One of the four corners, chosen by ID
                switch (rc.getID() % 4) {
                    case 1:  exploreTarget = new MapLocation(0,     0    ); break;
                    case 3:  exploreTarget = new MapLocation(w - 1, 0    ); break;
                    default: exploreTarget = new MapLocation(0,     h - 1); break;
                }
            }
        }
        int raw = rc.getRawCheese();
        MapLocation kingLoc = Comms.readKingLocation(rc);
        if (raw > 0 && rc.canTransferCheese(kingLoc, raw)) {
            rc.transferCheese(kingLoc, raw);
            state = State.EXPLORE;
            return;
        }
        Movement.moveTowards(rc, exploreTarget);
        return;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * If a cat is visible, places a cat trap on the first valid tile adjacent to it.
     * Uses action cooldown, so only one trap per action slot.
     */
    private static void tryPlaceCatTrapNearCat(RobotController rc) throws GameActionException {
    	if (!rc.isActionReady()) return;

    	for (RobotInfo robot : rc.senseNearbyRobots(-1)) {
    		if (robot.type != UnitType.CAT) {
    			continue;
    		} else  {
    			for (Direction dir : Direction.values()) {
    				MapLocation adj = robot.location.add(dir);
    				if (rc.canPlaceCatTrap(adj)) {
    					rc.placeCatTrap(adj);
    					return;
    				} 
    			}
    		}
    	}
    }

    private static void pickUpAdjacentCheese(RobotController rc) throws GameActionException {
        for (Direction dir : Direction.values()) {
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canPickUpCheese(adj)) {
                rc.pickUpCheese(adj);
                if(PlacedRatTraps < 1 && rc.canPlaceRatTrap(adj)) {
                	rc.placeRatTrap(adj);
                	PlacedRatTraps+=1;
                } else {
                	return;
                }
                return;
            }
        }
    }

    private static MapLocation findNearestVisibleCheese(RobotController rc)
            throws GameActionException {
        MapLocation me      = rc.getLocation();
        MapLocation best    = null;
        int         bestDist = 6767;
        for (MapInfo tile : rc.senseNearbyMapInfos()) {
            if (tile.getCheeseAmount() > 0) {
                int dist = me.distanceSquaredTo(tile.getMapLocation());
                if (dist < bestDist) {
                    bestDist = dist;
                    best     = tile.getMapLocation();
                }
            }
        }
        return best;
    }
}
