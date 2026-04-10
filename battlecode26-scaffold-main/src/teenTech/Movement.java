package theVibers1;

import battlecode.common.*;

/**
 * Handles robot movement using a turn-then-move approach with bug navigation.
 *
 * Strategy:
 *   - Each turn, first try to face the target direction (uses turning cooldown).
 *   - Then move: forward if facing the right way, strafing otherwise .
 *   - When blocked, use right-hand wall-following until we get closer than when we were blocked.
 *
 * Static fields here are per-robot (each robot gets its own JVM copy).
 */
public class Movement {

    //navigation state
    private static MapLocation destination     = null;
    private static boolean     blocked         = false;
    private static Direction   direction   = Direction.CENTER;
    private static int         distWhenBlocked = 6767;
    
    public static void moveTowards(RobotController rc, MapLocation target)
            throws GameActionException {

        if (target == null || rc.getLocation().equals(target)) return;

        // Reset nav state whenever the destination changes
        if (!target.equals(destination)) {
            destination     = target;
            blocked         = false;
            direction   = Direction.CENTER;
            distWhenBlocked = 6767;
        }

        // Turn toward the target while turning is available 
        turnToward(rc, target);

        // Move if movement cooldown is ready
        if (!rc.isMovementReady()) return;

        if (blocked) {
            blockedMove(rc);
        } else {
            unblockedMove(rc);
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static void turnToward(RobotController rc, MapLocation target)
            throws GameActionException {
        if (!rc.isTurningReady()) return;
        Direction desired = rc.getLocation().directionTo(target);
        if (rc.getDirection() != desired && rc.canTurn(desired)) {
            rc.turn(desired);
        }
    }

    /**
     * Moves directly toward the destination.
     * Switches to blocked mode when the direct path is obstructed.
     */
    private static void unblockedMove(RobotController rc) throws GameActionException {
        Direction toward = rc.getLocation().directionTo(destination);

        if (rc.canMove(toward)) {
            rc.move(toward);
        } else {
            // Path is blocked — start wall-following
            blocked         = true;
            distWhenBlocked = rc.getLocation().distanceSquaredTo(destination);

            // Pick the first open direction starting from a left-turn of toward
            direction = toward;
            for (int i = 0; i < 8; i++) {
                direction = direction.rotateLeft();
                if (rc.canMove(direction)) {
                    rc.move(direction);
                    return;
                }
            }
            // Completely surrounded; wait for an opening next turn
        }
    }

    /**
     * Right-hand wall-following: prefer turning right along the wall,
     * keep going straight when possible, turn left as a last resort.
     * Reverts to direct navigation once we get closer than when we got stuck.
     */
    private static void blockedMove(RobotController rc) throws GameActionException {
        // Unblock as soon as we make real progress toward the destination
        if (rc.getLocation().distanceSquaredTo(destination) <= distWhenBlocked) {
            blocked = false;
            unblockedMove(rc);
            return;
        }

        // Right-hand rule: if the tile to our right is open, turn right (wall corner)
        Direction rightDir = direction.rotateRight();
        if (rc.canMove(rightDir)) {
            direction = rightDir;
        }

        // Try to move in wallFollowDir; if still blocked, rotate left to find a gap
        if (rc.canMove(direction)) {
            rc.move(direction);
            return;
        }

        for (int i = 0; i < 7; i++) {
            direction = direction.rotateLeft();
            if (rc.canMove(direction)) {
                rc.move(direction);
                return;
            }
        }
    }
}
