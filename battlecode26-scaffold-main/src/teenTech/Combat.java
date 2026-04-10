package theVibers1;

import battlecode.common.*;

/**
 * Shared attack and terrain logic for both BabyRat and RatKing.
 *
 * Attack priority:
 *   1. Cats        
 *   2. Enemy kings 
 *   3. Enemy rats  
 *
 * During cooperation mode only cats are attacked
 */
public class Combat {

    /**
     * Finds the best adjacent attackable target and bites it.
     * Returns true if an attack was performed.
     */
    static boolean tryAttack(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return false;

        Team myTeam      = rc.getTeam();
        RobotInfo best   = null;
        int bestPriority = -1;

        for (RobotInfo robot : rc.senseNearbyRobots(-1)) {
            if (!rc.canAttack(robot.location)) continue;

            int priority = priorityOf(robot, myTeam);
            if (priority > bestPriority) {
                bestPriority = priority;
                best = robot;
            }
        }

        if (best != null) {
        	if (rc.getRawCheese() > 5) {
        		rc.attack(best.location);
        	} 
            return true;
        }
        return false;
    }

    /**
     * Digs the closest dirt tile within range.
     * Returns true if dirt was removed.
     */
    static boolean tryDigDirt(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return false;

        MapLocation me        = rc.getLocation();
        MapLocation bestDirt  = null;
        int         bestDist  = 6767;

        for (MapInfo tile : rc.senseNearbyMapInfos()) {
        	MapLocation loc = tile.getMapLocation();
        	
            if (!tile.isDirt()) continue;
            if (!rc.canRemoveDirt(loc)) continue;
            
            int dist = me.distanceSquaredTo(loc);
            if (dist < bestDist) {
                bestDist = dist;
                bestDirt = loc;
            }
        }

        if (bestDirt != null) {
            rc.removeDirt(bestDirt);
            return true;
        }
        return false;
    }

    /**
     * Returns the attack priority for a given robot (higher = more urgent).
     * Returns -1 if this robot should not be attacked.
     */
    private static int priorityOf(RobotInfo robot, Team myTeam) {
        if (robot.type == UnitType.CAT) {
            return 3; // always attack cats
        }
        if (robot.team != myTeam) {
            return robot.type == UnitType.RAT_KING ? 2 : 1;
        }
        return -1; // allied robot
    }
}
