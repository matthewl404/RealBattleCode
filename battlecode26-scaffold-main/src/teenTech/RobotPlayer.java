package teenTech;

import battlecode.common.*;

public class RobotPlayer {

	public static void run(RobotController rc)
	{
		if (rc.getType() == UnitType.RAT_KING) {
			RatKing.run(rc);
		}
		else
		{
			BabyRat.run(rc);
		}
	}
}
