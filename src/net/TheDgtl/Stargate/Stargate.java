package net.TheDgtl.Stargate;

import java.io.File;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRightClickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleListener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

// Permissions
import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijiko.permissions.PermissionHandler;

/**
 * Stargate.java - Plug-in for hey0's minecraft mod.
 * @author Shaun (sturmeh)
 * @author Dinnerbone
 */
public class Stargate extends JavaPlugin implements Runnable {
	// Permissions
	public static PermissionHandler Permissions = null;
	
    private final bListener blockListener = new bListener();
    private final pListener playerListener = new pListener();
    private final vListener vehicleListener = new vListener();
    public static Logger log;
    private Configuration config;
    private PluginManager pm;
    private static String portalFile;
    private static String gateFolder;
    private static String teleMsg = "Teleported";
    private static String regMsg = "Gate Created";
    private static String dmgMsg = "Gate Destroyed";
    private static String denyMsg = "Access Denied";
    private static String invMsg = "Invalid Destination"; 
    private static String blockMsg = "Destination Blocked";
    private static String defNetwork = "central";
    private static SynchronousQueue<Portal> slip = new SynchronousQueue<Portal>();
    //private HashMap<Integer, Location> vehicles = new HashMap<Integer, Location>();
    
    // Threading stuff
    private Thread clock;
    private long interval = 0;

	public Stargate(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
    	super(pluginLoader, instance, desc, folder, plugin, cLoader);
    	log = Logger.getLogger("Minecraft");
    	
    	// Set portalFile and gateFolder to the plugin folder as defaults.
    	portalFile = folder + File.separator + "stargate.db";
    	gateFolder = folder + File.separator + "gates" + File.separator;
    }
	
    public void onDisable() {
    	Portal.closeAllGates();
    }

    public void onEnable() {
        PluginDescriptionFile pdfFile = this.getDescription();
        log.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " is enabled.");
        
    	pm = getServer().getPluginManager();
    	config = this.getConfiguration();
		if (clock == null)
			clock = new Thread(this);
		
    	pm.registerEvent(Event.Type.BLOCK_FLOW, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Priority.Normal, this);
		
    	this.reloadConfig();
    	this.migrate();
    	this.reloadGates();
    	this.setupPermissions();
    	
    	pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
    	
    	pm.registerEvent(Event.Type.BLOCK_RIGHTCLICKED, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.VEHICLE_MOVE, vehicleListener, Priority.Normal, this);
    	//pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Normal, this);
    	
        setInterval(160); // 8 seconds.

