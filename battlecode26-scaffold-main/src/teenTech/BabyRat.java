package theVibers;

import battlecode.common.*;

/**
 * Baby rat AI.
 *
 * State machine:
 *   FORAGE  — cheese is visible; navigate to it and pick it up
 *   DELIVER — carrying raw cheese; navigate to the rat king and transfer it
 *   EXPLORE — nothing to do; explore unvisited map chunks or route to a known mine
 *
 * Features implemented:
 *   F1  — mine discovery: sensed mines are written to the shared array
 *   F7  — dig escape: if forward 3 directions blocked, dig toward goal
 *   F10 — dirt clearing: dig dirt on direct path when cheese budget allows
 *   F12 — chunk exploration: 4x4 chunks tracked per robot
 */
public class BabyRat {

    enum State { FORAGE, DELIVER, EXPLORE }

    private static State       state         = State.EXPLORE;
    private static MapLocation cheeseTarget  = null;
    private static MapLocation exploreTarget = null;

    // F12 — chunk tracking
    private static final int CHUNK_SIZE = 4;
    private static boolean[] visitedChunks = null;
    private static int chunksWide;
    private static int chunksTall;

    private static void initChunks(RobotController rc) {
        if (visitedChunks != null) return;
        chunksWide    = (rc.getMapWidth()  + CHUNK_SIZE - 1) / CHUNK_SIZE;
        chunksTall    = (rc.getMapHeight() + CHUNK_SIZE - 1) / CHUNK_SIZE;
        visitedChunks = new boolean[chunksWide * chunksTall];
    }

    private static void markCurrentChunk(RobotController rc) {
        MapLocation loc = rc.getLocation();
        visitedChunks[(loc.y / CHUNK_SIZE) * chunksWide + (loc.x / CHUNK_SIZE)] = true;
    }

    private static MapLocation findExploreTarget(RobotController rc) {
        int total  = chunksWide * chunksTall;
        int offset = Math.abs(rc.getID() ^ rc.getRoundNum()) % total;
        for (int i = 0; i < total; i++) {
            int idx = (offset + i) % total;
            if (!visitedChunks[idx]) {
                int cx = idx % chunksWide;
                int cy = idx / chunksWide;
                int tx = Math.min(cx * CHUNK_SIZE + CHUNK_SIZE / 2, rc.getMapWidth()  - 1);
                int ty = Math.min(cy * CHUNK_SIZE + CHUNK_SIZE / 2, rc.getMapHeight() - 1);
                return new MapLocation(tx, ty);
            }
        }
        // All chunks visited — reset and start over
        for (int i = 0; i < total; i++) visitedChunks[i] = false;
        int cx = offset % chunksWide;
        int cy = offset / chunksWide;
        int tx = Math.min(cx * CHUNK_SIZE + CHUNK_SIZE / 2, rc.getMapWidth()  - 1);
        int ty = Math.min(cy * CHUNK_SIZE + CHUNK_SIZE / 2, rc.getMapHeight() - 1);
        return new MapLocation(tx, ty);
    }

    // Entry point
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

    private static void runTurn(RobotController rc) throws GameActionException {
        // 0. Bookkeeping — reuse sensed tiles across graph, chunk, and mine steps
        Graph.init(rc);
        MapInfo[] sensed = rc.senseNearbyMapInfos();
        Graph.update(sensed);
        initChunks(rc);
        markCurrentChunk(rc);
        scanAndReportMines(rc, sensed);  // F1

        // 1. Attack
        Combat.tryAttack(rc);

        // 2. Collect adjacent cheese
        pickUpAdjacentCheese(rc);

        // 3. Decide state
        updateState(rc, sensed);

        // 4. Act
        switch (state) {
            case FORAGE:  runForage(rc);  break;
            case DELIVER: runDeliver(rc); break;
            case EXPLORE: runExplore(rc); break;
        }

        rc.setIndicatorString(state + " raw=" + rc.getRawCheese());
    }

    private static void updateState(RobotController rc, MapInfo[] sensed) throws GameActionException {
        if (rc.getRawCheese() > 0) {
            state = State.DELIVER;
            return;
        }
        MapLocation nearest = findNearestVisibleCheese(rc, sensed);
        if (nearest != null) {
            cheeseTarget = nearest;
            state = State.FORAGE;
        } else {
            state = State.EXPLORE;
        }
    }

