package net.TheDgtl.Stargate;

import net.minecraft.server.TileEntitySign;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockState;

public class StargateSign extends CraftBlockState implements Sign {
    private final TileEntitySign sign;
    private final String[] lines;

    public StargateSign(final Block block) {
        super(block);

        CraftWorld world = (CraftWorld) block.getWorld();
        sign = (TileEntitySign) world.getTileEntityAt(getX(), getY(), getZ());
        if (sign != null) {
        	lines = new String[sign.lines.length];
        	System.arraycopy(sign.lines, 0, lines, 0, lines.length);
        } else {
        	// Sadly, due to Minecraft having many issues with blocks, chunks
        	// and entities, we must assume a 4-line sign if the sign is null
        	lines = new String[4];
        }
    }

    public String[] getLines() {
        return lines;
    }

    public String getLine(int index) throws IndexOutOfBoundsException {
        return lines[index];
    }

    public void setLine(int index, String line) throws IndexOutOfBoundsException {
        lines[index] = line;
    }

    @Override
    public boolean update(boolean force) {
        boolean result = super.update(force) && (sign != null);

        if (result) {
            for(int i = 0; i < sign.lines.length; i++) {
                if(lines[i] != null) {
                    sign.lines[i] = lines[i];
                } else {
                    sign.lines[i] = "";
                }
            }
            sign.update();
        }

        return result;
    }
}
