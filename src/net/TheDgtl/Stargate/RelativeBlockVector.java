package net.TheDgtl.Stargate;

/**
 * RelativeBlockVector.java - Plug-in for hey0's minecraft mod.
 * @author Shaun (sturmeh)
 * @author Dinnerbone
 */
public class RelativeBlockVector {
    private int right = 0;
    private int depth = 0;
    private int distance = 0;

    public RelativeBlockVector(int right, int depth, int distance) {
        this.right = right;
        this.depth = depth;
        this.distance = distance;
    }

    public int getRight() {
        return right;
    }

    public int getDepth() {
        return depth;
    }

    public int getDistance() {
        return distance;
    }
}
