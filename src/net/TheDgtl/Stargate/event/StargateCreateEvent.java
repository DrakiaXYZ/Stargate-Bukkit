package net.TheDgtl.Stargate.event;

import net.TheDgtl.Stargate.Portal;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class StargateCreateEvent extends StargateEvent {
	private Player player;
	private boolean deny;
	private String denyReason;
	private String[] lines;
	private int cost;
	
	private static final HandlerList handlers = new HandlerList();
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public StargateCreateEvent(Player player, Portal portal, String[] lines, boolean deny, String denyReason, int cost) {
		super("StargateCreateEvent", portal);
		this.player = player;
		this.lines = lines;
		this.deny = deny;
		this.denyReason = denyReason;
		this.cost = cost;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public String getLine(int index) throws IndexOutOfBoundsException {
		return lines[index];
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
