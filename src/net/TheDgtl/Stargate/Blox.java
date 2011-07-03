package net.TheDgtl.Stargate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Blox.java
 * @author Shaun (sturmeh)
 * @author Dinnerbone
 * @author Steven "Drakia" Scott
 */

public class Blox {
	private int x;
	private int y;
	private int z;
	private World world;

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