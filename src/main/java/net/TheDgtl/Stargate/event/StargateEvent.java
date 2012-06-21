package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

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

public abstract class StargateEvent extends Event implements Cancellable {
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
