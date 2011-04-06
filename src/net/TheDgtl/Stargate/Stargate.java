package net.TheDgtl.Stargate;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.event.vehicle.VehicleListener;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

// Permissions
import com.nijikokun.bukkit.Permissions.Permissions;
// iConomy
import com.nijiko.coelho.iConomy.iConomy;

/**
 * Stargate.java - A customizeable portal plugin for Bukkit
 * @author Shaun (sturmeh)
 * @author Dinnerbone
 * @author Steven "Drakia" Scott
 */
public class Stargate extends JavaPlugin {
	// Permissions
	private static Permissions permissions = null;
	
	private final bListener blockListener = new bListener();
	private final pListener playerListener = new pListener();
	private final vListener vehicleListener = new vListener();
	private final wListener worldListener = new wListener();
	private final eListener entityListener = new eListener();
	private final sListener serverListener = new sListener();
	
	public static Logger log;
	private Configuration config;
	private PluginManager pm;
	public static Server server;
	public static Stargate stargate;
	
	private static String portalFolder;
	private static String gateFolder;
	private static String teleMsg = "Teleported";
	private static String regMsg = "Gate Created";
	private static String dmgMsg = "Gate Destroyed";
	private static String denyMsg = "Access Denied";
	private static String invMsg = "Invalid Destination"; 
	private static String blockMsg = "Destination Blocked";
	private static String defNetwork = "central";
	private static boolean destroyExplosion = false;
	public static boolean networkFilter = false;
	public static boolean worldFilter = false;
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
		Stargate.server = getServer();
		Stargate.stargate = this;
		
		// Set portalFile and gateFolder to the plugin folder as defaults.
		portalFolder = getDataFolder() + "/portals";
		gateFolder = getDataFolder() + "/gates/";
		