    private static void runForage(RobotController rc) throws GameActionException {
        if (cheeseTarget == null) { state = State.EXPLORE; return; }
        tryDigIfNeeded(rc, cheeseTarget);
        Movement.moveTowards(rc, cheeseTarget);
    }

    private static void runDeliver(RobotController rc) throws GameActionException {
        MapLocation kingLoc = Comms.readKingLocation(rc);
        if (kingLoc == null) { state = State.EXPLORE; return; }

        int raw = rc.getRawCheese();
        if (raw > 0 && rc.canTransferCheese(kingLoc, raw)) {
            rc.transferCheese(kingLoc, raw);
            state = State.EXPLORE;
            return;
        }
        tryDigIfNeeded(rc, kingLoc);
        Movement.moveTowards(rc, kingLoc);
    }

    private static void runExplore(RobotController rc) throws GameActionException {
        // Route to nearest known mine first (F1)
        MapLocation[] mines = Comms.readMineLocations(rc);
        if (mines.length > 0) {
            MapLocation me      = rc.getLocation();
            MapLocation nearest = mines[0];
            int bestDist        = me.distanceSquaredTo(mines[0]);
            for (int i = 1; i < mines.length; i++) {
                int d = me.distanceSquaredTo(mines[i]);
                if (d < bestDist) { bestDist = d; nearest = mines[i]; }
            }
            exploreTarget = nearest;
            tryDigIfNeeded(rc, exploreTarget);
            Movement.moveTowards(rc, exploreTarget);
            return;
        }

        // Chunk-based exploration (F12)
        if (exploreTarget == null || rc.getLocation().equals(exploreTarget)) {
            exploreTarget = findExploreTarget(rc);
        }
        tryDigIfNeeded(rc, exploreTarget);
        Movement.moveTowards(rc, exploreTarget);
    }

    // F7 + F10 — dig helper
    private static void tryDigIfNeeded(RobotController rc, MapLocation goal)
            throws GameActionException {
        if (!rc.isActionReady() || goal == null) return;

        // F7: if all 3 forward directions blocked, dig toward goal
        Direction toGoal = rc.getLocation().directionTo(goal);
        if (toGoal != Direction.CENTER) {
            boolean fwd   = rc.canMove(toGoal);
            boolean left  = rc.canMove(toGoal.rotateLeft());
            boolean right = rc.canMove(toGoal.rotateRight());
            if (!fwd && !left && !right) {
                MapLocation ahead = rc.adjacentLocation(toGoal);
                if (rc.canRemoveDirt(ahead)) { rc.removeDirt(ahead); return; }
                if (rc.canRemoveDirt(rc.adjacentLocation(toGoal.rotateLeft())))  { rc.removeDirt(rc.adjacentLocation(toGoal.rotateLeft()));  return; }
                if (rc.canRemoveDirt(rc.adjacentLocation(toGoal.rotateRight()))) { rc.removeDirt(rc.adjacentLocation(toGoal.rotateRight())); return; }
            }
        }

        // F10: proactively clear dirt on direct path if cheese budget allows
        if (rc.getGlobalCheese() <= 300 && rc.getRawCheese() <= 40) return;
        if (toGoal == Direction.CENTER) return;
        if (rc.canMove(toGoal)) return;
        MapLocation ahead = rc.adjacentLocation(toGoal);
        if (rc.canRemoveDirt(ahead)) rc.removeDirt(ahead);
    }

    // F1 — mine discovery
    private static void scanAndReportMines(RobotController rc, MapInfo[] tiles)
            throws GameActionException {
        for (MapInfo tile : tiles) {
            if (tile.hasCheeseMine()) {
                Comms.reportMine(rc, tile.getMapLocation());
                return;
            }
        }
    }

    private static void pickUpAdjacentCheese(RobotController rc) throws GameActionException {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.CENTER) continue;
            MapLocation adj = rc.adjacentLocation(dir);
            if (rc.canPickUpCheese(adj)) {
                rc.pickUpCheese(adj);
                return;
            }
        }
    }

    private static MapLocation findNearestVisibleCheese(RobotController rc, MapInfo[] tiles)
            throws GameActionException {
        MapLocation me      = rc.getLocation();
        MapLocation best    = null;
        int         bestDist = Integer.MAX_VALUE;
        for (MapInfo tile : tiles) {
            if (tile.getCheeseAmount() > 0) {
                int dist = me.distanceSquaredTo(tile.getMapLocation());
                if (dist < bestDist) { bestDist = dist; best = tile.getMapLocation(); }
            }
        }
        return best;
    }
}
