package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

public class StargateEvent extends Event implements Cancellable {
	private static final long serialVersionUID = -5079274654178040431L;
	protected Portal portal;
	protected boolean cancelled;
	protected boolean force;
	
	public StargateEvent(String event, Portal portal, boolean force) {
		super (event);
		this.portal = portal;
		this.cancelled = false;
		this.force = force;
	}
	
	public Portal getPortal() {
		return portal;
	}
	
	public boolean getForce() {
		return force;
	}
	
	public void setForce(boolean force) {
		this.force = force;
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
