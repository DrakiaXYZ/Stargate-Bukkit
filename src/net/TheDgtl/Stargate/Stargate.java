package net.TheDgtl.Stargate;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockDamageLevel;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRightClickEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleListener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.WorldEvent;
import org.bukkit.event.world.WorldListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

// Permissions
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * Stargate.java - Plug-in for hey0's minecraft mod.
 * @author Shaun (sturmeh)
 * @author Dinnerbone
 */
public class Stargate extends JavaPlugin {
	// Permissions
	private Permissions permissions = null;
	private double permVersion = 0;
	
    private final bListener blockListener = new bListener(this);
    private final pListener playerListener = new pListener();
    private final vListener vehicleListener = new vListener();
    private final wListener worldListener = new wListener();
    public static Logger log;
    private Configuration config;
    private PluginManager pm;
    
    private static String portalFolder;
    private static String gateFolder;
    private static String teleMsg = "Teleported";
    private static String regMsg = "Gate Created";
    private static String dmgMsg = "Gate Destroyed";
    private static String denyMsg = "Access Denied";
    private static String invMsg = "Invalid Destination"; 
    private static String blockMsg = "Destination Blocked";
    private static String defNetwork = "central";
    private static int activeLimit = 10;
    private static int openLimit = 10;
    
    public static ConcurrentLinkedQueue<Portal> openList = new ConcurrentLinkedQueue<Portal>();
    public static ConcurrentLinkedQueue<Portal> activeList = new ConcurrentLinkedQueue<Portal>();
    //private HashMap<Integer, Location> vehicles = new HashMap<Integer, Location>();
	
    public void onDisable() {
    	Portal.closeAllGates();
    	Portal.clearGates();
    }

    public void onEnable() {
        PluginDescriptionFile pdfFile = this.getDescription();
        pm = getServer().getPluginManager();
        config = this.getConfiguration();
    	log = Logger.getLogger("Minecraft");
    	
    	// Set portalFile and gateFolder to the plugin folder as defaults.
    	portalFolder = getDataFolder() + "/portals";
    	gateFolder = getDataFolder() + "/gates/";
    	
        
        log.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " is enabled.");
		
    	pm.registerEvent(Event.Type.BLOCK_FLOW, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Priority.Normal, this);
		
    	this.reloadConfig();
    	this.migrate();
    	this.reloadGates();
    	
    	if (!this.setupPermissions()) {
    		log.info("[Stargate] Permissions not loaded, using defaults");
    	} else {
    		if (permissions != null)
    			log.info("[Stargate] Using Permissions " + permVersion + " (" + Permissions.version + ") for permissions");
    	}
    	
    	pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
    	
