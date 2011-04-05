package net.TheDgtl.Stargate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.block.Block;

/**
 * Gate.java - Plug-in for hey0's minecraft mod.
 * @author Shaun (sturmeh)
 * @author Dinnerbone
 */
public class Gate {
	public static final int ANYTHING = -1;
	public static final int ENTRANCE = -2;
	public static final int CONTROL = -3;
	public static final int EXIT = -4;
	private static HashMap<String, Gate> gates = new HashMap<String, Gate>();
	private static HashMap<Integer, ArrayList<Gate>> controlBlocks = new HashMap<Integer, ArrayList<Gate>>();

	private String filename;
	private Integer[][] layout;
	private HashMap<Character, Integer> types;
	private RelativeBlockVector[] entrances = new RelativeBlockVector[0];
	private RelativeBlockVector[] border = new RelativeBlockVector[0];
	private RelativeBlockVector[] controls = new RelativeBlockVector[0];
	private RelativeBlockVector exitBlock = null;
	private HashMap<RelativeBlockVector, Integer> exits = new HashMap<RelativeBlockVector, Integer>();
	private int portalBlockOpen = Material.PORTAL.getId();
	private int portalBlockClosed = Material.AIR.getId();
	
	// iConomy information
	private int useCost = 0;
	private int createCost = 0;
	private int destroyCost = 0;

	private Gate(String filename, Integer[][] layout, HashMap<Character, Integer> types) {
		this.filename = filename;
		this.layout = layout;
		this.types = types;

		populateCoordinates();
	}

	private void populateCoordinates() {
		ArrayList<RelativeBlockVector> entranceList = new ArrayList<RelativeBlockVector>();
		ArrayList<RelativeBlockVector> borderList = new ArrayList<RelativeBlockVector>();
		ArrayList<RelativeBlockVector> controlList = new ArrayList<RelativeBlockVector>();
		RelativeBlockVector[] relativeExits = new RelativeBlockVector[layout[0].length];
		int[] exitDepths = new int[layout[0].length];
		//int bottom = 0;
		RelativeBlockVector lastExit = null;

		for (int y = 0; y < layout.length; y++) {
			for (int x = 0; x < layout[y].length; x++) {
				Integer id = layout[y][x];

				if (id == ENTRANCE || id == EXIT) {
					entranceList.add(new RelativeBlockVector(x, y, 0));
					exitDepths[x] = y;
					if (id == EXIT)
						this.exitBlock = new RelativeBlockVector(x, y, 0);
					//bottom = y;
				} else if (id == CONTROL) {
					controlList.add(new RelativeBlockVector(x, y, 0));
				} else if (id != ANYTHING) {
					borderList.add(new RelativeBlockVector(x, y, 0));
				}
			}
		}

		for (int x = 0; x < exitDepths.length; x++) {
			relativeExits[x] = new RelativeBlockVector(x, exitDepths[x], 0);
		}

		for (int x = relativeExits.length - 1; x >= 0; x--) {
			if (relativeExits[x] != null) {
				lastExit = relativeExits[x];
			} else {
				relativeExits[x] = lastExit;
			}

			if (exitDepths[x] > 0) this.exits.put(relativeExits[x], x);
		}

		this.entrances = entranceList.toArray(this.entrances);
		this.border = borderList.toArray(this.border);
		this.controls = controlList.toArray(this.controls);
	}
	
	public void save(String gateFolder) {
		HashMap<Integer, Character> reverse = new HashMap<Integer, Character>();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(gateFolder + filename));

			writeConfig(bw, "portal-open", portalBlockOpen);
			writeConfig(bw, "portal-closed", portalBlockClosed);
			if (useCost != iConomyHandler.useCost)
				writeConfig(bw, "usecost", useCost);
			if (createCost != iConomyHandler.createCost)
				writeConfig(bw, "createcost", createCost);
			if (destroyCost != iConomyHandler.destroyCost)
				writeConfig(bw, "destroycost", destroyCost);

			for (Character type : types.keySet()) {
				Integer value = types.get(type);

				if (!type.equals('-')) {
					reverse.put(value, type);
				}

				bw.append(type);
				bw.append('=');
				bw.append(value.toString());
				bw.newLine();
			}

			bw.newLine();

