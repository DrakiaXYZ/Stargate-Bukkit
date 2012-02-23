package net.TheDgtl.Stargate.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import net.TheDgtl.Stargate.Portal;

public class StargateAccessEvent extends StargateEvent {
	private Player player;
	private boolean deny;
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public StargateAccessEvent(Player player, Portal portal, boolean deny) {
		super("StargateAccessEvent", portal);
		
		this.player = player;
		this.deny = deny;
	}
	
	public boolean getDeny() {
		return this.deny;
	}
	
	public void setDeny(boolean deny) {
		this.deny = deny;
	}
	
	public Player getPlayer() {
		return this.player;
	}

}