		log.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " is enabled.");
		
		pm.registerEvent(Event.Type.BLOCK_FROMTO, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Priority.Normal, this);
		
		this.reloadConfig();
		this.migrate();
		this.reloadGates();
		
		// Check to see if iConomy/Permissions is loaded yet.
		permissions = (Permissions)checkPlugin("Permissions");
		iConomyHandler.iconomy = (iConomy)checkPlugin("iConomy");
		
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		
		//pm.registerEvent(Event.Type.BLOCK_RIGHTCLICKED, blockListener, Priority.Normal, this);
		//pm.registerEvent(Event.Type.BLOCK_PLACED, blockListener, Priority.Normal, this);
		//pm.registerEvent(Event.Type.BLOCK_DAMAGED, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.VEHICLE_MOVE, vehicleListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Normal, this);
		
		pm.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Normal, this);
		
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Priority.Normal, this);
		
		// iConomy Loading
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Priority.Monitor, this);
		
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
		destroyExplosion = config.getBoolean("destroyexplosion", destroyExplosion);
		networkFilter = config.getBoolean("networkfilter", networkFilter);
		worldFilter = config.getBoolean("worldfilter", worldFilter);
		// iConomy
		iConomyHandler.useiConomy = config.getBoolean("useiconomy", iConomyHandler.useiConomy);
		iConomyHandler.createCost = config.getInt("createcost", iConomyHandler.createCost);
		iConomyHandler.destroyCost = config.getInt("destroycost", iConomyHandler.destroyCost);
		iConomyHandler.useCost = config.getInt("usecost", iConomyHandler.useCost);
		iConomyHandler.inFundMsg = config.getString("not-enough-money-message", iConomyHandler.inFundMsg);
		
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
		config.setProperty("destroyexplosion", destroyExplosion);
		config.setProperty("networkfilter", networkFilter);
		config.setProperty("worldfilter", worldFilter);
		// iConomy
		config.setProperty("useiconomy", iConomyHandler.useiConomy);
		config.setProperty("createcost", iConomyHandler.createCost);
		config.setProperty("destroycost", iConomyHandler.destroyCost);
		config.setProperty("usecost", iConomyHandler.useCost);
		config.setProperty("not-enough-money-message", iConomyHandler.inFundMsg);
		
		config.save();
	}
	
	public void reloadGates() {
		Gate.loadGates(gateFolder);
		// Replace nethergate.gate if it doesn't have an exit point.
		if (Gate.getGateByName("nethergate.gate") == null || Gate.getGateByName("nethergate.gate").getExit() == null) {
			Gate.populateDefaults(gateFolder);
		}
		log.info("[Stargate] Loaded " + Gate.getGateCount() + " gate layouts");
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
			newFile.getParentFile().mkdirs();
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
			if (!gate.isFree() && !hasPerm(player, "stargate.free", player.isOp()) && 
					iConomyHandler.useiConomy() && iConomyHandler.getBalance(player.getName()) < gate.getGate().getUseCost()) {
				player.sendMessage(ChatColor.RED + iConomyHandler.inFundMsg);
			} else if ((!gate.isFixed()) && gate.isActive() &&  (gate.getActivePlayer() != player)) {
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
	 * Check whether the player has the given permissions.
	 */
	public static boolean hasPerm(Player player, String perm, boolean def) {
		if (permissions != null) {
			return permissions.getHandler().has(player, perm);
		} else {
			return def;
		}
	}
	
	/*
	 * Check if a plugin is loaded/enabled already. Returns the plugin if so, null otherwise
	 */
	private Plugin checkPlugin(String p) {
		Plugin plugin = pm.getPlugin(p);
		return checkPlugin(plugin);
	}
	
	private Plugin checkPlugin(Plugin plugin) {
		if (plugin != null && plugin.isEnabled()) {
			log.info("[Stargate] Found " + plugin.getDescription().getName() + " (v" + plugin.getDescription().getVersion() + ")");
			return plugin;
		}
		return null;
	}
	
	private class vListener extends VehicleListener {
		@Override
		public void onVehicleMove(VehicleMoveEvent event) {
			Entity passenger = event.getVehicle().getPassenger();
			Vehicle vehicle = event.getVehicle();
			
			Portal portal = Portal.getByEntrance(event.getTo());
			if (portal == null || !portal.isOpen()) return;
			
			if (passenger instanceof Player) {
				Player player = (Player)passenger;
				if (!portal.isOpenFor(player)) {
					player.sendMessage(ChatColor.RED + denyMsg);
					return;
				}
				Portal dest = portal.getDestination();
				if (dest == null) return;
				
				if (portal.isFree() || !iConomyHandler.useiConomy() || hasPerm(player, "stargate.free", player.isOp()) || 
						iConomyHandler.chargePlayer(player.getName(), null, portal.getGate().getUseCost())) {
					if (!portal.isFree() && iConomyHandler.useiConomy()) {
						player.sendMessage(ChatColor.GREEN + "Deducted " + iConomy.getBank().format(portal.getGate().getUseCost()));
					}
					if (!teleMsg.isEmpty()) {
						player.sendMessage(ChatColor.BLUE + teleMsg);
					}
					dest.teleport(vehicle, portal);
				} else {
					if (!iConomyHandler.inFundMsg.isEmpty()) {
						player.sendMessage(ChatColor.RED + iConomyHandler.inFundMsg);
					}
				}
				portal.close(false);
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
						if (portal.isFree() || !iConomyHandler.useiConomy() || hasPerm(player, "stargate.free", player.isOp()) || 
								iConomyHandler.chargePlayer(player.getName(), null, portal.getGate().getUseCost())) {
							if (!portal.isFree() && iConomyHandler.useiConomy()) {
								player.sendMessage(ChatColor.GREEN + "Deducted " + iConomy.getBank().format(portal.getGate().getUseCost()));
							}
							if (!teleMsg.isEmpty()) {
								player.sendMessage(ChatColor.BLUE + teleMsg);
							}
	
							destination.teleport(player, portal, event);
						} else {
							if (!iConomyHandler.inFundMsg.isEmpty()) {
								player.sendMessage(ChatColor.RED + iConomyHandler.inFundMsg);
							}
						}
						portal.close(false);
					}
				} else {
					if (!denyMsg.isEmpty()) {
						player.sendMessage(ChatColor.RED + denyMsg);
					}
				}
			}
		}
		
		@Override
		public void onPlayerInteract(PlayerInteractEvent event) {
			Player player = event.getPlayer();
			Block block = event.getClickedBlock();
			
			// Right click
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (block.getType() == Material.WALL_SIGN) {
					Portal portal = Portal.getByBlock(block);
					// Cycle through a stargates locations
					if (portal != null) {
						if (!hasPerm(player, "stargate.use", true) ||
							(networkFilter && !hasPerm(player, "stargate.network." + portal.getNetwork(), player.isOp()))) {
							if (!denyMsg.isEmpty()) {
								player.sendMessage(denyMsg);
							}
							return;
						}
						
						if ((!portal.isOpen()) && (!portal.isFixed())) {
							portal.cycleDestination(player);
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
			
			// Left click
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				// Check if we're pushing a button.
				if (block.getType() == Material.STONE_BUTTON) {
					if (hasPerm(player, "stargate.use", true)) {
						Portal portal = Portal.getByBlock(block);
						if (portal != null) {
							onButtonPressed(player, portal);
						}
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
		public void onSignChange(SignChangeEvent event) {
			Player player = event.getPlayer();
			Block block = event.getBlock();
			if (block.getType() != Material.WALL_SIGN) return;
			
			// Initialize a stargate
			if (hasPerm(player, "stargate.create", player.isOp()) ||
				hasPerm(player, "stargate.create.personal", false)) {
				SignPost sign = new SignPost(new Blox(block));
				// Set sign text so we can create a gate with it.
				sign.setText(0, event.getLine(0));
				sign.setText(1, event.getLine(1));
				sign.setText(2, event.getLine(2));
				sign.setText(3, event.getLine(3));
				Portal portal = Portal.createPortal(sign, player);
				if (portal == null) return;
				
				if (iConomyHandler.useiConomy()) {
					player.sendMessage(ChatColor.GREEN + "Deducted " + iConomy.getBank().format(portal.getGate().getCreateCost()));
				}
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
		public void onBlockBreak(BlockBreakEvent event) {
			Block block = event.getBlock();
			Player player = event.getPlayer();
			if (block.getType() != Material.WALL_SIGN && block.getType() != Material.STONE_BUTTON && Gate.getGatesByControlBlock(block).length == 0) {
				return;
			}

			Portal portal = Portal.getByBlock(block);
			if (portal == null) return;
			
			if (hasPerm(player, "stargate.destroy", player.isOp()) || hasPerm(player, "stargate.destroy.all", player.isOp()) ||
			   ( portal.getOwner().equalsIgnoreCase(player.getName()) && hasPerm(player, "stargate.destroy.owner", false) )) {
				// Can't afford
				if (iConomyHandler.useiConomy()) {
					if(iConomyHandler.getBalance(player.getName()) < portal.getGate().getDestroyCost()) {
						if (!iConomyHandler.inFundMsg.isEmpty()) {
							player.sendMessage(ChatColor.RED + iConomyHandler.inFundMsg);
							event.setCancelled(true);
							return;
						}
					}
					iConomyHandler.chargePlayer(player.getName(), null, portal.getGate().getDestroyCost());
					if (portal.getGate().getDestroyCost() > 0) {
						player.sendMessage(ChatColor.GREEN + "Deducted " + iConomy.getBank().format(portal.getGate().getDestroyCost()));
					} else if (portal.getGate().getDestroyCost() < 0) {
						player.sendMessage(ChatColor.GREEN + "Refunded " + iConomy.getBank().format(-portal.getGate().getDestroyCost()));
					}
				}
				
				portal.unregister(true);
				if (!dmgMsg.isEmpty()) {
					player.sendMessage(ChatColor.RED + dmgMsg);
				}
				return;
			}
			
			event.setCancelled(true);
		}

		@Override
		public void onBlockPhysics(BlockPhysicsEvent event) {
			Block block = event.getBlock();
			if (block.getType() == Material.PORTAL) {
				event.setCancelled((Portal.getByEntrance(block) != null));
			}
		}

		@Override
		public void onBlockFromTo(BlockFromToEvent event) {
			Portal portal = Portal.getByEntrance(event.getBlock());

			if (portal != null) {
				event.setCancelled((event.getBlock().getY() == event.getToBlock().getY()));
			}
		}
	}
	
	private class wListener extends WorldListener {
		@Override
		public void onWorldLoad(WorldLoadEvent event) {
			World w = event.getWorld();
			// We have to make sure the world is actually loaded. This gets called twice for some reason.
			if (w.getBlockAt(w.getSpawnLocation()).getWorld() != null) {
				Portal.loadAllGates(w);
			}
		}
	}
	
	private class eListener extends EntityListener {
		@Override
		public void onEntityExplode(EntityExplodeEvent event) {
			if (event.isCancelled()) return;
			for (Block b : event.blockList()) {
				if (b.getTypeId() != Material.WALL_SIGN.getId() && b.getTypeId() != Material.STONE_BUTTON.getId()) continue;
				Portal portal = Portal.getByBlock(b);
				if (portal == null) continue;
				if (destroyExplosion) {
					portal.unregister(true);
				} else {
					b.setType(b.getType());
					event.setCancelled(true);
				}
			}
		}
	}
	
	private class sListener extends ServerListener {
		@Override
		public void onPluginEnable(PluginEnableEvent event) {
			if (iConomyHandler.iconomy == null) {
				if (event.getPlugin().getDescription().getName().equalsIgnoreCase("iConomy")) {
					iConomyHandler.iconomy = (iConomy)checkPlugin(event.getPlugin());
				}
			}
			if (permissions == null) {
				if (event.getPlugin().getDescription().getName().equalsIgnoreCase("Permissions")) {
					permissions = (Permissions)checkPlugin(event.getPlugin());
				}
			}
		}
		
		@Override
		public void onPluginDisable(PluginDisableEvent event) {
			if (event.getPlugin() == iConomyHandler.iconomy) {
				log.info("[Stargate] iConomy plugin lost.");
				iConomyHandler.iconomy = null;
			}
			if (event.getPlugin() == permissions) {
				log.info("[Stargate] Permissions plugin lost.");
				permissions = null;
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
