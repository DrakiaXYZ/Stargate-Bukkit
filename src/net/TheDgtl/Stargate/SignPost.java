package net.TheDgtl.Stargate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

/**
 * SignPost.java
 * @author Shaun (sturmeh)
 * @author Dinnerbone
 * @author Steven "Drakia" Scott
 */

public class SignPost {
	private Blox parent;
	private Blox block;
	private World world;
	
	public SignPost(World world, Sign sign) {
		this.world = world;
		this.block = new Blox(world, sign.getX(), sign.getY(), sign.getZ());
	}
	
	public SignPost(Blox block) {
		this.block = block;
		this.world = block.getWorld();
	}
	
	public Block getParent() {
		if (parent == null) findParent();
		if (parent == null) return null;
		return parent.getBlock();
	}
	
	public Block getBlock() {
		return block.getBlock();
	}
	
	public String getText(int index) {
		Sign sign = findSign();
		if (sign == null) return "";
		return sign.getLine(index);
	}
	
	public void setText(int index, String value) {
		Sign sign = findSign();
		if (sign == null) return;
		sign.setLine(index, value);
	}
	
	public String getIdText() {
		Sign sign = findSign();
		if (sign == null) return "";
		StringBuilder result = new StringBuilder();

		result.append(getText(0));
		result.append("\n");
		result.append(getText(1));
		result.append("\n");
		result.append(getText(2));
		result.append("\n");
		result.append(getText(3));
		
		return result.toString().toLowerCase();
	}
	
	public void update() {
		final Sign sign = findSign();
		if (sign == null) return;
		
		Stargate.server.getScheduler().scheduleSyncDelayedTask(Stargate.stargate, new Runnable() {
			public void run() {
	        	sign.update();
			}
		});
	}
	
	private void findParent() {
		Sign sign = findSign();
		int offsetX = 0;
		int offsetY = 0;
		int offsetZ = 0;
		
		if (block.getBlock().getType() == Material.WALL_SIGN) {
			if (block.getData() == 0x2) {
				offsetZ = 1;
			} else if (block.getData() == 0x3) {
				offsetZ = -1;
			} else if (block.getData() == 0x4) {
				offsetX = 1;
			} else if (block.getData() == 0x5) {
				offsetX = -1;
			}
		} else if (block.getBlock().getType() == Material.SIGN_POST) {
			offsetY = -1;
		}
		if (sign == null) {
			Stargate.debug("findParent", "sign == null");
			return;
		}
		if (world == null) {
			Stargate.debug("findParent", "world == null");
			return;
		}
		parent = new Blox(world, sign.getX() + offsetX, sign.getY() + offsetY, sign.getZ() + offsetZ);
	}
	
	private Sign findSign() {
		try {
			BlockState sign = this.world.getBlockAt(block.getX(), block.getY(), block.getZ()).getState();
			if (sign instanceof Sign) return (Sign)sign;
			return null;
		} catch (Exception e) {}
		return null;
	}
	
	public static SignPost getFromBlock(Block block) {
		BlockState state = block.getState();
		if (!(state instanceof Sign)) return null;
		return new SignPost(block.getWorld(), (Sign)state);
	}
	
	public static SignPost getFromLocation(Location location) {
		return getFromBlock(location.getWorld().getBlockAt((int)location.getX(), (int)location.getY(), (int)location.getZ()));
	}
}