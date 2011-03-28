package net.TheDgtl.Stargate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.Button;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

/**
 * Portal.java - Plug-in for hey0's minecraft mod.
 * @author Shaun (sturmeh)
 * @author Dinnerbone
 */
public class Portal {
	// Variables used to store portal lists
	private static final HashMap<Blox, Portal> lookupBlocks = new HashMap<Blox, Portal>();
	private static final HashMap<Blox, Portal> lookupEntrances = new HashMap<Blox, Portal>();
	private static final ArrayList<Portal> allPortals = new ArrayList<Portal>();
	private static final HashMap<String, ArrayList<String>> allPortalsNet = new HashMap<String, ArrayList<String>>();
	private static final HashMap<String, HashMap<String, Portal>> lookupNamesNet = new HashMap<String, HashMap<String, Portal>>();
	
	// Gate location block info
	private Blox topLeft;
	private int modX;
	private int modZ;
	private float rotX;
	// Block references
	private SignPost id;
	private Blox button;
	private Blox[] frame;
	private Blox[] entrances;
	// Gate information
	private String name;
	private String destination;
	private String network;
	private Gate gate;
	private String owner = "";
	private boolean hidden = false;
	private boolean alwaysOn = false;
	private boolean priv = false;
	private World world;
	// Gate options
	private boolean verified;
	private boolean fixed;
	// In-use information
	private Player player;
	private Player activePlayer;
	private ArrayList<String> destinations = new ArrayList<String>();
	private boolean isOpen = false;
	private long openTime;

	private Portal(Blox topLeft, int modX, int modZ,
			float rotX, SignPost id, Blox button,
			String dest, String name,
			boolean verified, String network, Gate gate,
			String owner, boolean hidden, boolean alwaysOn, boolean priv) {
		this.topLeft = topLeft;
		this.modX = modX;
		this.modZ = modZ;
		this.rotX = rotX;
		this.id = id;
		this.destination = dest;
		this.button = button;
		this.verified = verified;
		this.fixed = dest.length() > 0;
		this.network = network;
		this.name = name;
		this.gate = gate;
		this.owner = owner;
		this.hidden = hidden;
		this.alwaysOn = alwaysOn;
		this.priv = priv;
		this.world = topLeft.getWorld();
		
		if (this.alwaysOn && !this.fixed) {
			this.alwaysOn = false;
			Stargate.log.log(Level.WARNING, "Can not create a non-fixed always-on gate.");
		}

		this.register();
		if (verified) {
			this.drawSign();
		}
	}

	public boolean isOpen() {
		return isOpen || isAlwaysOn();
	}
	
	public boolean isAlwaysOn() {
		return alwaysOn && isFixed();
	}
	
	public boolean isHidden() {
		return hidden;
	}
	
	public boolean isPrivate() {
		return priv;
	}

	public boolean open(boolean force) {
		return open(null, force);
	}

	public boolean open(Player openFor, boolean force) {
		if (isOpen() && !force) return false;

		world.loadChunk(world.getChunkAt(topLeft.getBlock()));

		for (Blox inside : getEntrances()) {
			inside.setType(gate.getPortalBlockOpen());
		}

		isOpen = true;
		openTime = System.currentTimeMillis() / 1000;
		Stargate.openList.add(this);
		Stargate.activeList.remove(this);
		// Open remote gate
		if (!isAlwaysOn()) {
			player = openFor;

			Portal end = getDestination();
			if (end != null && !end.isFixed() && !end.isOpen()) {
				end.open(openFor, false);
				end.setDestination(this);
				if (end.isVerified()) end.drawSign();
			}
		}

		return true;
	}

	public void close(boolean force) {
		if (isAlwaysOn() && !force) return; // Never close an always open gate
		
		// Close this gate, then the dest gate.
		for (Blox inside : getEntrances()) {
			inside.setType(gate.getPortalBlockClosed());
		}

		player = null;
		isOpen = false;
		Stargate.openList.remove(this);
		Stargate.activeList.remove(this);
		
		if (!isAlwaysOn()) {
			Portal end = getDestination();

			if (end != null && end.isOpen()) {
				end.deactivate(); // Clear it's destination first.
				end.close(false);
			}
		}
		
		deactivate();
	}

