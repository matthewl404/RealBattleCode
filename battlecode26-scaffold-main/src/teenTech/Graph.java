package theVibers;

import battlecode.common.*;

/**
 * Graph-based passability map with BFS pathfinding.
 *
 * Each robot maintains its own learned map of tile passability, updated
 * incrementally from sensor data every turn. BFS over this map replaces
 * the reactive bug-navigation fallback with a planned path around known obstacles.
 *
 * Design notes:
 *   - Generation-based visited marking avoids clearing arrays between BFS calls.
 *   - Unsensed tiles are treated as passable (optimistic) to allow exploration
 *     into unknown territory.
 *   - BFS is capped at MAX_BFS_NODES to keep per-turn bytecode usage bounded.
 */
public class Graph {

    static boolean[] passable;
    static boolean[] sensed;
    static int       mapWidth;
    static int       mapHeight;
    static boolean   initialized = false;

    static int[] visitedGen;
    static int[] parentIdx;
    static int[] bfsQueue;
    static int   gen = 0;

    /** Maximum BFS nodes explored per call (bounds bytecode cost). */
    static final int MAX_BFS_NODES = 50;

    public static void init(RobotController rc) {
        if (initialized) return;
        mapWidth  = rc.getMapWidth();
        mapHeight = rc.getMapHeight();
        int size  = mapWidth * mapHeight;
        passable   = new boolean[size];
        sensed     = new boolean[size];
        visitedGen = new int[size];
        parentIdx  = new int[size];
        bfsQueue   = new int[size];
        initialized = true;
    }

    public static void update(MapInfo[] tiles) {
        for (MapInfo info : tiles) {
            MapLocation loc = info.getMapLocation();
            int i = loc.y * mapWidth + loc.x;
            sensed[i]   = true;
            passable[i] = info.isPassable();
        }
    }

    public static Direction findNextStep(MapLocation src, MapLocation dst) {
        if (!initialized || src.equals(dst)) return null;

        gen++;
        final int thisGen = gen;

        int srcIdx = src.y * mapWidth + src.x;
        int dstIdx = dst.y * mapWidth + dst.x;

        int head = 0, tail = 0;
        bfsQueue[tail++]   = srcIdx;
        visitedGen[srcIdx] = thisGen;
        parentIdx[srcIdx]  = srcIdx;

        int explored = 0;
        boolean reached = false;

        outer:
        while (head < tail && explored < MAX_BFS_NODES) {
            int cur = bfsQueue[head++];
            explored++;
            int cx = cur % mapWidth;
            int cy = cur / mapWidth;

            for (Direction dir : Direction.values()) {
                if (dir == Direction.CENTER) continue;
                int nx = cx + dir.dx;
                int ny = cy + dir.dy;
                if (nx < 0 || nx >= mapWidth || ny < 0 || ny >= mapHeight) continue;
                int nIdx = ny * mapWidth + nx;
                if (visitedGen[nIdx] == thisGen) continue;
                if (sensed[nIdx] && !passable[nIdx]) continue;
                visitedGen[nIdx] = thisGen;
                parentIdx[nIdx]  = cur;
                if (nIdx == dstIdx) {
                    reached = true;
                    break outer;
                }
                bfsQueue[tail++] = nIdx;
            }
        }

        if (!reached) return null;

        int step = dstIdx;
        while (parentIdx[step] != srcIdx) {
            step = parentIdx[step];
        }
        return src.directionTo(new MapLocation(step % mapWidth, step / mapWidth));
    }
}
