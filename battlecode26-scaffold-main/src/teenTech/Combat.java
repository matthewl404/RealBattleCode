package teenTech;
 
import battlecode.common.*;
 
/**
 * Shared attack logic for both BabyRat and RatKing.
 *
 * Attack priority:
 *   1. Cats         — always attack (deals damage toward shared cat-kill goal)
 *   2. Enemy kings  — high-value target in backstab mode
 *   3. Enemy rats   — standard target in backstab mode
 *
 * During cooperation mode, only cats are attacked (attacking enemy rats
 * would trigger a backstab).
 */
public class Combat {
 
    /**
     * Finds the best adjacent attackable target and bites it.
     * Returns true if an attack was performed.
     */
    static boolean tryAttack(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return false;
 
        Team myTeam       = rc.getTeam();
        boolean coop      = rc.isCooperation();
        RobotInfo best    = null;
        int bestPriority  = -1;
 
        for (RobotInfo robot : rc.senseNearbyRobots(-1)) {
            if (!rc.canAttack(robot.location)) continue;
 
            int priority = priorityOf(robot, myTeam, coop);
            if (priority > bestPriority) {
                bestPriority = priority;
                best = robot;
            }
        }
 
        if (best != null) {
            rc.attack(best.location);
            return true;
        }
        return false;
    }
 
    /**
     * Returns the attack priority for a given robot (higher = more urgent).
     * Returns -1 if this robot should not be attacked.
     */
    private static int priorityOf(RobotInfo robot, Team myTeam, boolean coop) {
        if (robot.type == UnitType.CAT) {
            return 3; // always attack cats
        }
        if (coop) {
            return -1; // never attack enemy rats during cooperation
        }
        // Backstab mode: attack enemies
        if (robot.team != myTeam) {
            return robot.type == UnitType.RAT_KING ? 2 : 1;
        }
        return -1; // allied robot
    }
}