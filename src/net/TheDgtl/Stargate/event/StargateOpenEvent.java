package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;

import org.bukkit.entity.Player;

public class StargateOpenEvent extends StargateEvent {
	private static final long serialVersionUID = -2804865767733660648L;
	Player player;
	
	public StargateOpenEvent(Player player, Portal portal, boolean force) {
		super ("StargateOpenEvent", portal, force);
		
		this.player = player;
		this.portal = portal;
	}
	
	/**
	 * Return the player than opened the gate.
	 * @return player than opened the gate
	 */
	public Player getPlayer() {
		return player;
	}
}
