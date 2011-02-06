package com.bukkit.TheDgtl.Stargate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

public class SignPost {
	private Block parent;
	private Sign sign;
	private Block block;
	private World world;
	
	public SignPost(World world, Sign sign) {
		this.world = world;
		this.sign = sign;
		block = world.getBlockAt(sign.getX(), sign.getY(), sign.getZ());
	}
	
	public SignPost(Blox block) {
		this.block = block.getBlock();
		this.world = block.getWorld();
		this.sign = (Sign)block.getBlock().getState();
	}
	
	public Block getParent() {
		if (parent == null) findParent();
		return parent;
	}
	
	public Block getBlock() {
		return block;
	}
	
	public String getText(int index) {
		if (sign == null) findSign();
		if (sign == null) return "";
		return sign.getLine(index);
	}
	
	public void setText(int index, String value) {
		if (sign == null) findSign();
		if (sign == null) return;
		sign.setLine(index, value);
	}
	
	public String getIdText() {
		if (sign == null) findSign();
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
		if (sign == null) findSign();
		if (sign == null) return;
		sign.update();
	}
	
	private void findParent() {
		int offsetX = 0;
		int offsetY = 0;
		int offsetZ = 0;
		
		if (block.getType() == Material.WALL_SIGN) {
			if (block.getData() == 0x2) {
				offsetZ = 1;
			} else if (block.getData() == 0x3) {
				offsetZ = -1;
			} else if (block.getData() == 0x4) {
				offsetX = 1;
			} else if (block.getData() == 0x5) {
				offsetX = -1;
			}
		} else if (block.getType() == Material.SIGN_POST) {
			offsetY = -1;
		}
		if (sign == null) {
			Stargate.log.info("Sign is null");
			return;
		}
		if (world == null) {
			Stargate.log.info("World is null");
		}
		parent = world.getBlockAt(sign.getX() + offsetX, sign.getY() + offsetY, sign.getZ() + offsetZ);		
	}
	
	private void findSign() {
		try {
			sign = (Sign)this.world.getBlockAt(block.getX(), block.getY(), block.getZ()).getState();
		} finally {
			
		}
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