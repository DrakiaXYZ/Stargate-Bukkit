package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class StargateDestroyEvent extends StargateEvent {
	private static final long serialVersionUID = 1429032103662930225L;
	private Player player;
	private boolean deny;
	private String denyReason;
	private int cost;
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public StargateDestroyEvent(Portal portal, Player player, boolean deny, String denyMsg, int cost) {
		super("StargateDestroyEvent", portal);
		this.player = player;
		this.deny = deny;
		this.denyReason = denyMsg;
		this.cost = cost;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public boolean getDeny() {
		return deny;
	}
	
	public void setDeny(boolean deny) {
		this.deny = deny;
	}
	
	public String getDenyReason() {
		return denyReason;
	}
	
	public void setDenyReason(String denyReason) {
		this.denyReason = denyReason;
	}
	
	public int getCost() {
		return cost;
	}
	
	public void setCost(int cost) {
		this.cost = cost;
	}

}
