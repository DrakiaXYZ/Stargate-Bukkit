package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

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
