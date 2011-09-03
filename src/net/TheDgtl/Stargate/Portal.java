package net.TheDgtl.Stargate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.logging.Level;

import net.TheDgtl.Stargate.event.StargateActivateEvent;
import net.TheDgtl.Stargate.event.StargateCloseEvent;
import net.TheDgtl.Stargate.event.StargateDeactivateEvent;
import net.TheDgtl.Stargate.event.StargateOpenEvent;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.material.Button;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;

/**
 * Portal.java
 * @author Shaun (sturmeh)
 * @author Dinnerbone
 * @author Steven "Drakia" Scott
 */
 
public class Portal {
	// Static variables used to store portal lists
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
	private World world;
	private boolean verified;
	private boolean fixed;
	
	// Options
	private boolean hidden = false;
	private boolean alwaysOn = false;
	private boolean priv = false;
	private boolean free = false;
	private boolean backwards = false;
	private boolean show = false;
	
	// In-use information
	private Player player;
	private Player activePlayer;
	private ArrayList<String> destinations = new ArrayList<String>();
	private boolean isOpen = false;
	private long openTime;

	private Portal(Blox topLeft, int modX, int modZ,
			float rotX, SignPost id, Blox button,
			String dest, String name,
			boolean verified, String network, Gate gate, String owner, 
			boolean hidden, boolean alwaysOn, boolean priv, boolean free, boolean backwards, boolean show) {
		this.topLeft = topLeft;
		this.modX = modX;
		this.modZ = modZ;
		this.rotX = rotX;
		this.id = id;
		this.destination = dest;
		this.button = button;
		this.verified = verified;
		this.network = network;
		this.name = name;
		this.gate = gate;
		this.owner = owner;
		this.hidden = hidden;
		this.alwaysOn = alwaysOn;
		this.priv = priv;
		this.free = free;
		this.backwards = backwards;
		this.show = show;
		this.world = topLeft.getWorld();
		this.fixed = dest.length() > 0;
		
		if (this.isAlwaysOn() && !this.isFixed()) {
			this.alwaysOn = false;
			Stargate.debug("Portal", "Can not create a non-fixed always-on gate. Setting AlwaysOn = false");
		}

		this.register();
		if (verified) {
			this.drawSign();
		}
	}
	
	/**
	 * Option Check Functions
	 */
	public boolean isOpen() {
		return isOpen || isAlwaysOn();
	}
	
	public boolean isAlwaysOn() {
		return alwaysOn;
	}
	
	public boolean isHidden() {
		return hidden;
	}
	
	public boolean isPrivate() {
		return priv;
	}
	
	public boolean isFree() {
		return free;
	}
	
	public boolean isBackwards() {
		return backwards;
	}
	
	public boolean isShown() {
		return show;
	}
	
	/**
	 * Getters and Setters
	 */

	public float getRotation() {
		return rotX;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = filterName(name);
		drawSign();
	}

	public Portal getDestination() {
		return Portal.getByName(destination, getNetwork());
	}
	
	public void setDestination(Portal destination) {
		setDestination(destination.getName());
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getDestinationName() {
		return destination;
	}
	
	public Gate getGate() {
		return gate;
	}

	public String getOwner() {
		return owner;
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
			frame = new Blox[border.length];
			int i = 0;

			for (RelativeBlockVector vector : border) {
				frame[i++] = getBlockAt(vector);
			}
		}

		return frame;
	}
	
	public World getWorld() {
		return world;
	}

	public boolean open(boolean force) {
		return open(null, force);
	}

	public boolean open(Player openFor, boolean force) {
		// Call the StargateOpenEvent
		StargateOpenEvent event = new StargateOpenEvent(openFor, this, force);
		Stargate.server.getPluginManager().callEvent(event);
		if (event.isCancelled()) return false;
		force = event.getForce();
		
		if (isOpen() && !force) return false;

		getWorld().loadChunk(getWorld().getChunkAt(topLeft.getBlock()));

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
			// Only open dest if it's not-fixed or points at this gate
			if (end != null && (!end.isFixed() || end.getDestinationName().equalsIgnoreCase(getName())) && !end.isOpen()) {
				end.open(openFor, false);
				end.setDestination(this);
				if (end.isVerified()) end.drawSign();
			}
		}

