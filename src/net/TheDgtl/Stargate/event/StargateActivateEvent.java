package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

public class StargateActivateEvent extends StargateEvent {
	private static final long serialVersionUID = -8058490029263773684L;

	public StargateActivateEvent(Portal portal) {
		super("StargatActivateEvent", portal);
		
	}
}
