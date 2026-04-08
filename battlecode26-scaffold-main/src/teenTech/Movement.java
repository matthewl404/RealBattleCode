package teenTech;

/*
package teenTech;


import battlecode.common.*;

public class Movement {

	private static MapLocation destination = null;
	private static boolean blocked = false;
	private static Direction desiredDir = Direction.CENTER;
	private static int distToDestWhenBlocked = 9999;
	
	public static void moveTowards(RobotController rc, MapLocation target) throws GameActionException
	{
		if(!rc.isMovementReady())
		{
			return;
		}
		
		if (target == null || rc.getLocation().equals(target))
		{
			return;
		}

		if (!target.equals(destination))
		{
			destination = target;
			blocked = false;
		}
		//check if blocked
		//if blocked, use "blocked" algorithm
		if (blocked) {
			blockedMovement(rc);
		}

		//if not blocked, use "unblocked" algorithm
		else {
			unblockedMovement(rc);
		}

	}

	private static void unblockedMovement(RobotController rc) throws GameActionException {
		//find the direction to destination
		desiredDir = rc.getLocation().directionTo(destination);

		//if can move that way
		if (rc.canMove(desiredDir)) {
			rc.move(desiredDir);
		}
			else {
				blocked = true;
				distToDestWhenBlocked = rc.getLocation().distanceSquaredTo(destination);
				
				//turn to the first open path on the left
				for (int turns = 0; turns < 7; turns += 1)
				{
					desiredDir = desiredDir.rotateLeft();
					if (rc.canMove(desiredDir)) {
						break;
					}
				}
				
				//use blocked algorithm
				blockedMovement(rc);
			}
		}


	private static void blockedMovement(RobotController rc) throws GameActionException {
		//check if we're still blocked
		if (rc.getLocation().distanceSquaredTo(destination) < distToDestWhenBlocked) {
			blocked = false;
			unblockedMovement(rc);
			return;
		}
		
		//move forward if we can
		if (rc.canMove(desiredDir))
		{
			rc.move(desiredDir);
		}
		
		//check to the right to see if there is an open path
		for (int turns = 0; turns < 4; turns += 1) {
			if (rc.canMove(desiredDir.rotateRight())) {
				desiredDir = desiredDir.rotateRight();
			}
			else {
				break;
			}
			if (turns == 3)
			{
				blocked = false;
			}
		}
		//check to the left to see if we can't move forward
		for (int turns = 0; turns < 7; turns += 1)
		{
			desiredDir = desiredDir.rotateLeft();
			if (rc.canMove(desiredDir)) {
				break;
			}
		}
	}
}
*/
import battlecode.common.*;
 
/**
 * Handles robot movement using a turn-then-move approach with bug navigation.
 *
 * Strategy:
 *   - Each turn, first try to face the target direction (uses turning cooldown).
 *   - Then move: forward if facing the right way (cooldown 10), strafing otherwise (cooldown 18).
 *   - When blocked, use right-hand wall-following until we get closer than when we were blocked.
 *
 * Static fields here are per-robot (each robot gets its own JVM copy).
 */
public class Movement {
 
	private static MapLocation destination = null;
	private static boolean blocked = false;
	private static Direction desiredDir = Direction.CENTER;
	private static int distToDestWhenBlocked = 9999;
	
	public static void moveTowards(RobotController rc, MapLocation target) throws GameActionException
	{
		if(!rc.isMovementReady())
		{
			return;
		}
		
		if (target == null || rc.getLocation().equals(target))
		{
			return;
		}
 
		if (!target.equals(destination))
		{
			destination = target;
			blocked = false;
		}
		//check if blocked
		//if blocked, use "blocked" algorithm
		if (blocked) {
			blockedMovement(rc);
		}
 
		//if not blocked, use "unblocked" algorithm
		else {
			unblockedMovement(rc);
		}
 
	}
 
	private static void unblockedMovement(RobotController rc) throws GameActionException {
		//find the direction to destination
		desiredDir = rc.getLocation().directionTo(destination);
 
		//if can move that way
		if (rc.canMove(desiredDir)) {
			rc.move(desiredDir);
		}
			else {
				blocked = true;
				distToDestWhenBlocked = rc.getLocation().distanceSquaredTo(destination);
				
				//turn to the first open path on the left
				for (int turns = 0; turns < 7; turns += 1)
				{
					desiredDir = desiredDir.rotateLeft();
					if (rc.canMove(desiredDir)) {
						break;
					}
				}
				
				//use blocked algorithm
				blockedMovement(rc);
			}
		}
 
 
	private static void blockedMovement(RobotController rc) throws GameActionException {
		//check if we're still blocked
		if (rc.getLocation().distanceSquaredTo(destination) < distToDestWhenBlocked) {
			blocked = false;
			unblockedMovement(rc);
			return;
		}
		
		//move forward if we can
		if (rc.canMove(desiredDir))
		{
			rc.move(desiredDir);
		}
		
		//check to the right to see if there is an open path
		for (int turns = 0; turns < 4; turns += 1) {
			if (rc.canMove(desiredDir.rotateRight())) {
				desiredDir = desiredDir.rotateRight();
			}
			else {
				break;
			}
			if (turns == 3)
			{
				blocked = false;
			}
		}
		//check to the left to see if we can't move forward
		for (int turns = 0; turns < 7; turns += 1)
		{
			desiredDir = desiredDir.rotateLeft();
			if (rc.canMove(desiredDir)) {
				break;
			}
		}
	}
    // Bug-navigation state (per robot)
    private static MapLocation dest         = null;
    private static boolean     ifBlock             = false;
    private static Direction   wallFollowDir       = Direction.CENTER;
    private static int         distWhenBlocked     = Integer.MAX_VALUE;
 
    /**
     * Turns toward {@code target} and moves one step closer using bug navigation.
     * Safe to call every turn; returns immediately if cooldowns are not ready.
     */
    public static void moveTowards1(RobotController rc, MapLocation target)
            throws GameActionException {
 
        if (target == null || rc.getLocation().equals(target)) return;
 
        // Reset bug-nav state whenever the destination changes
        if (!target.equals(destination)) {
            destination     = target;
            blocked         = false;
            wallFollowDir   = Direction.CENTER;
            distWhenBlocked = Integer.MAX_VALUE;
        }
 
        // Turn toward the target while turning is available (independent cooldown)
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
 
    /**
     * Attempts to face {@code target}. Turning and moving have independent
     * cooldowns, so this can execute on the same turn as a move.
     */
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
 
    /**
     * Right-hand wall-following: prefer turning right along the wall,
     * keep going straight when possible, turn left as a last resort.
     * Reverts to direct navigation once we get closer than when we got stuck.
     */
    private static void blockedMove(RobotController rc) throws GameActionException {
        // Unblock as soon as we make real progress toward the destination
        if (rc.getLocation().distanceSquaredTo(destination) < distWhenBlocked) {
            blocked = false;
            unblockedMove(rc);
            return;
        }
 
        // Right-hand rule: if the tile to our right is open, turn right (wall corner)
        Direction rightDir = wallFollowDir.rotateRight();
        if (rc.canMove(rightDir)) {
            wallFollowDir = rightDir;
        }
 
        // Try to move in wallFollowDir; if still blocked, rotate left to find a gap
        if (rc.canMove(wallFollowDir)) {
            rc.move(wallFollowDir);
            return;
        }
 
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
