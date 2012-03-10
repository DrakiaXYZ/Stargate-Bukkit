package net.TheDgtl.Stargate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Stargate - A portal plugin for Bukkit
 * Copyright (C) 2011 Shaun (sturmeh)
 * Copyright (C) 2011 Dinnerbone
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

public class Blox {
	private int x;
	private int y;
	private int z;
	private World world;
	private Blox parent = null;

	public Blox (World world, int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.world = world;
	}
	
	public Blox (Block block) {
		this.x = block.getX();
		this.y = block.getY();
		this.z = block.getZ();
		this.world = block.getWorld();
	}
	
	public Blox (Location location) {
		this.x = location.getBlockX();
		this.y = location.getBlockY();
		this.z = location.getBlockZ();
		this.world = location.getWorld();
	}
	
	public Blox (World world, String string) {
		String[] split = string.split(",");
		this.x = Integer.parseInt(split[0]);
		this.y = Integer.parseInt(split[1]);
		this.z = Integer.parseInt(split[2]);
		this.world = world;
	}
	
	public Blox makeRelative(int x, int y, int z) {
		return new Blox(this.world, this.x + x, this.y + y, this.z + z);
	}
	
	public Location makeRelativeLoc(double x, double y, double z, float rotX, float rotY) {
		return new Location(this.world, (double)this.x + x, (double)this.y + y, (double)this.z + z, rotX, rotY);
	}

	public Blox modRelative(int right, int depth, int distance, int modX, int modY, int modZ) {
		 return makeRelative(-right * modX + distance * modZ, -depth * modY, -right * modZ + -distance * modX);
	}

	public Location modRelativeLoc(double right, double depth, double distance, float rotX, float rotY, int modX, int modY, int modZ) {
		return makeRelativeLoc(0.5 + -right * modX + distance * modZ, depth, 0.5 + -right * modZ + -distance * modX, rotX, 0);
	}

	public void setType(int type) {
		world.getBlockAt(x, y, z).setTypeId(type);
	}

	public int getType() {
		return world.getBlockAt(x, y, z).getTypeId();
	}

	public void setData(int data) {
		world.getBlockAt(x, y, z).setData((byte)data);
	}

	public int getData() {
		return world.getBlockAt(x, y, z).getData();
	}

	public Block getBlock() {
		return world.getBlockAt(x, y, z);
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getZ() {
		return z;
	}
	
	public World getWorld() {
		return world;
	}
	
	public Block getParent() {
		if (parent == null) findParent();
		if (parent == null) return null;
		return parent.getBlock();
	}
	
	private void findParent() {
		int offsetX = 0;
		int offsetY = 0;
		int offsetZ = 0;
		
		if (getBlock().getType() == Material.WALL_SIGN) {
			if (getData() == 0x2) {
				offsetZ = 1;
			} else if (getData() == 0x3) {
				offsetZ = -1;
			} else if (getData() == 0x4) {
				offsetX = 1;
			} else if (getData() == 0x5) {
				offsetX = -1;
			}
		} else if (getBlock().getType() == Material.SIGN_POST) {
			offsetY = -1;
		} else {
			return;
		}
		parent = new Blox(world, getX() + offsetX, getY() + offsetY, getZ() + offsetZ);
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		//builder.append(world.getName());
		//builder.append(',');
		builder.append(x);
		builder.append(',');
		builder.append(y);
		builder.append(',');
		builder.append(z);
		return builder.toString();
	}
	
	@Override
	public int hashCode() {
		int result = 18;
		
		result = result * 27 + x;
		result = result * 27 + y;
		result = result * 27 + z;
		result = result * 27 + world.getName().hashCode();
		
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Blox blox = (Blox) obj;
		return (x == blox.x) && (y == blox.y) && (z == blox.z) && (world.getName().equals(blox.world.getName())); 
	}
}