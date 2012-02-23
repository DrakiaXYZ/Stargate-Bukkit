package net.TheDgtl.Stargate.event;

import org.bukkit.event.HandlerList;

import net.TheDgtl.Stargate.Portal;

public class StargateCloseEvent extends StargateEvent {
	private boolean force;
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
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
