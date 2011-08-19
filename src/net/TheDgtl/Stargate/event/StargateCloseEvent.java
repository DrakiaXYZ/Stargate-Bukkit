package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

public class StargateCloseEvent extends StargateEvent {
	private static final long serialVersionUID = -4382967941863636023L;
	private boolean force;

	public StargateCloseEvent(Portal portal, boolean force) {
		super("StargateCloseEvent", portal);
		
		this.force = force;
	}
	
	public boolean getForce() {
		return force;
	}
	
	public void setForce(boolean force) {
		this.force = force;
	}
}
