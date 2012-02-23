package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class StargateEvent extends Event implements Cancellable {
	protected Portal portal;
	protected boolean cancelled;
	
	public StargateEvent(String event, Portal portal) {
		this.portal = portal;
		this.cancelled = false;
	}
	
	public Portal getPortal() {
		return portal;
	}
	
	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}
	
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
}
