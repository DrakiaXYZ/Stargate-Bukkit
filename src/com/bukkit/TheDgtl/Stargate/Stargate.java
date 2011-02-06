package com.bukkit.TheDgtl.Stargate;

import java.io.File;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRightClickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
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
    //private final vListener vehicleListener = new vListener();
    public static Logger log;
    private Configuration config;
    private static String gateSaveLocation = "stargates/locations.dat";
    private static String teleportMessage = "You feel weightless as the portal carries you to new land...";
    private static String registerMessage = "You feel a slight tremble in the ground around the portal...";
    private static String destroyzMessage = "You feel a great shift in energy, as it leaves the portal...";
    private static String noownersMessage = "You feel a great power, yet feel a lack of belonging here...";
    private static String unselectMessage = "You seem to want to go somewhere, but it's still a secret..."; 
    private static String collisionMessage = "You anticipate a great surge, but it appears it's blocked...";
    private static String defaultNetwork = "central";
    private static SynchronousQueue<Portal> slip = new SynchronousQueue<Portal>();
    //private HashMap<Integer, Location> vehicles = new HashMap<Integer, Location>();
    
    // Threading stuff
    private Thread clock;
    private long interval = 0;

	public Stargate(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
    	super(pluginLoader, instance, desc, folder, plugin, cLoader);
    	log = Logger.getLogger("Minecraft");
    	
    	// Migrate old settings if applicable.
        File oldFile = new File("stargates.txt");
        if (oldFile.exists())
            oldFile.renameTo(new File(gateSaveLocation));
    }
	
    public void onDisable() {
    	Portal.closeAllGates();
    }

    public void onEnable() {
        PluginDescriptionFile pdfFile = this.getDescription();
        log.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " is enabled.");
        
    	PluginManager pm = getServer().getPluginManager();
    	config = this.getConfiguration();
		if (clock == null)
			clock = new Thread(this);
    	this.reloadConfig();
    	this.setupPermissions();
    	
    	pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
    	
    	pm.registerEvent(Event.Type.BLOCK_RIGHTCLICKED, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.BLOCK_FLOW, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Priority.Normal, this);
    	
    	//pm.registerEvent(Event.Type.VEHICLE_MOVE, vehicleListener, Priority.Normal, this);
    	
        setInterval(160); // 8 seconds.

		clock.start();
    }

    public void reloadConfig() {
    	config.load();
        gateSaveLocation = config.getString("portal-save-location", gateSaveLocation);
        teleportMessage = config.getString("teleport-message", teleportMessage);
        registerMessage = config.getString("portal-create-message", registerMessage);
        destroyzMessage = config.getString("portal-destroy-message", destroyzMessage);
        noownersMessage = config.getString("not-owner-message", noownersMessage);
        unselectMessage = config.getString("not-selected-message", unselectMessage);
        collisionMessage = config.getString("other-side-blocked-message", collisionMessage);

        defaultNetwork = config.getString("default-gate-network", defaultNetwork).trim();

        Gate.loadGates();
        Portal.loadAllGates(this.getServer().getWorlds()[0]);
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
        return gateSaveLocation;
    }

    public static String getDefaultNetwork() {
        return defaultNetwork;
    }

    private void onButtonPressed(Player player, Portal gate) {
        Portal destination = gate.getDestination();

        if (!gate.isOpen()) {
        	if ((!gate.isFixed()) && (gate.getActivePlayer() != player)) {
        		gate.deactivate();
                if (!noownersMessage.isEmpty()) {
                    player.sendMessage(ChatColor.RED + noownersMessage);
                }
        	} else if ((destination == null) || (destination == gate)) {
                if (!unselectMessage.isEmpty()) {
                    player.sendMessage(ChatColor.RED + unselectMessage);
                }
            } else if ((destination.isOpen()) && (!destination.isFixed())) {
                if (!collisionMessage.isEmpty()) {
                    player.sendMessage(ChatColor.RED + collisionMessage);
                }
            } else {
                gate.open(player, false);
            }
        } else {
            gate.close(false);
        }
    }
    
    public void setupPermissions() {
    	Plugin perm = this.getServer().getPluginManager().getPlugin("Permissions");

    	if(Stargate.Permissions == null) {
    	    if(perm != null) {
    	    	Stargate.Permissions = ((Permissions)perm).getHandler();
    	    } else {
    	    	log.info("[" + this.getDescription().getName() + "] Permission system not enabled. Disabling plugin.");
    			this.getServer().getPluginManager().disablePlugin(this);
    	    }
    	}
    }
    
/*    private class vListener extends VehicleListener {
        @Override
        public void onVehicleMove(VehicleMoveEvent event) {
        	Player player = (Player)event.getVehicle().getPassenger();

            Location lookup = vehicles.get(vehicle.getId());

            if (lookup != null) {
                vehicle.setMotion(lookup.x, lookup.y, lookup.z);
                vehicles.remove(vehicle.getId());
            }

            if (player != null) {
                Portal portal = Portal.getByEntrance(etc.getServer().getBlockAt(x, y, z));

                if ((portal != null) && (portal.isOpen())) {
                    if (portal.isOpenFor(player)) {
                        Portal destination = portal.getDestination();

                        if (destination != null) {
                            if (!teleportMessage.isEmpty()) {
                                player.sendMessage(Colors.Blue + teleportMessage);
                            }

                            vehicles.put(vehicle.getId(), destination.teleport(vehicle, portal));

                            portal.close(false);
                        }
                    } else {
                        if (!noownersMessage.isEmpty()) {
                            player.sendMessage(Colors.Red + noownersMessage);
                        }
                    }
                }
            }
        }
    }*/
    
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
                        if (!teleportMessage.isEmpty()) {
                            player.sendMessage(ChatColor.BLUE + teleportMessage);
                        }

                        destination.teleport(player, portal, event);
                        portal.close(false);
                    }
                } else {
                    if (!noownersMessage.isEmpty()) {
                        player.sendMessage(ChatColor.RED + noownersMessage);
                    }
                }
            }
        }
    }

    private class bListener extends BlockListener {

        @Override
        public void onBlockRightClick(BlockRightClickEvent event) {
        	Player player = event.getPlayer();
        	Block block = event.getBlock();
        	
            if ((block.getType() == Material.SIGN_POST) || (block.getType() == Material.WALL_SIGN)) {
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
    	            portal = Portal.getByBlock(sign.getParent());
    	            if (portal == null) {
    		            log.info("Initializing stargate");
    	                portal = Portal.createPortal(sign, player);
    	    
    	                if (portal != null && !registerMessage.isEmpty()) {
    	                    player.sendMessage(ChatColor.GREEN + registerMessage);
    	                }

    	                if (portal == null) return;
    	                log.info("Initialized stargate: " + portal.getName());
                        portal.drawSign(true);
    	            }
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
            if (portal == null) portal = Portal.getByEntrance(block);
            if (portal == null) return;
            if (!Stargate.Permissions.has(player, "stargate.destroy")) {
            	event.setCancelled(true);
            	return;
            }

        	if (event.getDamageLevel() == BlockDamageLevel.BROKEN) {
                portal.unregister();
                if (!destroyzMessage.isEmpty()) {
                    player.sendMessage(ChatColor.RED + destroyzMessage);
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
