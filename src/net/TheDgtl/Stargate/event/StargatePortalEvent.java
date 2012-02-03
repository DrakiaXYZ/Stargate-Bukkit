package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class StargatePortalEvent extends StargateEvent {
	private static final long serialVersionUID = -7263321536459960366L;
	private Player player;
	private Portal destination;
	private Location exit;
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public StargatePortalEvent(Player player, Portal portal, Portal dest, Location exit) {
		super ("StargatePortalEvent", portal);
		
		this.player = player;
		this.destination = dest;
		this.exit = exit;
	}
	
	/**
	 * Return the player that went through the gate.
	 * @return player that went through the gate
	 */
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * Return the destination gate
	 * @return destination gate
	 */
	public Portal getDestination() {
		return destination;
	}
	
	/**
	 * Return the location of the players exit point
	 * @return org.bukkit.Location Location of the exit point
	 */
	public Location getExit() {
		return exit;
	}
	
	/**
	 * Set the location of the players exit point
	 */
	public void setExit(Location loc) {
		this.exit = loc;
	}
}