		return true;
	}

	public void close(boolean force) {
		if (!isOpen) return;
		// Call the StargateCloseEvent
		StargateCloseEvent event = new StargateCloseEvent(this, force);
		Stargate.server.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		force = event.getForce();
		
		if (isAlwaysOn() && !force) return; // Only close always-open if forced
		
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
		Location exit = getExit(traveller);

		// Handle backwards gates
		int adjust = isBackwards() ? 0 :180;
		exit.setYaw(origin.getRotation() - traveller.getYaw() + this.getRotation() + adjust);

		// The new method to teleport in a move event is set the "to" field.
		event.setTo(exit);
	}

	public void teleport(final Vehicle vehicle) {
		Location traveller = new Location(this.world, vehicle.getLocation().getX(), vehicle.getLocation().getY(), vehicle.getLocation().getZ());
		Location exit = getExit(traveller);
		
		double velocity = vehicle.getVelocity().length();
		
		// Stop and teleport
		vehicle.setVelocity(new Vector());
		
		// Get new velocity
		final Vector newVelocity = new Vector();
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
		newVelocity.multiply(velocity);
		
		final Entity passenger = vehicle.getPassenger();
		if (passenger != null) {
			final Vehicle v = exit.getWorld().spawn(exit, vehicle.getClass());
			vehicle.eject();
			vehicle.remove();
			passenger.teleport(exit);
			Stargate.server.getScheduler().scheduleSyncDelayedTask(Stargate.stargate, new Runnable() {
				public void run() {
					v.setPassenger(passenger);
					v.setVelocity(newVelocity);
				}
			}, 1);
		} else {
			Vehicle mc = exit.getWorld().spawn(exit, vehicle.getClass());
			if (mc instanceof StorageMinecart) {
				StorageMinecart smc = (StorageMinecart)mc;
				smc.getInventory().setContents(((StorageMinecart)vehicle).getInventory().getContents());
			}
			mc.setVelocity(newVelocity);
			vehicle.remove();
		}
	}

	public Location getExit(Location traveller) {
		Location loc = null;
		// Check if the gate has an exit block
		if (gate.getExit() != null) {
			Blox exit = getBlockAt(gate.getExit());
			int back = (isBackwards()) ? -1 : 1;
			loc = exit.modRelativeLoc(0D, 0D, 1D, traveller.getYaw(), traveller.getPitch(), modX * back, 1, modZ * back);
		} else {
			Stargate.log.log(Level.WARNING, "[Stargate] Missing destination point in .gate file " + gate.getFilename());
		}
		if (loc != null) {

			if (getWorld().getBlockTypeIdAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) == Material.STEP.getId()) {
				loc.setY(loc.getY() + 0.5);
			}

			loc.setPitch(traveller.getPitch());
			return loc;
		}
		return traveller;
	}
	
	public boolean isChunkLoaded() {
		return getWorld().isChunkLoaded(topLeft.getBlock().getChunk());
	}
	
	public void loadChunk() {
		getWorld().loadChunk(topLeft.getBlock().getChunk());
	}

	public boolean isVerified() {
		verified = true;
		for (RelativeBlockVector control : gate.getControls())
			verified = verified && getBlockAt(control).getBlock().getTypeId() == gate.getControlBlock();
		return verified;
	}

	public boolean wasVerified() {
		return verified;
	}

	public boolean checkIntegrity() {
		return gate.matches(topLeft, modX, modZ);
	}

	public void activate(Player player) {
		StargateActivateEvent event = new StargateActivateEvent(this);
		Stargate.server.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		
		destinations.clear();
		destination = "";
		drawSign();
		Stargate.activeList.add(this);
		activePlayer = player;
		String network = getNetwork();
		for (String dest : allPortalsNet.get(network.toLowerCase())) {
			Portal portal = getByName(dest, network);
			// Check if dest is always open (Don't show if so)
			if (portal.isAlwaysOn() && !portal.isShown()) continue;
			// Check if this player can access the dest world
			if (!Stargate.canAccessWorld(player, portal.getWorld().getName())) continue;
			// Check if dest is this portal
			if (dest.equalsIgnoreCase(getName())) continue;
			// Check if dest is a fixed gate not pointing to this gate
			if (portal.isFixed() && !portal.getDestinationName().equalsIgnoreCase(getName())) continue;
			// Visible to this player.
			if (Stargate.canSee(player, portal)) {
				destinations.add(portal.getName());
			}
		}
	}

	public void deactivate() {
		StargateDeactivateEvent event = new StargateDeactivateEvent(this);
		Stargate.server.getPluginManager().callEvent(event);
		if (event.isCancelled()) return;
		
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
		return isFixed() || (destinations.size() > 0);
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
		cycleDestination(player, 1);
	}
	
	public void cycleDestination(Player player, int dir) {
		if (!isActive() || getActivePlayer() != player) {
			activate(player);
			Stargate.debug("cycleDestination", "Network Size: " + allPortalsNet.get(network.toLowerCase()).size());
			Stargate.debug("cycleDestination", "Player has access to: " + destinations.size());
		}
		
		if (destinations.size() == 0) {
			Stargate.sendMessage(player, Stargate.getString("destEmpty"));
			return;
		}

		if (destinations.size() > 0) {
			int index = destinations.indexOf(destination);
			index += dir;
			if (index >= destinations.size()) 
				index = 0;
			else if (index < 0) 
				index = destinations.size() - 1;
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
				Portal dest = Portal.getByName(destination, network);
				if (dest == null) {
					id.setText(++done, "(Not Connected)");
				} else {
					id.setText(++done, "");
				}
			} else {
				int index = destinations.indexOf(destination);

				if ((index == max) && (max > 1) && (++done <= 3)) {
					if (iConomyHandler.useiConomy() && iConomyHandler.freeGatesGreen) {
						Portal dest = Portal.getByName(destinations.get(index - 2), network);
						boolean green = Stargate.isFree(activePlayer, this, dest);
						id.setText(done, (green ? ChatColor.DARK_GREEN : "") + destinations.get(index - 2));
					} else {
						id.setText(done, destinations.get(index - 2));
					}
				}
				if ((index > 0) && (++done <= 3)) {
					if (iConomyHandler.useiConomy() && iConomyHandler.freeGatesGreen) {
						Portal dest = Portal.getByName(destinations.get(index - 1), network);
						boolean green = Stargate.isFree(activePlayer, this, dest);
						id.setText(done, (green ? ChatColor.DARK_GREEN : "") + destinations.get(index - 1));
					} else {
						id.setText(done, destinations.get(index - 1));
					}
				}
				if (++done <= 3) {
					if (iConomyHandler.useiConomy() && iConomyHandler.freeGatesGreen) {
						Portal dest = Portal.getByName(destination, network);
						boolean green = Stargate.isFree(activePlayer, this, dest);
						id.setText(done, (green ? ChatColor.DARK_GREEN : "") + " >" + destination + "< ");
					} else {
						id.setText(done, " >" + destination + "< ");
					}
				}
				if ((max >= index + 1) && (++done <= 3)) {
					if (iConomyHandler.useiConomy() && iConomyHandler.freeGatesGreen) {
						Portal dest = Portal.getByName(destinations.get(index + 1), network);
						boolean green = Stargate.isFree(activePlayer, this, dest);
						id.setText(done, (green ? ChatColor.DARK_GREEN : "") + destinations.get(index + 1));
					} else {
						id.setText(done, destinations.get(index + 1));
					}
				}
				if ((max >= index + 2) && (++done <= 3)) {
					if (iConomyHandler.useiConomy() && iConomyHandler.freeGatesGreen) {
						Portal dest = Portal.getByName(destinations.get(index + 2), network);
						boolean green = Stargate.isFree(activePlayer, this, dest);
						id.setText(done, (green ? ChatColor.DARK_GREEN : "") + destinations.get(index + 2));
					} else {
						id.setText(done, destinations.get(index + 2));
					}
				}
			}
		}

		for (done++; done <= 3; done++) {
			id.setText(done, "");
		}

		id.update();
	}

	public void unregister(boolean removeAll) {
		Stargate.debug("Unregister", "Unregistering gate " + getName());
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
			if (origin == null) continue;
			if (!origin.getDestinationName().equalsIgnoreCase(getName())) continue;
			if (!origin.isVerified()) continue;
			if (origin.isFixed()) origin.drawSign();
			if (origin.isAlwaysOn()) origin.close(true);
		}

		saveAllGates(getWorld());
	}

	private Blox getBlockAt(RelativeBlockVector vector) {
		return topLeft.modRelative(vector.getRight(), vector.getDepth(), vector.getDistance(), modX, 1, modZ);
	}

	private void register() {
		if (!lookupNamesNet.containsKey(getNetwork().toLowerCase())) {
			Stargate.debug("register", "Network not in lookupNamesNet, adding");
			lookupNamesNet.put(getNetwork().toLowerCase(), new HashMap<String, Portal>());
		}
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
		if (!allPortalsNet.containsKey(getNetwork().toLowerCase())) {
			Stargate.debug("register", "Network not in allPortalsNet, adding");
			allPortalsNet.put(getNetwork().toLowerCase(), new ArrayList<String>());
		}
		allPortalsNet.get(getNetwork().toLowerCase()).add(getName().toLowerCase());
	}

	public static Portal createPortal(SignChangeEvent event, Player player) {
		SignPost id = new SignPost(new Blox(event.getBlock()));
		Block idParent = id.getParent();
		if (idParent == null) {
			return null;
		}
		
		if (Gate.getGatesByControlBlock(idParent).length == 0) return null;
		
		if (Portal.getByBlock(idParent) != null) {
			Stargate.debug("createPortal", "idParent belongs to existing gate");
			return null;
		}

		Blox parent = new Blox(player.getWorld(), idParent.getX(), idParent.getY(), idParent.getZ());
		Blox topleft = null;
		String name = filterName(event.getLine(0));
		String destName = filterName(event.getLine(1));
		String network = filterName(event.getLine(2));
		String options = filterName(event.getLine(3));
		boolean hidden = (options.indexOf('h') != -1 || options.indexOf('H') != -1);
		boolean alwaysOn = (options.indexOf('a') != -1 || options.indexOf('A') != -1);
		boolean priv = (options.indexOf('p') != -1 || options.indexOf('P') != -1);
		boolean free = (options.indexOf('f') != - 1|| options.indexOf('F') != -1);
		boolean backwards = (options.indexOf('b') != -1 || options.indexOf('B') != -1);
		boolean show = (options.indexOf('s') != -1 || options.indexOf('S') != -1);
		
		// Check permissions for options.
		if (hidden && !Stargate.canOption(player, "hidden")) hidden = false;
		if (alwaysOn && !Stargate.canOption(player, "alwayson")) alwaysOn = false;
		if (priv && !Stargate.canOption(player, "private")) priv = false;
		if (free && !Stargate.canOption(player, "free")) free = false;
		if (backwards && !Stargate.canOption(player, "backwards")) backwards = false;
		if (show && !Stargate.canOption(player,  "show")) show = false;
		
		// Can not create a non-fixed always-on gate.
		if (alwaysOn && destName.length() == 0) {
			alwaysOn = false;
		}
		
		// Show isn't useful if A is false
		if (show && !alwaysOn) {
			show = false;
		}
		
		// Moved the layout check so as to avoid invalid messages when not making a gate
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
			Stargate.debug("createPortal", "Could not find matching gate layout");
			return null;
		}
		
		// Debug
		Stargate.debug("createPortal", "h = " + hidden + " a = " + alwaysOn + " p = " + priv + " f = " + free + " b = " + backwards + " s = " + show);

		if ((network.length() < 1) || (network.length() > 11)) {
			network = Stargate.getDefaultNetwork();
		}
		
		// Check if the player can create gates on this network
		if (!Stargate.canCreate(player, network)) {
			Stargate.debug("createPortal", "Player doesn't have create permissions on network. Trying personal");
			if (Stargate.canCreatePersonal(player)) {
				network = player.getName();
				if (network.length() > 11) network = network.substring(0, 11);
				Stargate.debug("createPortal", "Creating personal portal");
				Stargate.sendMessage(player, Stargate.getString("createPersonal"));
			} else {
				Stargate.debug("createPortal", "Player does not have access to network");
				Stargate.sendMessage(player, Stargate.getString("createNetDeny"));
				return null;
			}
		}
		
		// Check if the player can create this gate layout
		String gateName = gate.getFilename();
		gateName = gateName.substring(0, gateName.indexOf('.'));
		if (!Stargate.canCreateGate(player, gateName)) {
			Stargate.debug("createPortal", "Player does not have access to gate layout");
			Stargate.sendMessage(player, Stargate.getString("createGateDeny"));
			return null;
		}
		
		if (name.length() < 1 || name.length() > 11) {
			Stargate.debug("createPortal", "Name length error");
			Stargate.sendMessage(player, Stargate.getString("createNameLength"));
			return null;
		}
		
		if (getByName(name, network) != null) {
			Stargate.debug("createPortal", "Name Error");
			Stargate.sendMessage(player,  Stargate.getString("createExists"));
			return null;
		}
		
		// Check if there are too many gates in this network
		ArrayList<String> netList = allPortalsNet.get(network.toLowerCase());
		if (Stargate.maxGates > 0 && netList != null && netList.size() >= Stargate.maxGates) {
			Stargate.sendMessage(player, Stargate.getString("createFull"));
			return null;
		}
		
		// Check if the user can create gates to this world.
		if (destName.length() > 0) {
			Portal p = Portal.getByName(destName, network);
			if (p != null) {
				String world = p.getWorld().getName();
				if (!Stargate.canAccessWorld(player, world)) {
					Stargate.debug("canCreate", "Player does not have access to destination world");
					Stargate.sendMessage(player, Stargate.getString("createWorldDeny"));
					return null;
				}
			}
		}
		
		// Bleh, gotta check to make sure none of this gate belongs to another gate. Boo slow.
		for (RelativeBlockVector v : gate.getBorder()) {
			Blox b = topleft.modRelative(v.getRight(), v.getDepth(), v.getDistance(), modX, 1, modZ);
			if (Portal.getByBlock(b.getBlock()) != null) {
				Stargate.debug("createPortal", "Gate conflicts with existing gate");
				Stargate.sendMessage(player, Stargate.getString("createConflict"));
				return null;
			}
		}
		
		int cost = Stargate.getCreateCost(player, gate); 
		if (cost > 0) {
			if (!Stargate.chargePlayer(player, null, gate.getCreateCost())) {
				String inFundMsg = Stargate.getString("ecoInFunds");
				inFundMsg = Stargate.replaceVars(inFundMsg, new String[] {"%cost%", "%portal%"}, new String[] {iConomyHandler.format(cost), name});
				Stargate.sendMessage(player, inFundMsg);
				Stargate.debug("createPortal", "Insufficient Funds");
				return null;
			}
			String deductMsg = Stargate.getString("ecoDeduct");
			deductMsg = Stargate.replaceVars(deductMsg, new String[] {"%cost%", "%portal%"}, new String[] {iConomyHandler.format(cost), name});
			Stargate.sendMessage(player, deductMsg, false);
		}

		Portal portal = null;

		Blox button = null;
		// No button on an always-open gate.
		if (!alwaysOn) {
			button = topleft.modRelative(buttonVector.getRight(), buttonVector.getDepth(), buttonVector.getDistance() + 1, modX, 1, modZ);
			button.setType(Material.STONE_BUTTON.getId());
			button.setData(facing);
		}
		portal = new Portal(topleft, modX, modZ, rotX, id, button, destName, name, true, network, gate, player.getName(), hidden, alwaysOn, priv, free, backwards, show);

		// Open always on gate
		if (portal.isAlwaysOn()) {
			Portal dest = Portal.getByName(destName, portal.getNetwork());
			if (dest != null) {
				portal.open(true);
				dest.drawSign();
			}
		}
		
		// Open any always on gate pointing at this gate
		for (String originName : allPortalsNet.get(portal.getNetwork().toLowerCase())) {
			Portal origin = Portal.getByName(originName, portal.getNetwork());
			if (origin == null) continue;
			if (!origin.getDestinationName().equalsIgnoreCase(portal.getName())) continue;
			if (!origin.isVerified()) continue;
			if (origin.isFixed()) origin.drawSign();
			if (origin.isAlwaysOn()) origin.open(true);
		}

		saveAllGates(portal.getWorld());

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
				builder.append(':');
				builder.append(portal.isFree());
				builder.append(':');
				builder.append(portal.isBackwards());
				builder.append(':');
				builder.append(portal.isShown());
				
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
						Stargate.log.info("[Stargate] Sign on line " + l + " doesn't exist. BlockType = " + s.getBlock().getType());
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
						Stargate.log.info("[Stargate] Gate layout on line " + l + " does not exist [" + split[7] + "]");
						continue;
					}

					String dest = (split.length > 8) ? split[8] : "";
					String network = (split.length > 9) ? split[9] : Stargate.getDefaultNetwork();
					if (network.isEmpty()) network = Stargate.getDefaultNetwork();
					String owner = (split.length > 10) ? split[10] : "";
					boolean hidden = (split.length > 11) ? split[11].equalsIgnoreCase("true") : false;
					boolean alwaysOn = (split.length > 12) ? split[12].equalsIgnoreCase("true") : false;
					boolean priv = (split.length > 13) ? split[13].equalsIgnoreCase("true") : false;
					boolean free = (split.length > 15) ? split[15].equalsIgnoreCase("true") : false;
					boolean backwards = (split.length > 16) ? split[16].equalsIgnoreCase("true") : false;
					boolean show = (split.length > 17) ? split[17].equalsIgnoreCase("true") : false;

					Portal portal = new Portal(topLeft, modX, modZ, rotX, sign, button, dest, name, false, network, gate, owner, hidden, alwaysOn, priv, free, backwards, show);
					portal.close(true);
				}
				scanner.close();
				
				// Open any always-on gates. Do this here as it should be more efficient than in the loop.
				int OpenCount = 0;
				for (Iterator<Portal> iter = allPortals.iterator(); iter.hasNext(); ) {
					Portal portal = iter.next();
					if (portal == null) continue;

					// Verify portal integrity/register portal
					if (!portal.wasVerified()) {
						if (!portal.isVerified() || !portal.checkIntegrity()) {
							// DEBUG
							for (RelativeBlockVector control : portal.getGate().getControls()) {
								if (portal.getBlockAt(control).getBlock().getTypeId() != portal.getGate().getControlBlock()) {
									Stargate.debug("loadAllGates", "Control Block Type == " + portal.getBlockAt(control).getBlock().getTypeId());
								}
							}
							portal.unregister(false);
							iter.remove();
							Stargate.log.info("[Stargate] Destroying stargate at " + portal.toString());
							continue;
						} else {
							portal.drawSign();
							portalCount++;
						}
					}

					if (!portal.isFixed()) continue;
					Portal dest = portal.getDestination();
					if (dest != null) {
						if (portal.isAlwaysOn()) {
							portal.open(true);
							OpenCount++;
						}
						portal.drawSign();
						dest.drawSign();
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
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((network == null) ? 0 : network.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Portal other = (Portal) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equalsIgnoreCase(other.name))
			return false;
		if (network == null) {
			if (other.network != null)
				return false;
		} else if (!network.equalsIgnoreCase(other.network))
			return false;
		return true;
	}
}