    	pm.registerEvent(Event.Type.BLOCK_RIGHTCLICKED, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.VEHICLE_MOVE, vehicleListener, Priority.Normal, this);
    	pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Normal, this);
    	
    	pm.registerEvent(Event.Type.WORLD_LOADED, worldListener, Priority.Normal, this);
    	
    	getServer().getScheduler().scheduleSyncRepeatingTask(this, new SGThread(), 0L, 100L);
    }

	public void reloadConfig() {
    	config.load();
        portalFolder = config.getString("portal-folder", portalFolder);
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
        config.setProperty("portal-folder", portalFolder);
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
		Gate.loadGates(gateFolder);
		// Replace nethergate.gate if it doesn't have an exit point.
		if (Gate.getGateByName("nethergate.gate").getExit() == null) {
			Gate.populateDefaults(gateFolder);
		}
		
		for (World world : getServer().getWorlds()) {
			Portal.loadAllGates(world);
		}
	}
	
	private void migrate() {
		// Only migrate if new file doesn't exist.
		File newPortalDir = new File(portalFolder);
		if (!newPortalDir.exists()) {
			newPortalDir.mkdirs();
		}
		File newFile = new File(portalFolder, getServer().getWorlds().get(0).getName() + ".db");
		if (!newFile.exists()) {
	        // Migrate not-so-old stargate db
	        File oldishFile = new File("plugins/Stargate/stargate.db");
	        if (oldishFile.exists()) {
	        	Stargate.log.info("[Stargate] Migrating existing stargate.db");
	        	oldishFile.renameTo(newFile);
	        }
		}
        
        // Migrate old gates if applicaple
        File oldDir = new File("stargates");
        if (oldDir.exists()) {
        	File newDir = new File(gateFolder);
        	if (!newDir.exists()) newDir.mkdirs();
            for (File file : oldDir.listFiles(new Gate.StargateFilenameFilter())) {
            	Stargate.log.info("[Stargate] Migrating existing gate " + file.getName());
            	file.renameTo(new File(gateFolder, file.getName()));
            }
        }
	}

    public static String getSaveLocation() {
        return portalFolder;
    }

    public static String getDefaultNetwork() {
        return defNetwork;
    }

    private void onButtonPressed(Player player, Portal gate) {
        Portal destination = gate.getDestination();

        if (!gate.isOpen()) {
        	if ((!gate.isFixed()) && gate.isActive() &&  (gate.getActivePlayer() != player)) {
        		gate.deactivate();
                if (!denyMsg.isEmpty()) {
                    player.sendMessage(ChatColor.RED + denyMsg);
                }
        	} else if (gate.isPrivate() && !gate.getOwner().equals(player.getName()) && !hasPerm(player, "stargate.private", player.isOp())) {
            	if (!denyMsg.isEmpty()) {
            		player.sendMessage(ChatColor.RED + denyMsg);
            	}
        	} else if ((destination == null) || (destination == gate)) {
                if (!invMsg.isEmpty()) {
                    player.sendMessage(ChatColor.RED + invMsg);
                }
            } else if ((destination.isOpen()) && (!destination.isAlwaysOn())) {
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
    
	/*
	 * Find what Permissions plugin we're using and enable it.
	 */
	private boolean setupPermissions() {
		Plugin perm;
		perm = pm.getPlugin("Permissions");
		// We're running Permissions
		if (perm != null) {
			if (!perm.isEnabled()) {
				pm.enablePlugin(perm);
			}
			permissions = (Permissions)perm;
			try {
				String[] permParts = Permissions.version.split("\\.");
				permVersion = Double.parseDouble(permParts[0] + "." + permParts[1]);
			} catch (Exception e) {
				log.info("Could not determine Permissions version: " + Permissions.version);
				return true;
			}
			return true;
		}
		// Permissions not loaded
		return false;
	}

	/*
	 * Check whether the player has the given permissions.
	 */
	public boolean hasPerm(Player player, String perm, boolean def) {
		if (permissions != null) {
			return permissions.getHandler().has(player, perm);
		} else {
			return def;
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
    	Stargate stargate;
    	
    	bListener(Stargate stargate) {
    		this.stargate = stargate;
    	}
    	
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
    	public void onSignChange(SignChangeEvent event) {
    		Player player = event.getPlayer();
    		Block block = event.getBlock();
    		if (block.getType() != Material.WALL_SIGN) return;
    		
    		// Initialize a stargate
            if (hasPerm(player, "stargate.create", player.isOp())) {
	            SignPost sign = new SignPost(new Blox(block));
	            // Set sign text so we can create a gate with it.
	            sign.setText(0, event.getLine(0));
	            sign.setText(1, event.getLine(1));
	            sign.setText(2, event.getLine(2));
	            sign.setText(3, event.getLine(3));
                Portal portal = Portal.createPortal(sign, player);
                if (portal == null) return;
                
                if (!regMsg.isEmpty()) {
                    player.sendMessage(ChatColor.GREEN + regMsg);
                }
                log.info("[Stargate] Initialized stargate: " + portal.getName());
                portal.drawSign();
                // Set event text so our new sign is instantly initialized
                event.setLine(0, sign.getText(0));
                event.setLine(1, sign.getText(1));
                event.setLine(2, sign.getText(2));
                event.setLine(3, sign.getText(3));
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
                	if (hasPerm(player, "stargate.use", true)) {
	                    if ((!portal.isOpen()) && (!portal.isFixed())) {
	                        portal.cycleDestination(player, stargate);
	                    }
                	} else {
                		if (!denyMsg.isEmpty()) {
                			player.sendMessage(denyMsg);
                		}
                	}
                }
            }
            
            // Implement right-click to toggle a stargate, gets around spawn protection problem.
            if ((block.getType() == Material.STONE_BUTTON)) {
            	if (hasPerm(player, "stargate.use", true)) {
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
        	// Check if we're pushing a button.
        	if (block.getType() == Material.STONE_BUTTON && event.getDamageLevel() == BlockDamageLevel.STARTED) {
            	if (hasPerm(player, "stargate.use", true)) {
            		Portal portal = Portal.getByBlock(block);
            		if (portal != null) {
            			onButtonPressed(player, portal);
            		}
            	}
        	}
        }
        
        @Override
        public void onBlockBreak(BlockBreakEvent event) {
        	Block block = event.getBlock();
        	Player player = event.getPlayer();
            if (block.getType() != Material.WALL_SIGN && block.getType() != Material.STONE_BUTTON && Gate.getGatesByControlBlock(block).length == 0) {
                return;
            }

            Portal portal = Portal.getByBlock(block);
            if (portal == null) return;
            
            if (!hasPerm(player, "stargate.destroy", player.isOp())) {
            	event.setCancelled(true);
            	return;
            }

            portal.unregister();
            if (!dmgMsg.isEmpty()) {
                player.sendMessage(ChatColor.RED + dmgMsg);
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
    
    private class wListener extends WorldListener {
    	@Override
    	public void onWorldLoaded(WorldEvent event) {
    		World w = event.getWorld();
    		// We have to make sure the world is actually loaded. This gets called twice for some reason.
    		if (w.getBlockAt(w.getSpawnLocation()).getWorld() != null) {
    			Portal.loadAllGates(w);
    		}
    	}
    }
    
    private class SGThread implements Runnable {
	    public void run() {
	    	long time = System.currentTimeMillis() / 1000;
	    	// Close open portals
	    	for (Iterator<Portal> iter = Stargate.openList.iterator(); iter.hasNext();) {
	    		Portal p = iter.next();
	    		if (time > p.getOpenTime() + Stargate.openLimit) {
	    			p.close(false);
	    			iter.remove();
	    		}
	    	}
	    	// Deactivate active portals
	    	for (Iterator<Portal> iter = Stargate.activeList.iterator(); iter.hasNext();) {
	    		Portal p = iter.next();
	    		if (time > p.getOpenTime() + Stargate.activeLimit) {
	    			p.deactivate();
	    			iter.remove();
	    		}
	    	}
	    }
    }
}
