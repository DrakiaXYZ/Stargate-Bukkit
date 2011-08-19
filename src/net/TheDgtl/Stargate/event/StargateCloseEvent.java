package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

public class StargateCloseEvent extends StargateEvent {
	private static final long serialVersionUID = -4382967941863636023L;

	public StargateCloseEvent(Portal portal, boolean force) {
		super("StargateCloseEvent", portal, force);
	}
}
