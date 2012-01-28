package net.TheDgtl.Stargate.event;

import org.bukkit.entity.Player;

import net.TheDgtl.Stargate.Portal;

public class StargateAccessEvent extends StargateEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1009056668229801760L;
	private Player player;
	private boolean deny;
	
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