	public boolean isOpenFor(Player player) {
		if ((isAlwaysOn()) || (this.player == null)) {
			return true;
		}
		return (player != null) && (player.getName().equalsIgnoreCase(this.player.getName()));
	}

	public boolean isFixed() {
		return fixed;
	}

	public boolean isPowered() {
		RelativeBlockVector[] controls = gate.getControls();

		for (RelativeBlockVector vector : controls) {
			MaterialData mat = getBlockAt(vector).getBlock().getState().getData();
			
			if (mat instanceof Button && ((Button)mat).isPowered())
				return true;
		}

		return false;
	}

	public void teleport(Player player, Portal origin, PlayerMoveEvent event) {
		Location traveller = player.getLocation();
		Location exit = getExit(traveller, origin);

		exit.setYaw(origin.getRotation() - traveller.getYaw() + this.getRotation() + 180);

		// Change "from" so we don't get hack warnings. Cancel player move event.
		event.setFrom(exit);
		player.teleportTo(exit);
		event.setCancelled(true);
	}

	public void teleport(Vehicle vehicle, Portal origin) {
		Location traveller = new Location(this.world, vehicle.getLocation().getX(), vehicle.getLocation().getY(), vehicle.getLocation().getZ());
		Location exit = getExit(traveller, origin);
		
		//double velocity = vehicle.getVelocity().length();
		
		// Stop and teleport
		vehicle.setVelocity(new Vector());
		Entity passenger = vehicle.getPassenger();
		
		vehicle.teleportTo(exit);
		if (passenger != null) {
			if (passenger instanceof Player)
				((Player)passenger).teleportTo(exit);
			vehicle.setPassenger(passenger);
		}
		
		// Get new velocity
		Vector newVelocity = new Vector();
		switch ((int)id.getBlock().getData()) {
		case 2:
			newVelocity.setZ(-1);
			break;
		case 3:
			newVelocity.setZ(1);
			break;
		case 4:
			newVelocity.setX(-1);
			break;
		case 5:
			newVelocity.setX(1);
			break;
		}
		// TODO: Initial velocity is returning 0, odd.
		//newVelocity.multiply(velocity);
		// Set new velocity.
		vehicle.setVelocity(newVelocity);
	}

	public Location getExit(Location traveller, Portal origin) {
		Location loc = null;
		// Check if the gate has an exit block
		if (gate.getExit() != null) {
			Blox exit = getBlockAt(gate.getExit());
			loc = exit.modRelativeLoc(0D, 0D, 1D, traveller.getYaw(), traveller.getPitch(), modX, 1, modZ);
		} else {
			Stargate.log.log(Level.WARNING, "[Stargate] Missing destination point in .gate file " + gate.getFilename());
		}
		if (loc != null) {
			Block block = world.getBlockAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

			if (block.getType() == Material.STEP) {
				loc.setY(loc.getY() + 0.5);
			}

			loc.setPitch(traveller.getPitch());
			return loc;
		}
		return traveller;
	}

	public float getRotation() {
		return rotX;
	}

	public void setName(String name) {
		this.name = filterName(name);

		drawSign();
	}

	public String getName() {
		return name;
	}

