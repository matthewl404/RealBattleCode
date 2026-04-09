package teenTech;
 
import battlecode.common.*;
 
/**
 * Handles all shared-array communication between robots.
 *
 * Layout (64 slots, values 0-1023):
 *   [0-1]   Rat king location:  x+1, y+1  (0 = unknown)
 *   [2-11]  Cat locations:      up to 5 cats, each stored as x+1, y+1
 *
 * Coordinates are stored as (coord + 1) so that 0 acts as a "no data" sentinel,
 * since valid map coordinates are 0–59.
 *
 * Only rat kings may write; any robot may read.
 */
public class Comms {
 
    // Shared array indices
    static final int IDX_KING_X      = 0;
    static final int IDX_KING_Y      = 1;
    static final int IDX_CATS_START  = 2;
    static final int MAX_CATS        = 5;   // 5 cats × 2 slots = indices 2-11
 
    // -----------------------------------------------------------------------
    // Encoding helpers
    // -----------------------------------------------------------------------
 
    /** Encode a coordinate for storage (0 = no data, so offset by 1). */
    private static int encode(int coord) { return coord + 1; }
 
    /** Decode a stored coordinate back to a map coordinate. */
    private static int decode(int stored) { return stored - 1; }
 
    /** Returns true if the stored value represents a real coordinate. */
    private static boolean valid(int stored) { return stored != 0; }
 
    // -----------------------------------------------------------------------
    // Rat king location (write = rat king only, read = any)
    // -----------------------------------------------------------------------
 
    /** Rat king broadcasts its center location each turn. */
    static void writeKingLocation(RobotController rc) throws GameActionException {
        MapLocation loc = rc.getLocation();
        rc.writeSharedArray(IDX_KING_X, encode(loc.x));
        rc.writeSharedArray(IDX_KING_Y, encode(loc.y));
    }
 
    /**
     * Returns the last known rat king location, or null if not yet reported.
     * Baby rats use this to navigate toward the king for cheese delivery.
     */
    static MapLocation readKingLocation(RobotController rc) throws GameActionException {
        int sx = rc.readSharedArray(IDX_KING_X);
        int sy = rc.readSharedArray(IDX_KING_Y);
        if (!valid(sx) || !valid(sy)) return null;
        return new MapLocation(decode(sx), decode(sy));
    }
 
    // -----------------------------------------------------------------------
    // Cat locations (write = rat king only, read = any)
    // -----------------------------------------------------------------------
 
    /**
     * Rat king scans its 360° vision and writes up to MAX_CATS cat locations.
     * Unused slots are cleared to 0.
     */
    static void writeCatLocations(RobotController rc) throws GameActionException {
        RobotInfo[] nearby = rc.senseNearbyRobots(-1);
        int slot = 0;
        for (RobotInfo robot : nearby) {
            if (slot >= MAX_CATS) break;
            if (robot.type == UnitType.CAT) {
                int base = IDX_CATS_START + slot * 2;
                rc.writeSharedArray(base,     encode(robot.location.x));
                rc.writeSharedArray(base + 1, encode(robot.location.y));
                slot++;
            }
        }
        // Clear any remaining slots from a previous turn with more cats
        for (; slot < MAX_CATS; slot++) {
            int base = IDX_CATS_START + slot * 2;
            rc.writeSharedArray(base,     0);
            rc.writeSharedArray(base + 1, 0);
        }
    }
 
    /**
     * Returns all cat locations currently stored in the shared array.
     * The array may be shorter than MAX_CATS if fewer cats were seen.
     */
    static MapLocation[] readCatLocations(RobotController rc) throws GameActionException {
        MapLocation[] buf = new MapLocation[MAX_CATS];
        int count = 0;
        for (int i = 0; i < MAX_CATS; i++) {
            int base = IDX_CATS_START + i * 2;
            int sx = rc.readSharedArray(base);
            int sy = rc.readSharedArray(base + 1);
            if (valid(sx) && valid(sy)) {
                buf[count++] = new MapLocation(decode(sx), decode(sy));
            }
        }
        MapLocation[] result = new MapLocation[count];
        System.arraycopy(buf, 0, result, 0, count);
        return result;
    }
}