			for (int y = 0; y < layout.length; y++) {
				for (int x = 0; x < layout[y].length; x++) {
					Integer id = layout[y][x];
					Character symbol;

					if (id == ENTRANCE) {
						symbol = '.';
					} else if (id == ANYTHING) {
						symbol = ' ';
					} else if (id == CONTROL) {
						symbol = '-';
					} else if (id == EXIT) {
						symbol = '*';
					} else if (reverse.containsKey(id)) {
						symbol = reverse.get(id);
					} else {
						symbol = '?';
					}

					bw.append(symbol);
				}
				bw.newLine();
			}

			bw.close();
		} catch (IOException ex) {
			Stargate.log.log(Level.SEVERE, "Could not load Gate " + filename + " - " + ex.getMessage());
		}
	}

	private void writeConfig(BufferedWriter bw, String key, int value) throws IOException {
		bw.append(String.format("%s=%d", key, value));
		bw.newLine();
	}

	public Integer[][] getLayout() {
		return layout;
	}

	public RelativeBlockVector[] getEntrances() {
		return entrances;
	}

	public RelativeBlockVector[] getBorder() {
		return border;
	}

	public RelativeBlockVector[] getControls() {
		return controls;
	}

	public HashMap<RelativeBlockVector, Integer> getExits() {
		return exits;
	}
	public RelativeBlockVector getExit() {
		return exitBlock;
	}

	public int getControlBlock() {
		return types.get('-');
	}

	public String getFilename() {
		return filename;
	}

	public int getPortalBlockOpen() {
		return portalBlockOpen;
	}

	public int getPortalBlockClosed() {
		return portalBlockClosed;
	}
	
	public int getUseCost() {
		return useCost;
	}
	
	public Integer getCreateCost() {
		return createCost;
	}
	
	public Integer getDestroyCost() {
		return destroyCost;
	}

	public boolean matches(Block topleft, int modX, int modZ) {
		return matches(new Blox(topleft), modX, modZ);
	}

	public boolean matches(Blox topleft, int modX, int modZ) {
		for (int y = 0; y < layout.length; y++) {
			for (int x = 0; x < layout[y].length; x++) {
				int id = layout[y][x];

				if (id == ENTRANCE || id == EXIT) {
					if (topleft.modRelative(x, y, 0, modX, 1, modZ).getType() != 0) {
						return false;
					}
				} else if (id == CONTROL) {
					if (topleft.modRelative(x, y, 0, modX, 1, modZ).getType() != getControlBlock()) {
						return false;
					}
				} else if (id != ANYTHING) {
					 if (topleft.modRelative(x, y, 0, modX, 1, modZ).getType() != id) {
						 return false;
					 }
				}
			}
		}

		return true;
	}

	private static void registerGate(Gate gate) {
		gates.put(gate.getFilename(), gate);

		int blockID = gate.getControlBlock();

		if (!controlBlocks.containsKey(blockID)) {
			controlBlocks.put(blockID, new ArrayList<Gate>());
		}

		controlBlocks.get(blockID).add(gate);
	}

	private static Gate loadGate(File file) {
		Scanner scanner = null;
		boolean designing = false;
		ArrayList<ArrayList<Integer>> design = new ArrayList<ArrayList<Integer>>();
		HashMap<Character, Integer> types = new HashMap<Character, Integer>();
		HashMap<String, String> config = new HashMap<String, String>();
		int cols = 0;

		try {
			scanner = new Scanner(file);

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();

				if (designing) {
					ArrayList<Integer> row = new ArrayList<Integer>();

					if (line.length() > cols) {
						cols = line.length();
					}

					for (Character symbol : line.toCharArray()) {
						Integer id = ANYTHING;

						if (symbol.equals('.')) {
							id = ENTRANCE;
						} else if (symbol.equals('*')) {
							id = EXIT;
						} else if (symbol.equals(' ')) {
							id = ANYTHING;
						} else if (symbol.equals('-')) {
							id = CONTROL;
						} else if ((symbol.equals('?')) || (!types.containsKey(symbol))) {
							Stargate.log.log(Level.SEVERE, "Could not load Gate " + file.getName() + " - Unknown symbol '" + symbol + "' in diagram");
							return null;
						} else {
							id = types.get(symbol);
						}

						row.add(id);
					}

					design.add(row);
				} else {
					if ((line.isEmpty()) || (!line.contains("="))) {
						designing = true;
					} else {
						String[] split = line.split("=");
						String key = split[0].trim();
						String value = split[1].trim();

						if (key.length() == 1) {
							Character symbol = key.charAt(0);
							Integer id = Integer.parseInt(value);

							types.put(symbol, id);
						} else {
							config.put(key, value);
						}
					}
				}
			}
		} catch (Exception ex) {
			Stargate.log.log(Level.SEVERE, "Could not load Gate " + file.getName() + " - Invalid block ID given");
			return null;
		} finally {
			if (scanner != null) scanner.close();
		}

		Integer[][] layout = new Integer[design.size()][cols];

		for (int y = 0; y < design.size(); y++) {
			ArrayList<Integer> row = design.get(y);
			Integer[] result = new Integer[cols];

			for (int x = 0; x < cols; x++) {
				if (x < row.size()) {
					result[x] = row.get(x);
				} else {
					result[x] = ANYTHING;
				}
			}

			layout[y] = result;
		}

		Gate gate = new Gate(file.getName(), layout, types);

		gate.portalBlockOpen = readConfig(config, gate, file, "portal-open", gate.portalBlockOpen);
		gate.portalBlockClosed = readConfig(config, gate, file, "portal-closed", gate.portalBlockClosed);
		gate.useCost = readConfig(config, gate, file, "usecost", iConomyHandler.useCost);
		gate.destroyCost = readConfig(config, gate, file, "destroycost", iConomyHandler.destroyCost);
		gate.createCost = readConfig(config, gate, file, "createcost", iConomyHandler.createCost);

		if (gate.getControls().length != 2) {
			Stargate.log.log(Level.SEVERE, "Could not load Gate " + file.getName() + " - Gates must have exactly 2 control points.");
			return null;
		} else {
			gate.save(file.getParent() + "/"); // Updates format for version changes
			return gate;
		}
	}

	private static int readConfig(HashMap<String, String> config, Gate gate, File file, String key, int def) {
		if (config.containsKey(key)) {
			try {
				return Integer.parseInt(config.get(key));
			} catch (NumberFormatException ex) {
				Stargate.log.log(Level.WARNING, String.format("%s reading %s: %s is not numeric", ex.getClass().getName(), file, key));
			}
		}

		return def;
	}

	public static void loadGates(String gateFolder) {
		File dir = new File(gateFolder);
		File[] files;

		if (dir.exists()) {
			files = dir.listFiles(new StargateFilenameFilter());
		} else {
			files = new File[0];
		}

		if (files.length == 0) {
			dir.mkdir();
			populateDefaults(gateFolder);
		} else {
			for (File file : files) {
				Gate gate = loadGate(file);
				if (gate != null) registerGate(gate);
			}
		}
	}
	
	public static void populateDefaults(String gateFolder) {
		int Obsidian = Material.OBSIDIAN.getId();
		Integer[][] layout = new Integer[][] {
			{ANYTHING, Obsidian,Obsidian, ANYTHING},
			{Obsidian, ENTRANCE, ENTRANCE, Obsidian},
			{CONTROL, ENTRANCE, ENTRANCE, CONTROL},
			{Obsidian, EXIT, ENTRANCE, Obsidian},
			{ANYTHING, Obsidian, Obsidian, ANYTHING},
		};
		HashMap<Character, Integer> types = new HashMap<Character, Integer>();
		types.put('X', Obsidian);
		types.put('-', Obsidian);

		Gate gate = new Gate("nethergate.gate", layout, types);
		gate.save(gateFolder);
		registerGate(gate);
	}

	public static Gate[] getGatesByControlBlock(Block block) {
		return getGatesByControlBlock(block.getTypeId());
	}

	public static Gate[] getGatesByControlBlock(int type) {
		Gate[] result = new Gate[0];
		ArrayList<Gate> lookup = controlBlocks.get(type);
		
		if (lookup != null) result = lookup.toArray(result);

		return result;
	}

	public static Gate getGateByName(String name) {
		return gates.get(name);
	}
	
	public static int getGateCount() {
		return gates.size();
	}
	
	static class StargateFilenameFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return name.endsWith(".gate");
		}
	}
}
