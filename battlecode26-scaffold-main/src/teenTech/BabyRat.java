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
