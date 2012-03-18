package net.TheDgtl.Stargate;

public class BloxPopulator {
	private Blox blox;
	private int nextMat;
	private byte nextData;
	
	public BloxPopulator(Blox b, int m) {
		blox = b;
		nextMat = m;
		nextData = 0;
	}
	
	public BloxPopulator(Blox b, int m, byte d) {
		blox = b;
		nextMat = m;
		nextData = d;
	}
	
	public void setBlox(Blox b) {
		blox = b;
	}
	
	public void setMat(int m) {
		nextMat = m;
	}
	
	public void setData(byte d) {
		nextData = d;
	}
	
	public Blox getBlox() {
		return blox;
	}
	
	public int getMat() {
		return nextMat;
	}
	
	public byte getData() {
		return nextData;
	}
	
}