		clock.start();
    }

    public void reloadConfig() {
    	config.load();
        portalFile = config.getString("portal-save-location", portalFile);
        gateFolder = config.getString("gate-folder", gateFolder);
        teleMsg = config.getString("teleport-message", teleMsg);
        regMsg = config.getString("portal-create-message", regMsg);
        dmgMsg = config.getString("portal-destroy-message", dmgMsg);
        denyMsg = config.getString("not-owner-message", denyMsg);
        invMsg = config.getString("not-selected-message", invMsg);
        blockMsg = config.getString("other-side-blocked-message", blockMsg);
        defNetwork = config.getString("default-gate-network", defNetwork).trim();
        saveConfig();
    }

	public void saveConfig() {
        config.setProperty("portal-save-location", portalFile);
        config.setProperty("gate-folder", gateFolder);
        config.setProperty("teleport-message", teleMsg);
        config.setProperty("portal-create-message", regMsg);
        config.setProperty("portal-destroy-message", dmgMsg);
        config.setProperty("not-owner-message", denyMsg);
        config.setProperty("not-selected-message", invMsg);
        config.setProperty("other-side-blocked-message", blockMsg);
        config.setProperty("default-gate-network", defNetwork);
        config.save();
	}

	public void reloadGates() {
        Gate.loadGates();
        Portal.loadAllGates(this.getServer().getWorlds().get(0));
	}
	
	private void migrate() {
    	// Migrate old stargates if applicable.
        File oldFile = new File("stargates/locations.dat");
        if (oldFile.exists()) {
        	Stargate.log.info("[Stargate] Migrated existing locations.dat");
            oldFile.renameTo(new File(portalFile));
        }
        
        // Migrate old gates if applicaple
        File oldDir = new File("stargates");
        if (oldDir.exists()) {
        	File newDir = new File(gateFolder);
        	if (!newDir.exists()) newDir.mkdirs();
            for (File file : oldDir.listFiles(new Gate.StargateFilenameFilter())) {
            	Stargate.log.info("[Stargate] Migrating existing gate " + file.getName());
            	file.renameTo(new File(gateFolder + file.getName()));
            }
        }
    }

    public synchronized void doWork() {
        Portal open = Portal.getNextOpen();

        if (open != null) {
            try {
                slip.put(open);
            } catch (InterruptedException e) {
            }
        }
    }

    public void threadSafeOperation() {
        Portal open = slip.poll();
        if (open != null) {
            if (open.isOpen()) {
                open.close(false);
            } else if (open.isActive()) {
                open.deactivate();
            }
        }
    }

    public static String getSaveLocation() {
        return portalFile;
    }

    public static String getDefaultNetwork() {
        return defNetwork;
    }

    private void onButtonPressed(Player player, Portal gate) {
        Portal destination = gate.getDestination();

        if (!gate.isOpen()) {
        	if ((!gate.isFixed()) && (gate.getActivePlayer() != player)) {
        		gate.deactivate();
                if (!denyMsg.isEmpty()) {
                    player.sendMessage(ChatColor.RED + denyMsg);
                }
        	} else if ((destination == null) || (destination == gate)) {
                if (!invMsg.isEmpty()) {
                    player.sendMessage(ChatColor.RED + invMsg);
                }
            } else if ((destination.isOpen()) && (!destination.isFixed())) {
                if (!blockMsg.isEmpty()) {
                    player.sendMessage(ChatColor.RED + blockMsg);
                }
            } else {
                gate.open(player, false);
            }
        } else {
            gate.close(false);
        }
    }
    
    public void setupPermissions() {
    	Plugin perm = pm.getPlugin("Permissions");

    	    if(perm != null) {
    	    	Stargate.Permissions = ((Permissions)perm).getHandler();
    	    } else {
    	    	log.info("[" + this.getDescription().getName() + "] Permission system not enabled. Disabling plugin.");
			pm.disablePlugin(this);
    	}
    }
    
    private class vListener extends VehicleListener {
        @Override
        public void onVehicleMove(VehicleMoveEvent event) {
        	Entity passenger = event.getVehicle().getPassenger();
        	Vehicle vehicle = event.getVehicle();
        	
        	Portal portal = Portal.getByEntrance(event.getTo());
        	if (portal != null && portal.isOpen()) {
        		if (passenger instanceof Player) {
        	Player player = (Player)event.getVehicle().getPassenger();
        			if (!portal.isOpenFor(player)) {
        				player.sendMessage(ChatColor.RED + denyMsg);
        				return;
            }
        			Portal dest = portal.getDestination();
        			if (dest == null) return;
        			dest.teleport(vehicle, portal);

        			if (!teleMsg.isEmpty())
        				player.sendMessage(ChatColor.BLUE + teleMsg);
                            portal.close(false);
                    } else {
        			
                    }
                }
            }
        }
    
    private class pListener extends PlayerListener {
        @Override
        public void onPlayerMove(PlayerMoveEvent event) {
            threadSafeOperation();
            Player player = event.getPlayer();
            Portal portal = Portal.getByEntrance(event.getTo());

            if ((portal != null) && (portal.isOpen())) {
                if (portal.isOpenFor(player)) {
                    Portal destination = portal.getDestination();

                    if (destination != null) {
                        if (!teleMsg.isEmpty()) {
                            player.sendMessage(ChatColor.BLUE + teleMsg);
                        }

                        destination.teleport(player, portal, event);
                        portal.close(false);
                    }
                } else {
                    if (!denyMsg.isEmpty()) {
                        player.sendMessage(ChatColor.RED + denyMsg);
                    }
                }
            }
        }
    }

    private class bListener extends BlockListener {
    	@Override
    	public void onBlockPlace(BlockPlaceEvent event) {
    		// Stop player from placing a block touching a portals controls
    		if (event.getBlockAgainst().getType() == Material.STONE_BUTTON || 
    			event.getBlockAgainst().getType() == Material.WALL_SIGN) {
    			Portal portal = Portal.getByBlock(event.getBlockAgainst());
    			if (portal != null) event.setCancelled(true);
    		}
    	}

        @Override
        public void onBlockRightClick(BlockRightClickEvent event) {
        	Player player = event.getPlayer();
        	Block block = event.getBlock();
            if (block.getType() == Material.WALL_SIGN) {
                Portal portal = Portal.getByBlock(block);
                // Cycle through a stargates locations
                if (portal != null) {
                	if (Stargate.Permissions.has(player, "stargate.use")) {
	                    if ((!portal.isOpen()) && (!portal.isFixed())) {
	                        portal.cycleDestination(player);
	                    }
                	}
                }
                
                // Check if the player is initializing a stargate
                if (portal == null && Stargate.Permissions.has(player, "stargate.create")) {
    	            SignPost sign = new SignPost(new Blox(block));
    	                portal = Portal.createPortal(sign, player);
    	    
	                if (portal != null && !regMsg.isEmpty()) {
	                    player.sendMessage(ChatColor.GREEN + regMsg);
    	                }

    	                if (portal == null) return;
    	                log.info("Initialized stargate: " + portal.getName());
                        portal.drawSign(true);
    	            }
                }
            
            // Implement right-click to toggle a stargate, gets around spawn protection problem.
            if ((block.getType() == Material.STONE_BUTTON)) {
            	if (Stargate.Permissions.has(player, "stargate.use")) {
            		Portal portal = Portal.getByBlock(block);
            		if (portal != null) {
            			onButtonPressed(player, portal);
            		}
            	}
            }
        }

        @Override
        public void onBlockDamage(BlockDamageEvent event) {
        	Player player = event.getPlayer();
        	Block block = event.getBlock();
            if (block.getType() != Material.WALL_SIGN && block.getType() != Material.OBSIDIAN && block.getType() != Material.STONE_BUTTON) {
                return;
            }
            
            Portal portal = Portal.getByBlock(block);
            if (portal == null) return;
            
            if (!Stargate.Permissions.has(player, "stargate.destroy")) {
            	event.setCancelled(true);
            	return;
            }

        	if (event.getDamageLevel() == BlockDamageLevel.BROKEN) {
                portal.unregister();
                if (!dmgMsg.isEmpty()) {
                    player.sendMessage(ChatColor.RED + dmgMsg);
                }
            }
        }

        @Override
        public void onBlockPhysics(BlockPhysicsEvent event) {
        	Block block = event.getBlock();
            if (block.getType() == Material.PORTAL) {
            	event.setCancelled((Portal.getByEntrance(block) != null));
            }
        }

        @Override
        public void onBlockFlow(BlockFromToEvent event) {
            Portal portal = Portal.getByEntrance(event.getBlock());

            if (portal != null) {
            	event.setCancelled((event.getBlock().getY() == event.getToBlock().getY()));
            }
        }
    }
    
    public void run() {
    	while (isEnabled()) {
    		try {
    			while (interval <= 0)
    				Thread.sleep(50); // Thread is dormant
    			for (long i = 0; i < interval && isEnabled(); i++)
    				Thread.sleep(50); // Sleep for an in-game second?
    			if (isEnabled()) doWork();
    		} catch (InterruptedException e) {}
    	}
    }
    
	public void setInterval(long interval) {
		this.interval = interval;
	}
}
