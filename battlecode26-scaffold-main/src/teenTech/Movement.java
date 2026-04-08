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
