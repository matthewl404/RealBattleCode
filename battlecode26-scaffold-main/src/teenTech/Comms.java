package theVibers1;
 
import battlecode.common.*;
 
/**
 * Handles all shared-array communication between robots.
 *
 * Layout (64 slots, values 0-1023):
 *   [0-1]   Rat king location:  x+1, y+1  (0 = unknown)
 *   [2-11]  Cat locations:      up to 5 cats, each stored as x+1, y+1
 *
 * Coordinates are stored as (coord + 1) so that 0 acts as a "null" ,
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
}
