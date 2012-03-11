package net.TheDgtl.Stargate;

public class BloxPopulator {
	private Blox blox;
	private int nextMat;
	
	public BloxPopulator(Blox b, int m) {
		blox = b;
		nextMat = m;
	}
	
	public void setBlox(Blox b) {
		blox = b;
	}
	
	public void setMat(int m) {
		nextMat = m;
	}
	
	public Blox getBlox() {
		return blox;
	}
	
	public int getMat() {
		return nextMat;
	}
	
}
