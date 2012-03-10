package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Stargate - A portal plugin for Bukkit
 * Copyright (C) 2011, 2012 Steven "Drakia" Scott <Contact@TheDgtl.net>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class StargatePortalEvent extends StargateEvent {
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
