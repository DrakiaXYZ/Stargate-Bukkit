package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class StargateOpenEvent extends StargateEvent {
	private Player player;
	private boolean force;
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	public StargateOpenEvent(Player player, Portal portal, boolean force) {
		super ("StargateOpenEvent", portal);
		
		this.player = player;
		this.force = force;
	}
	
	/**
	 * Return the player than opened the gate.
	 * @return player than opened the gate
	 */
	public Player getPlayer() {
		return player;
	}
	
	public boolean getForce() {
		return force;
	}
	
	public void setForce(boolean force) {
		this.force = force;
	}
}
