/*
package teenTech;

import battlecode.common.*;

public class BabyRat {
	private static MapLocation target;

	public static void run (RobotController rc)
	{
		while (true) {
			try {
				setTarget(rc);

				//go to target 
				Movement.moveTowards(rc, target);
			}
			catch(GameActionException e) {
				System.out.println(e);
			}
			Clock.yield();
		}
	}

	private static void setTarget(RobotController rc) {
		// if there is no destination, then set the destination to the opposite of where the robot currently is
		if (target == null)
		{
			int x = rc.getMapWidth() - rc.getLocation().x;
			int y = rc.getMapHeight() - rc.getLocation().y;
			target = new MapLocation(x,y);
		}	
	}
}
*/

package teenTech;
 
import battlecode.common.*;
 
/**
 * Baby rat AI.
 *
 * State machine:
 *   FORAGE  — cheese is visible; navigate to it and pick it up
 *   DELIVER — carrying raw cheese; navigate to the rat king and transfer it
 *   EXPLORE — nothing to do; head toward the opposite corner to uncover new cheese mines
 *
 * Every turn (regardless of state):
 *   1. Attack the best adjacent threat (cats always; enemy rats only after backstab)
 *   2. Pick up any cheese sitting in an adjacent tile
 *   3. Update state based on current cheese and vision
 *   4. Execute the current state's movement
 */
public class BabyRat {
	private static MapLocation target;
 
	public static void run1 (RobotController rc)
	{
		while (true) {
			try {
				setTarget(rc);
 
				//go to target 
				Movement.moveTowards(rc, target);
			}
			catch(GameActionException e) {
				System.out.println(e);
			}
			Clock.yield();
		}
	}
 
	private static void setTarget(RobotController rc) {
		// if there is no destination, then set the destination to the opposite of where the robot currently is
		if (target == null)
		{
			int x = rc.getMapWidth() - rc.getLocation().x;
			int y = rc.getMapWidth() - rc.getLocation().y;
			target = new MapLocation(x,y);
		}	
	}
 
    enum State { FORAGE, DELIVER, EXPLORE }
 
    // Per-robot state (static = own JVM copy, not shared)
    private static State      state         = State.EXPLORE;
    private static MapLocation cheeseTarget  = null;  // tile to forage
    private static MapLocation exploreTarget = null;  // long-range exploration goal
 
    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------
 
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
 
    // -----------------------------------------------------------------------
    // Per-turn logic
    // -----------------------------------------------------------------------
 
    private static void runTurn(RobotController rc) throws GameActionException {
        // 1. Attack: cats always; enemy rats only in back stab mode
        Combat.tryAttack(rc);
 
        // 2. Collect any cheese that is already adjacent (action may or may not cost cool down)
        pickUpAdjacentCheese(rc);
 
        // 3. Decide what to do this turn
        updateState(rc);
 
        // 4. Act on current state
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
            // Carrying cheese → deliver it
            state = State.DELIVER;
            return;
        }
 
        // Look for cheese in the vision cone
        MapLocation nearest = findNearestVisibleCheese(rc);
        if (nearest != null) {
            cheeseTarget = nearest;
            state = State.FORAGE;
        } else {
            state = State.EXPLORE;
        }
    }
 
    // -----------------------------------------------------------------------
    // State behaviours
    // -----------------------------------------------------------------------
 
    /** Move toward the nearest visible cheese tile. */
    private static void runForage(RobotController rc) throws GameActionException {
        if (cheeseTarget == null) {
            state = State.EXPLORE;
            return;
        }
        Movement.moveTowards(rc, cheeseTarget);
    }
 
    /**
     * Move toward the rat king and transfer all raw cheese once adjacent.
     * Falls back to EXPLORE if the king's location is not yet in the shared array.
     */
    private static void runDeliver(RobotController rc) throws GameActionException {
        MapLocation kingLoc = Comms.readKingLocation(rc);
        if (kingLoc == null) {
            // King hasn't written its location yet; explore until it does
            state = State.EXPLORE;
            return;
        }
 
        // Transfer cheese if we are close enough to the king
        int raw = rc.getRawCheese();
        if (raw > 0 && rc.canTransferCheese(kingLoc, raw)) {
            rc.transferCheese(kingLoc, raw);
            state = State.EXPLORE;
            return;
        }
 
        // Otherwise keep moving toward the king
        Movement.moveTowards(rc, kingLoc);
    }
 
    /**
     * Head toward the mirror of our current position (opposite corner of the map).
     * This spreads rats across the map to discover cheese mines.
     * Resets the target once we arrive so we bounce back and forth.
     */
    private static void runExplore(RobotController rc) throws GameActionException {
        if (exploreTarget == null || rc.getLocation().equals(exploreTarget)) {
            int x = rc.getMapWidth()  - 1 - rc.getLocation().x;
            int y = rc.getMapHeight() - 1 - rc.getLocation().y;
            exploreTarget = new MapLocation(x, y);
        }
        Movement.moveTowards(rc, exploreTarget);
    }
 
    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
 
    /**
     * Picks up cheese from any adjacent tile where cheese is present and we can reach it.
     * Baby rats may only collect cheese from adjacent locations, not from their own tile.
     */
    private static void pickUpAdjacentCheese(RobotController rc) throws GameActionException {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.CENTER) continue;
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canPickUpCheese(adj)) {
                rc.pickUpCheese(adj);
                return; // one pick-up per action slot
            }
        }
    }
 
    /**
     * Scans sensed map tiles and returns the closest one with cheese, or null.
     * Baby rats can only sense tiles within their 90° vision cone.
     */
    private static MapLocation findNearestVisibleCheese(RobotController rc)
            throws GameActionException {
        MapInfo[]  tiles    = rc.senseNearbyMapInfos();
        MapLocation me      = rc.getLocation();
        MapLocation best    = null;
        int         bestDist = Integer.MAX_VALUE;
 
        for (MapInfo tile : tiles) {
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