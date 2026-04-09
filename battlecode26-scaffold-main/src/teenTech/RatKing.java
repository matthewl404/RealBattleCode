package theVibers;

import battlecode.common.*;
 
/**
 * Rat king AI.
 *
 * Each turn (in priority order):
 *   1. Communicate — write king location and visible cat positions to the shared array
 *   2. Attack      — bite the highest-priority adjacent target
 *   3. Trap        — place a cat trap nearby during cooperation (up to the 10-trap cap)
 *   4. Spawn       — build a baby rat if we have enough cheese above our safety buffer
 *
 * Cheese budget:
 *   We keep CHEESE_BUFFER in reserve to cover rat-king upkeep (2/round) and future traps.
 *   Spawning only happens when global cheese exceeds (spawn cost + buffer).
 */
public class RatKing {
 
    /** Cheese kept in reserve; spawning is deferred until we exceed this. */
    private static final int CHEESE_BUFFER = 200;
 
    /** Minimum cheese before we bother placing cat traps. */
    private static final int TRAP_THRESHOLD = 60;

    /** F6: hard cap on baby rats to prevent runaway spawn-cost scaling. */
    private static final int MAX_BABY_RATS = 20;

    public static void run(RobotController rc) {
        while (true) {
            try {
                runTurn(rc);
            } catch (GameActionException e) {
                System.out.println("RatKing GAE: " + e);
            }
            Clock.yield();
        }
    }
 
    private static void runTurn(RobotController rc) throws GameActionException {
        // 0. Maintain the graph-based passability map
        Graph.init(rc);
        Graph.update(rc.senseNearbyMapInfos());

        // 1. Always broadcast location + cat sightings (no cooldown cost)
        Comms.writeKingLocation(rc);
        Comms.writeCatLocations(rc);
 
        // 2. Attack the best adjacent target (uses action cooldown)
        Combat.tryAttack(rc);
 
        // 3. Place a cat trap if we're in cooperation, within trap cap, and can afford it
        if (rc.isCooperation()) {
            tryPlaceCatTrap(rc);
        }
 
        // 4. Spawn a baby rat if affordable
        trySpawnRat(rc);
 
        rc.setIndicatorString("RatKing cheese=" + rc.getGlobalCheese()
                + " traps=" + rc.getNumberCatTraps());
    }
 
    private static void tryPlaceCatTrap(RobotController rc) throws GameActionException {
        if (!rc.isActionReady())                   return;
        if (rc.getGlobalCheese() < TRAP_THRESHOLD) return;
        if (rc.getNumberCatTraps() >= 10)          return;
 
        MapLocation[] nearby = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 8);
        for (MapLocation loc : nearby) {
            if (rc.canPlaceCatTrap(loc)) {
                rc.placeCatTrap(loc);
                return;
            }
        }
    }
 
    private static void trySpawnRat(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;

        // F6: count visible baby rats and enforce hard cap
        RobotInfo[] friendly = rc.senseNearbyRobots(-1, rc.getTeam());
        int babyRatCount = 0;
        for (RobotInfo r : friendly) if (r.type == UnitType.BABY_RAT) babyRatCount++;
        if (babyRatCount >= MAX_BABY_RATS) return;

        int cost   = rc.getCurrentRatCost();
        int cheese = rc.getGlobalCheese();
        if (cheese < cost + CHEESE_BUFFER) return;
 
        MapLocation[] nearby = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 8);
        for (MapLocation loc : nearby) {
            if (rc.canBuildRat(loc)) {
                rc.buildRat(loc);
                return;
            }
        }
    }
}
