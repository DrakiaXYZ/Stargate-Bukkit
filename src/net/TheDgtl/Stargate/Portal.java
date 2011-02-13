package net.TheDgtl.Stargate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;

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
    public static final int OBSIDIAN = 49;
    public static final int FIRE = 51;
    public static final int SIGN = 68;
    public static final int BUTTON = 77;
    private static final HashMap<Blox, Portal> lookupBlocks = new HashMap<Blox, Portal>();
    private static final HashMap<String, Portal> lookupNames = new HashMap<String, Portal>();
    private static final HashMap<Blox, Portal> lookupEntrances = new HashMap<Blox, Portal>();
    private static final ArrayList<String> allPortals = new ArrayList<String>();
    private Blox topLeft;
    private int modX;
    private int modZ;
    private float rotX;
    private SignPost id;
    private String name;
    private String destination;
    private Blox button;
    private Player player;
    private Blox[] frame;
    private Blox[] entrances;
    private boolean verified;
    private boolean fixed;
    private boolean gracePeriod;
    private ArrayList<String> destinations = new ArrayList<String>();
    private String network;
    private Gate gate;
    private boolean isOpen = false;
    private String owner = "";
    private HashMap<Blox, Integer> exits;
    private HashMap<Integer, Blox> reverseExits;
    private boolean hidden = false;
    private Player activePlayer;
    private boolean alwaysOn = false;
    private World world;

    private Portal(Blox topLeft, int modX, int modZ,
            float rotX, SignPost id, Blox button,
            String dest, String name,
            boolean verified, String network, Gate gate,
            String owner, boolean hidden, boolean alwaysOn) {
        this.topLeft = topLeft;
        this.modX = modX;
        this.modZ = modZ;
        this.rotX = rotX;
        this.id = id;
        this.destination = dest;
        this.button = button;
        this.verified = verified;
        this.fixed = dest.length() > 0;
        this.gracePeriod = false;
        this.network = network;
        this.name = name;
        this.gate = gate;
        this.owner = owner;
        this.hidden = hidden;
        this.alwaysOn = alwaysOn;
        this.world = topLeft.getWorld();
        
        if (this.alwaysOn && !this.fixed) {
        	this.alwaysOn = false;
        	Stargate.log.log(Level.WARNING, "Can not create a non-fixed always-on gate.");
        }

        this.register();
        if (verified) {
            this.drawSign(true);
        }
    }

    public synchronized boolean manipGrace(boolean set, boolean var) {
        if (!set) {
            return gracePeriod;
        }
        gracePeriod = var;
        return false;
    }

    private boolean pastGrace() {
    	if (isAlwaysOn()) return false; // Ignore always open fixed gates.
        if (manipGrace(false, false)) {
            return manipGrace(true, false);
        }
        if (isActive() || isOpen()) {
            return true;
        }
        return false; // else.
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
        // Open remote gate
        if (!isFixed()) {
            player = openFor;
            manipGrace(true, true);

            Portal end = getDestination();
            if (end != null && !end.isOpen()) {
                end.open(openFor, false);
                end.setDestination(this);
                if (end.isVerified()) end.drawSign(true);
            }
        }

        return true;
    }

    public void close(boolean force) {
    	if (isAlwaysOn() && !force) return; // Never close an always open gate
        if (!isFixed()) {
        	Portal end = getDestination();

	        if (end != null && end.isOpen()) {
	            end.deactivate(); // Clear it's destination first.
	            end.close(false);
	        }
        }

        for (Blox inside : getEntrances()) {
            inside.setType(gate.getPortalBlockClosed());
        }

        player = null;
        isOpen = false;

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
        
        double velocity = vehicle.getVelocity().length();
        
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
        Blox entrance = new Blox(world.getBlockAt((int)Math.floor(traveller.getX()), (int)Math.floor(traveller.getY()), (int)Math.floor(traveller.getZ())));
        HashMap<Blox, Integer> originExits = origin.getExits();
        HashMap<Blox, Integer> destExits = this.getExits();

        if (originExits.containsKey(entrance)) {
            int position = (int)(((float)originExits.get(entrance) / originExits.size()) * destExits.size());
            Blox exit = getReverseExits().get(position);

            if (exit != null) {
                Location loc = exit.modRelativeLoc(0D, 0D, 1D, traveller.getYaw(), traveller.getPitch(), modX, 1, modZ);
                Block block = world.getBlockAt((int)Math.floor(loc.getX()), (int)Math.floor(loc.getY()), (int)Math.floor(loc.getZ()));

                if (block.getType() == Material.STEP) {
                    loc.setY(loc.getY() + 0.5);
                }

                loc.setPitch(traveller.getPitch());
                return loc;
            }
        }

        Stargate.log.log(Level.WARNING, "No position found calculting route from " + this + " to " + origin);
        Stargate.log.info(originExits.get(entrance) + " / " + originExits.size() + " * " + destExits.size());
        return traveller;
    }

    public float getRotation() {
        return rotX;
    }

    public void setName(String name) {
        this.name = filterName(name);

        drawSign(true);
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
        return Portal.getByName(destination);
    }

    public String getDestinationName() {
        return destination;
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
        drawSign(true);
        activePlayer = player;
        for (String dest : allPortals) {
            Portal portal = getByName(dest);
            if (	(portal.getNetwork().equalsIgnoreCase(network)) && 				// In the network
            		(!dest.equalsIgnoreCase(getName())) && 							// Not this portal
            		(!portal.isHidden() || Stargate.Permissions.has(player, "stargate.hidden") || portal.getOwner().equals(player.getName()))	// Is not hidden, player can view hidden, or player created
            	) {
                destinations.add(dest);
            }
        }
    }

    public void deactivate() {
        if (fixed) {
            return;
        }
        destinations.clear();
        destination = "";
        activePlayer = null;
        drawSign(true);
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

        drawSign(true);
    }

    public final void drawSign(boolean update) {
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
            } else {
                manipGrace(true, true);
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

        if (update) {
            id.update();
        }
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

    public HashMap<Blox, Integer> getExits() {
        if (exits == null) {
            exits = new HashMap<Blox, Integer>();
            reverseExits = new HashMap<Integer, Blox>();
            HashMap<RelativeBlockVector, Integer> relativeExits = gate.getExits();
            Set<RelativeBlockVector> exitBlocks = relativeExits.keySet();

            for (RelativeBlockVector vector : exitBlocks) {
                Blox block = getBlockAt(vector);
                Integer position = relativeExits.get(vector);
                exits.put(block, position);
                reverseExits.put(position, block);
            }
        }

        return exits;
    }

    public HashMap<Integer, Blox> getReverseExits() {
        if (reverseExits == null) {
            getExits();
        }

        return reverseExits;
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
        return this.getName().equalsIgnoreCase(portal.getName());
    }

    public void unregister() {
    	Stargate.log.info("[Stargate] Unregistering gate " + getName());
    	close(true);
        lookupNames.remove(getName().toLowerCase());

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

        allPortals.remove(getName());

        if (id.getBlock().getType() == Material.WALL_SIGN) {
            id.setText(0, getName());
            id.setText(1, "");
            id.setText(2, "");
            id.setText(3, "");
            id.update();
        }

        for (String originName : allPortals) {
            Portal origin = Portal.getByName(originName);
            if ((origin != null) && (origin.isAlwaysOn()) && (origin.getDestinationName().equalsIgnoreCase(getName())) && (origin.isVerified())) {
                origin.close(true);
            }
        }

        saveAllGates();
    }

    private Blox getBlockAt(int right, int depth) {
        return getBlockAt(right, depth, 0);
    }

    private Blox getBlockAt(RelativeBlockVector vector) {
        return topLeft.modRelative(vector.getRight(), vector.getDepth(), vector.getDistance(), modX, 1, modZ);
    }

    private Blox getBlockAt(int right, int depth, int distance) {
        return topLeft.modRelative(right, depth, distance, modX, 1, modZ);
    }

    private void register() {
    	Stargate.log.info("[Stargate] Registering gate " + getName());
        lookupNames.put(getName().toLowerCase(), this);

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

        allPortals.add(getName());
    }

    @Override
    public String toString() {
        return String.format("Portal [id=%s, name=%s, type=%s]", id, name, gate.getFilename());
    }

    public static Portal createPortal(SignPost id, Player player) {
        Block idParent = id.getParent();
        if (Gate.getGatesByControlBlock(idParent).length == 0) return null;

        Blox parent = new Blox(player.getWorld(), idParent.getX(), idParent.getY(), idParent.getZ());
        Blox topleft = null;
        String name = filterName(id.getText(0));
        String destName = filterName(id.getText(1));
        String network = filterName(id.getText(2));
        String options = filterName(id.getText(3));
        boolean hidden = (options.indexOf('h') != -1 || options.indexOf('H') != -1);
        boolean alwaysOn = (options.indexOf('a') != -1 || options.indexOf('A') != -1);
        
        // Can not create a non-fixed always-on gate.
        if (alwaysOn && destName.length() == 0) {
        	alwaysOn = false;
        }

        if ((name.length() < 1) || (name.length() > 11) || (getByName(name) != null)) {
            return null;
        }
        if ((network.length() < 1) || (network.length() > 11)) {
            network = Stargate.getDefaultNetwork();
        }
        if (destName.length() > 0) {
            network = "";
        }

        int modX = 0;
        int modZ = 0;
        float rotX = 0f;

        if (idParent.getX() > id.getBlock().getX()) {
            modZ -= 1;
            rotX = 90f;
        } else if (idParent.getX() < id.getBlock().getX()) {
            modZ += 1;
            rotX = 270f;
        } else if (idParent.getZ() > id.getBlock().getZ()) {
            modX += 1;
            rotX = 180f;
        } else if (idParent.getZ() < id.getBlock().getZ()) {
            modX -= 1;
            rotX = 0f;
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

        Portal portal = null;

        Blox button = null;
        // No button on an always-open gate.
        if (!alwaysOn) {
        	button = topleft.modRelative(buttonVector.getRight(), buttonVector.getDepth(), buttonVector.getDistance() + 1, modX, 1, modZ);
        button.setType(BUTTON);
        }
        portal = new Portal(topleft, modX, modZ, rotX, id, button, destName, name, true, network, gate, player.getName(), hidden, alwaysOn);

        // Open always on gate
        if (portal.isAlwaysOn()) {
        	Portal dest = Portal.getByName(destName);
        	if (dest != null)
        		portal.open(true);
        }
        
        // Open any always on gate pointing at this gate
        for (String originName : allPortals) {
        	Portal origin = Portal.getByName(originName);
        	if (origin != null && origin.isAlwaysOn() && origin.getDestinationName().equalsIgnoreCase(portal.getName()) && origin.isVerified()) 
        		origin.open(true);
        }

        saveAllGates();

        return portal;
    }

    public static Portal getByName(String name) {
        return lookupNames.get(name.toLowerCase());
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

    public static void saveAllGates() {
        String loc = Stargate.getSaveLocation();

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(loc, false));

            for (String name : allPortals) {
                Portal portal = Portal.getByName(name);
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

                bw.append(builder.toString());
                bw.newLine();
            }

            bw.close();
        } catch (Exception e) {
            Stargate.log.log(Level.SEVERE, "Exception while writing stargates to " + loc + ": " + e);
        }
    }

    public static void loadAllGates(World world) {
        String location = Stargate.getSaveLocation();

        lookupBlocks.clear();
        lookupNames.clear();
        lookupEntrances.clear();
        allPortals.clear();

        if (new File(location).exists()) {
        	int l = 0;
            try {
                Scanner scanner = new Scanner(new File(location));
                while (scanner.hasNextLine()) {
                	l++;
                    String line = scanner.nextLine().trim();
                    if (line.startsWith("#") || line.isEmpty()) {
                        continue;
                    }
                    String[] split = line.split(":");
                    if (split.length < 8) {
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

                    String fixed = (split.length > 8) ? split[8] : "";
                    String network = (split.length > 9) ? split[9] : Stargate.getDefaultNetwork();
                    String owner = (split.length > 10) ? split[10] : "";
                    boolean hidden = (split.length > 11) ? split[11].equalsIgnoreCase("true") : false;
                    boolean alwaysOn = (split.length > 12) ? split[12].equalsIgnoreCase("true") : false;

                    if (fixed.length() > 0) {
                        network = "";
                    }

                    Portal portal = new Portal(topLeft, modX, modZ, rotX, sign, button, fixed, name, false, network, gate, owner, hidden, alwaysOn);
                    portal.close(true);
                    // Verify portal integrity/register portal
                    if (!portal.isVerified() || !portal.checkIntegrity()) {
                            portal.close(true);
                            portal.unregister();
                            Stargate.log.info("Destroying stargate at " + portal.toString());
                    } else {
                    	portal.drawSign(true);
                    }

                }
                scanner.close();
                
                // Open any always-on gates. Do this here as it should be more efficient than in the loop.
                int OpenCount = 0;
                for (String srcName : allPortals) {
                	Portal portal = Portal.getByName(srcName);
                	if (portal == null) continue;
                	if (!portal.isAlwaysOn()) continue;
					if (!portal.wasVerified()) continue;
                	
                	Portal dest = portal.getDestination();
                	if (dest != null) {
                		portal.open(true);
                		OpenCount++;
                	}
                }
                Stargate.log.info("[Stargate] Loaded " + allPortals.size() + " stargates with " + OpenCount + " set as always-on");
            } catch (Exception e) {
                Stargate.log.log(Level.SEVERE, "Exception while reading stargates from " + location + ": " + l);
                e.printStackTrace();
            }
        }
    }
    
    public static void closeAllGates() {
    	Stargate.log.info("Closing all stargates.");
    	for (String name : allPortals) {
    		Portal p = Portal.getByName(name);
    		p.close(true);
    	}
    }

    public static String filterName(String input) {
        return input.replaceAll("[\\|:#]", "").trim();
    }

    public static Portal getNextOpen() {
        for (String name : allPortals) {
            Portal tmp = getByName(name);
            if (tmp.pastGrace()) {
                return tmp;
            }
        }
        return null;
    }
}
