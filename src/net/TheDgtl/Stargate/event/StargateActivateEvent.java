package net.TheDgtl.Stargate.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import net.TheDgtl.Stargate.Portal;

public class StargateActivateEvent extends StargateEvent {
	private static final long serialVersionUID = -8058490029263773684L;
	
	Player player;
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	public StargateActivateEvent(Portal portal, Player player) {
		super("StargatActivateEvent", portal);
		
		this.player = player;
	}
	
	public Player getPlayer() {
		return player;
	}
}