	public void setDestination(Portal destination) {
		setDestination(destination.getName());
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public Portal getDestination() {
		return Portal.getByName(destination, getNetwork());
	}

	public String getDestinationName() {
		return destination;
	}
	
	public boolean isChunkLoaded() {
		return topLeft.getWorld().isChunkLoaded(topLeft.getBlock().getChunk());
	}
	
	public void loadChunk() {
		topLeft.getWorld().loadChunk(topLeft.getBlock().getChunk());
	}

	public boolean isVerified() {
		for (RelativeBlockVector control : gate.getControls())
			verified = verified || getBlockAt(control).getBlock().getTypeId() == gate.getControlBlock();
		return verified;
	}

	public boolean wasVerified() {
		return verified;
	}

	public boolean checkIntegrity() {
		return gate.matches(topLeft, modX, modZ);
	}

	public Gate getGate() {
		return gate;
	}

	public String getOwner() {
		return owner;
	}

	public void activate(Player player) {
		destinations.clear();
		destination = "";
		drawSign();
		Stargate.activeList.add(this);
		activePlayer = player;
		for (String dest : allPortalsNet.get(getNetwork().toLowerCase())) {
			Portal portal = getByName(dest, getNetwork());
			// Not fixed, not this portal, and visible to this player.
			if (	(!portal.isFixed()) &&
					(!dest.equalsIgnoreCase(getName())) && 							// Not this portal
					(!portal.isHidden() || Stargate.hasPerm(player, "stargate.hidden", player.isOp()) || portal.getOwner().equals(player.getName()))
				) {
				destinations.add(portal.getName());
			}
		}
	}

	public void deactivate() {
		Stargate.activeList.remove(this);
		if (isFixed()) {
			return;
		}
		destinations.clear();
		destination = "";
		activePlayer = null;
		drawSign();
	}

	public boolean isActive() {
		return fixed || (destinations.size() > 0);
	}

	public Player getActivePlayer() {
		return activePlayer;
	}

	public String getNetwork() {
		return network;
	}
	
	public long getOpenTime() {
		return openTime;
	}

	public void cycleDestination(Player player) {
		if (!isActive() || getActivePlayer() != player) {
			activate(player);
		}

		if (destinations.size() > 0) {
			int index = destinations.indexOf(destination);
			if (++index >= destinations.size()) {
				index = 0;
			}
			destination = destinations.get(index);
		}
		openTime = System.currentTimeMillis() / 1000;
		drawSign();
	}

	public final void drawSign() {
		id.setText(0, "--" + name + "--");
		int max = destinations.size() - 1;
		int done = 0;

		if (!isActive()) {
			id.setText(++done, "Right click to");
			id.setText(++done, "use the gate");
			id.setText(++done, " (" + network + ") ");
		} else {
			if (isFixed()) {
				id.setText(++done, "To: " + destination);
				id.setText(++done, " (" + network + ") ");
			} else {
				int index = destinations.indexOf(destination);

				if ((index == max) && (max > 1) && (++done <= 3)) {
					id.setText(done, destinations.get(index - 2));
				}
				if ((index > 0) && (++done <= 3)) {
					id.setText(done, destinations.get(index - 1));
				}
				if (++done <= 3) {
					id.setText(done, " >" + destination + "< ");
				}
				if ((max >= index + 1) && (++done <= 3)) {
					id.setText(done, destinations.get(index + 1));
				}
				if ((max >= index + 2) && (++done <= 3)) {
					id.setText(done, destinations.get(index + 2));
				}
			}
		}

		for (done++; done <= 3; done++) {
			id.setText(done, "");
		}

		id.update();
	}

	public Blox[] getEntrances() {
		if (entrances == null) {
			RelativeBlockVector[] space = gate.getEntrances();
			entrances = new Blox[space.length];
			int i = 0;

			for (RelativeBlockVector vector : space) {
				entrances[i++] = getBlockAt(vector);
			}
		}
		return entrances;
	}

	public Blox[] getFrame() {
		if (frame == null) {
			RelativeBlockVector[] border = gate.getBorder();
			RelativeBlockVector[] controls = gate.getControls();
			frame = new Blox[border.length + controls.length];
			int i = 0;

			for (RelativeBlockVector vector : border) {
				frame[i++] = getBlockAt(vector);
			}

			for (RelativeBlockVector vector : controls) {
				frame[i++] = getBlockAt(vector);
			}
		}

		return frame;
	}

	public void unregister(boolean removeAll) {
		Stargate.log.info("[Stargate] Unregistering gate " + getName());
		close(true);
		lookupNamesNet.get(getNetwork().toLowerCase()).remove(getName().toLowerCase());

		for (Blox block : getFrame()) {
			lookupBlocks.remove(block);
		}
		// Include the sign and button
		lookupBlocks.remove(new Blox(id.getBlock()));
		if (button != null) {
			lookupBlocks.remove(button);
		}

		for (Blox entrance : getEntrances()) {
			lookupEntrances.remove(entrance);
		}

		if (removeAll)
			allPortals.remove(this);
		
		allPortalsNet.get(getNetwork().toLowerCase()).remove(getName().toLowerCase());

		if (id.getBlock().getType() == Material.WALL_SIGN) {
			id.setText(0, getName());
			id.setText(1, "");
			id.setText(2, "");
			id.setText(3, "");
			id.update();
		}

		for (String originName : allPortalsNet.get(getNetwork().toLowerCase())) {
			Portal origin = Portal.getByName(originName, getNetwork());
			if ((origin != null) && (origin.isAlwaysOn()) && (origin.getDestinationName().equalsIgnoreCase(getName())) && (origin.isVerified())) {
				origin.close(true);
			}
		}

		saveAllGates(world);
	}

	private Blox getBlockAt(RelativeBlockVector vector) {
		return topLeft.modRelative(vector.getRight(), vector.getDepth(), vector.getDistance(), modX, 1, modZ);
	}

	private void register() {
		if (!lookupNamesNet.containsKey(getNetwork().toLowerCase()))
			lookupNamesNet.put(getNetwork().toLowerCase(), new HashMap<String, Portal>());
		lookupNamesNet.get(getNetwork().toLowerCase()).put(getName().toLowerCase(), this);

		for (Blox block : getFrame()) {
			lookupBlocks.put(block, this);
		}
		// Include the sign and button
		lookupBlocks.put(new Blox(id.getBlock()), this);
		if (button != null) {
			lookupBlocks.put(button, this);
		}

		for (Blox entrance : getEntrances()) {
			lookupEntrances.put(entrance, this);
		}

		allPortals.add(this);
		// Check if this network exists
		if (!allPortalsNet.containsKey(getNetwork().toLowerCase()))
			allPortalsNet.put(getNetwork().toLowerCase(), new ArrayList<String>());
		allPortalsNet.get(getNetwork().toLowerCase()).add(getName().toLowerCase());
	}

	public static Portal createPortal(SignPost id, Player player) {
		Block idParent = id.getParent();
		if (idParent == null) return null;
		if (Gate.getGatesByControlBlock(idParent).length == 0) return null;

		Blox parent = new Blox(player.getWorld(), idParent.getX(), idParent.getY(), idParent.getZ());
		Blox topleft = null;
		String name = filterName(id.getText(0));
		String destName = filterName(id.getText(1));
		String network = filterName(id.getText(2));
		String options = filterName(id.getText(3));
		boolean hidden = (options.indexOf('h') != -1 || options.indexOf('H') != -1);
		boolean alwaysOn = (options.indexOf('a') != -1 || options.indexOf('A') != -1);
		boolean priv = (options.indexOf('p') != -1 || options.indexOf('P') != -1);
		
		// Check if the user can only create personal gates, set network if so
		if (Stargate.hasPerm(player, "stargate.create.personal", false) && 
			!Stargate.hasPerm(player, "stargate.create", player.isOp()) ) {
			network = player.getName();
			if (network.length() > 11) network = network.substring(0, 11);
		}
		
		// Can not create a non-fixed always-on gate.
		if (alwaysOn && destName.length() == 0) {
			alwaysOn = false;
		}

		if ((network.length() < 1) || (network.length() > 11)) {
			network = Stargate.getDefaultNetwork();
		}
		if ((name.length() < 1) || (name.length() > 11) || (getByName(name, network) != null)) {
			return null;
		}

		int modX = 0;
		int modZ = 0;
		float rotX = 0f;
		int facing = 0;

		if (idParent.getX() > id.getBlock().getX()) {
			modZ -= 1;
			rotX = 90f;
			facing = 2;
		} else if (idParent.getX() < id.getBlock().getX()) {
			modZ += 1;
			rotX = 270f;
			facing = 1;
		} else if (idParent.getZ() > id.getBlock().getZ()) {
			modX += 1;
			rotX = 180f;
			facing = 4;
		} else if (idParent.getZ() < id.getBlock().getZ()) {
			modX -= 1;
			rotX = 0f;
			facing = 3;
		}

		Gate[] possibleGates = Gate.getGatesByControlBlock(idParent);
		Gate gate = null;
		RelativeBlockVector buttonVector = null;

		for (Gate possibility : possibleGates) {
			if ((gate == null) && (buttonVector == null)) {
				RelativeBlockVector[] vectors = possibility.getControls();
				RelativeBlockVector otherControl = null;

				for (RelativeBlockVector vector : vectors) {
					Blox tl = parent.modRelative(-vector.getRight(), -vector.getDepth(), -vector.getDistance(), modX, 1, modZ);

					if (gate == null) {
						if (possibility.matches(tl, modX, modZ)) {
							gate = possibility;
							topleft = tl;

							if (otherControl != null) {
								buttonVector = otherControl;
							}
						}
					} else if (otherControl != null) {
						buttonVector = vector;
					}

					otherControl = vector;
				}
			}
		}

		if ((gate == null) || (buttonVector == null)) {
			return null;
		}
		
		if (iConomyHandler.useiConomy() && !iConomyHandler.chargePlayer(player.getName(), iConomyHandler.createCost)) {
			if (!iConomyHandler.inFundMsg.isEmpty()) {
				player.sendMessage(ChatColor.RED + iConomyHandler.inFundMsg);
			}
			return null;
		}

		Portal portal = null;

		Blox button = null;
		// No button on an always-open gate.
		if (!alwaysOn) {
			button = topleft.modRelative(buttonVector.getRight(), buttonVector.getDepth(), buttonVector.getDistance() + 1, modX, 1, modZ);
			button.setType(Material.STONE_BUTTON.getId());
			button.setData(facing);
		}
		portal = new Portal(topleft, modX, modZ, rotX, id, button, destName, name, true, network, gate, player.getName(), hidden, alwaysOn, priv);

		// Open always on gate
		if (portal.isAlwaysOn()) {
			Portal dest = Portal.getByName(destName, portal.getNetwork());
			if (dest != null)
				portal.open(true);
		}
		
		// Open any always on gate pointing at this gate
		for (String originName : allPortalsNet.get(portal.getNetwork().toLowerCase())) {
			Portal origin = Portal.getByName(originName, portal.getNetwork());
			if (origin != null && origin.isAlwaysOn() && origin.getDestinationName().equalsIgnoreCase(portal.getName()) && origin.isVerified()) 
				origin.open(true);
		}

		saveAllGates(topleft.getWorld());

		return portal;
	}

	public static Portal getByName(String name, String network) {
		if (!lookupNamesNet.containsKey(network.toLowerCase())) return null;
		return lookupNamesNet.get(network.toLowerCase()).get(name.toLowerCase());
		
	}

	public static Portal getByEntrance(Location location) {
		return getByEntrance(new Blox(location).getBlock());
	}

	public static Portal getByEntrance(Block block) {
		return lookupEntrances.get(new Blox(block));
	}

	public static Portal getByBlock(Block block) {
		return lookupBlocks.get(new Blox(block));
	}

	public static void saveAllGates(World world) {
		String loc = Stargate.getSaveLocation() + "/" + world.getName() + ".db";

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(loc, false));

			for (Portal portal : allPortals) {
				String wName = portal.world.getName();
				if (!wName.equalsIgnoreCase(world.getName())) continue;
				StringBuilder builder = new StringBuilder();
				Blox sign = new Blox(portal.id.getBlock());
				Blox button = portal.button;

				builder.append(portal.name);
				builder.append(':');
				builder.append(sign.toString());
				builder.append(':');
				builder.append((button != null) ? button.toString() : "");
				builder.append(':');
				builder.append(portal.modX);
				builder.append(':');
				builder.append(portal.modZ);
				builder.append(':');
				builder.append(portal.rotX);
				builder.append(':');
				builder.append(portal.topLeft.toString());
				builder.append(':');
				builder.append(portal.gate.getFilename());
				builder.append(':');
				builder.append(portal.isFixed() ? portal.getDestinationName() : "");
				builder.append(':');
				builder.append(portal.getNetwork());
				builder.append(':');
				builder.append(portal.getOwner());
				builder.append(':');
				builder.append(portal.isHidden());
				builder.append(':');
				builder.append(portal.isAlwaysOn());
				builder.append(':');
				builder.append(portal.isPrivate());
				builder.append(':');
				builder.append(portal.world.getName());
				
				bw.append(builder.toString());
				bw.newLine();
			}

			bw.close();
		} catch (Exception e) {
			Stargate.log.log(Level.SEVERE, "Exception while writing stargates to " + loc + ": " + e);
		}
	}
	
	public static void clearGates() {
		lookupBlocks.clear();
		lookupNamesNet.clear();
		lookupEntrances.clear();
		allPortals.clear();
		allPortalsNet.clear();
	}

	public static void loadAllGates(World world) {
		String location = Stargate.getSaveLocation();
		
		File db = new File(location, world.getName() + ".db");

		if (db.exists()) {
			int l = 0;
			int portalCount = 0;
			try {
				Scanner scanner = new Scanner(db);
				while (scanner.hasNextLine()) {
					l++;
					String line = scanner.nextLine().trim();
					if (line.startsWith("#") || line.isEmpty()) {
						continue;
					}
					String[] split = line.split(":");
					if (split.length < 8) {
						Stargate.log.info("[Stargate] Invalid line - " + l);
						continue;
					}
					String name = split[0];
					Blox s = new Blox(world, split[1]);
					if (!(s.getBlock().getState() instanceof Sign)) {
						Stargate.log.info("[Stargate] Invalid sign on line " + l + " [" + s.getBlock() + "]");
						continue;
					}
					SignPost sign = new SignPost(s);
					Blox button = (split[2].length() > 0) ? new Blox(world, split[2]) : null;
					int modX = Integer.parseInt(split[3]);
					int modZ = Integer.parseInt(split[4]);
					float rotX = Float.parseFloat(split[5]);
					Blox topLeft = new Blox(world, split[6]);
					Gate gate = (split[7].contains(";")) ? Gate.getGateByName("nethergate.gate") : Gate.getGateByName(split[7]);
					if (gate == null) {
						Stargate.log.info("[Stargate] Invalid gate layout on line " + l + " [" + split[7] + "]");
						continue;
					}

					String dest = (split.length > 8) ? split[8] : "";
					String network = (split.length > 9) ? split[9] : Stargate.getDefaultNetwork();
					if (network.isEmpty()) network = Stargate.getDefaultNetwork();
					String owner = (split.length > 10) ? split[10] : "";
					boolean hidden = (split.length > 11) ? split[11].equalsIgnoreCase("true") : false;
					boolean alwaysOn = (split.length > 12) ? split[12].equalsIgnoreCase("true") : false;
					boolean priv = (split.length > 13) ? split[13].equalsIgnoreCase("true") : false;

					Portal portal = new Portal(topLeft, modX, modZ, rotX, sign, button, dest, name, false, network, gate, owner, hidden, alwaysOn, priv);
					portal.close(true);
				}
				scanner.close();
				
				// Open any always-on gates. Do this here as it should be more efficient than in the loop.
				int OpenCount = 0;
				//for (Portal portal : allPortals) {
				for (Iterator<Portal> iter = allPortals.iterator(); iter.hasNext(); ) {
					Portal portal = iter.next();
					if (portal == null) continue;

					// Verify portal integrity/register portal
					if (!portal.wasVerified()) {
						if (!portal.isVerified() || !portal.checkIntegrity()) {
							portal.unregister(false);
							iter.remove();
							Stargate.log.info("[Stargate] Destroying stargate at " + portal.toString());
							continue;
						} else {
							portal.drawSign();
							portalCount++;
						}
					}

					if (!portal.isAlwaysOn()) continue;
					
					Portal dest = portal.getDestination();
					if (dest != null) {
						portal.open(true);
						OpenCount++;
					}
				}
				Stargate.log.info("[Stargate] {" + world.getName() + "} Loaded " + portalCount + " stargates with " + OpenCount + " set as always-on");
			} catch (Exception e) {
				Stargate.log.log(Level.SEVERE, "Exception while reading stargates from " + db.getName() + ": " + l);
				e.printStackTrace();
			}
		} else {
			Stargate.log.info("[Stargate] {" + world.getName() + "} No stargates for world ");
		}
	}
	
	public static void closeAllGates() {
		Stargate.log.info("Closing all stargates.");
		for (Portal p : allPortals) {
			if (p == null) continue;
			p.close(true);
		}
	}

	public static String filterName(String input) {
		return input.replaceAll("[\\|:#]", "").trim();
	}
	
	@Override
	public String toString() {
		return String.format("Portal [id=%s, network=%s name=%s, type=%s]", id, network, name, gate.getFilename());
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		Portal portal = (Portal) obj;
		return (this.getNetwork().equalsIgnoreCase(portal.getNetwork()) && 
				this.getName().equalsIgnoreCase(portal.getName()));
	}
}
