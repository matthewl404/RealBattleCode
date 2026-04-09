package theVibers;

import battlecode.common.*;

public class Movement {

    private static MapLocation destination    = null;
    private static boolean     blocked        = false;
    private static Direction   wallFollowDir  = Direction.CENTER;
    private static int         distWhenBlocked = Integer.MAX_VALUE;

    public static void moveTowards(RobotController rc, MapLocation target)
            throws GameActionException {

        if (target == null || rc.getLocation().equals(target)) return;

        // Use the independent turning cooldown to face the target (free action)
        turnToward(rc, target);

        if (!rc.isMovementReady()) return;

        // Reset bug-nav state when destination changes
        if (!target.equals(destination)) {
            destination     = target;
            blocked         = false;
            wallFollowDir   = Direction.CENTER;
            distWhenBlocked = Integer.MAX_VALUE;
        }

        // Try graph-based pathfinding first (uses the learned passability map)
        if (Graph.initialized) {
            Direction graphDir = Graph.findNextStep(rc.getLocation(), target);
            if (graphDir != null && rc.canMove(graphDir)) {
                rc.move(graphDir);
                blocked = false;
                return;
            }
        }

        // Fall back to bug navigation
        if (blocked) {
            blockedMove(rc);
        } else {
            unblockedMove(rc);
        }
    }

    private static void turnToward(RobotController rc, MapLocation target)
            throws GameActionException {
        if (!rc.isTurningReady()) return;
        Direction desired = rc.getLocation().directionTo(target);
        if (rc.getDirection() != desired && rc.canTurn(desired)) {
            rc.turn(desired);
        }
    }

    private static void unblockedMove(RobotController rc) throws GameActionException {
        Direction toward = rc.getLocation().directionTo(destination);

        if (rc.canMove(toward)) {
            rc.move(toward);
        } else {
            blocked         = true;
            distWhenBlocked = rc.getLocation().distanceSquaredTo(destination);

            // Find the first open direction starting from a left rotation
            wallFollowDir = toward;
            for (int i = 0; i < 8; i++) {
                wallFollowDir = wallFollowDir.rotateLeft();
                if (rc.canMove(wallFollowDir)) {
                    rc.move(wallFollowDir);
                    return;
                }
            }
            // Completely surrounded; wait for an opening next turn
        }
    }

    private static void blockedMove(RobotController rc) throws GameActionException {
        // Unblock as soon as we make real progress toward the destination
        if (rc.getLocation().distanceSquaredTo(destination) < distWhenBlocked) {
            blocked = false;
            unblockedMove(rc);
            return;
        }

        // Right-hand rule: hug the wall corner when the tile to our right opens up
        Direction rightDir = wallFollowDir.rotateRight();
        if (rc.canMove(rightDir)) {
            wallFollowDir = rightDir;
        }

        if (rc.canMove(wallFollowDir)) {
            rc.move(wallFollowDir);
            return;
        }

        // Rotate left to find any open direction
        for (int i = 0; i < 7; i++) {
            wallFollowDir = wallFollowDir.rotateLeft();
            if (rc.canMove(wallFollowDir)) {
                rc.move(wallFollowDir);
                return;
            }
        }
        // Completely surrounded; wait
    }
}
