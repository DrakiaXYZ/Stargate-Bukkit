package net.TheDgtl.Stargate;

/**
 * RelativeBlockVector.java
 * @author Shaun (sturmeh)
 * @author Dinnerbone
 * @author Steven "Drakia" Scott
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
