package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

public class StargateDeactivateEvent extends StargateEvent {
	private static final long serialVersionUID = -1863190375834892100L;

	public StargateDeactivateEvent(Portal portal) {
		super("StargatDeactivateEvent", portal);
		
	}
}
