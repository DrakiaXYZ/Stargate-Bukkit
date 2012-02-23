package net.TheDgtl.Stargate.event;

import org.bukkit.event.HandlerList;

import net.TheDgtl.Stargate.Portal;

public class StargateDeactivateEvent extends StargateEvent {
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	public StargateDeactivateEvent(Portal portal) {
		super("StargatDeactivateEvent", portal);
		
	}
}
