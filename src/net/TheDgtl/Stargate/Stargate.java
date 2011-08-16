package net.TheDgtl.Stargate;

import java.io.File;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
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
import com.iConomy.*;

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
	public static int maxGates = 0;
	private static int activeTime = 10;
	private static int openTime = 10;
	
	// Used for debug
	private static boolean debug = false;
	
	public static ConcurrentLinkedQueue<Portal> openList = new ConcurrentLinkedQueue<Portal>();
	public static ConcurrentLinkedQueue<Portal> activeList = new ConcurrentLinkedQueue<Portal>();
	
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
		if (iConomyHandler.useiConomy)
			iConomyHandler.iconomy = (iConomy)checkPlugin("iConomy");
		
		pm.registerEvent(Event.Type.PLAYER_MOVE, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
		
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Normal, this);
		
		pm.registerEvent(Event.Type.VEHICLE_MOVE, vehicleListener, Priority.Normal, this);
		
		pm.registerEvent(Event.Type.WORLD_LOAD, worldListener, Priority.Normal, this);
		
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Priority.Normal, this);
		//pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.Normal, this);
		//pm.registerEvent(Event.Type.ENTITY_COMBUST, entityListener, Priority.Normal, this);
		
		// Used to disable built-in portal for Stargates
		pm.registerEvent(Event.Type.PLAYER_PORTAL, playerListener, Priority.Normal, this);
		
		// Dependency Loading
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
		maxGates = config.getInt("maxgates", maxGates);
		// Debug
		debug = config.getBoolean("debug", debug);
		// iConomy
		iConomyHandler.useiConomy = config.getBoolean("useiconomy", iConomyHandler.useiConomy);
		iConomyHandler.createCost = config.getInt("createcost", iConomyHandler.createCost);
		iConomyHandler.destroyCost = config.getInt("destroycost", iConomyHandler.destroyCost);
		iConomyHandler.useCost = config.getInt("usecost", iConomyHandler.useCost);
		iConomyHandler.inFundMsg = config.getString("not-enough-money-message", iConomyHandler.inFundMsg);
		iConomyHandler.toOwner = config.getBoolean("toowner", iConomyHandler.toOwner);
		iConomyHandler.chargeFreeDestination = config.getBoolean("chargefreedestination", iConomyHandler.chargeFreeDestination);
		iConomyHandler.freeGatesGreen = config.getBoolean("freegatesgreen", iConomyHandler.freeGatesGreen);
		
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
		config.setProperty("maxgates", maxGates);
		// iConomy
		config.setProperty("useiconomy", iConomyHandler.useiConomy);
		config.setProperty("createcost", iConomyHandler.createCost);
		config.setProperty("destroycost", iConomyHandler.destroyCost);
		config.setProperty("usecost", iConomyHandler.useCost);
		config.setProperty("not-enough-money-message", iConomyHandler.inFundMsg);
		config.setProperty("toowner", iConomyHandler.toOwner);
		config.setProperty("chargefreedestination", iConomyHandler.chargeFreeDestination);
		config.setProperty("freegatesgreen", iConomyHandler.freeGatesGreen);
		
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
	
	public static void debug(String rout, String msg) {
		if (Stargate.debug) {
			log.info("[Stargate::" + rout + "] " + msg);
		} else {
			log.log(Level.FINEST, "[Stargate::" + rout + "] " + msg);
		}
	}
	
	public static void sendMessage(Player player, String message) {
		sendMessage(player, message, true);
	}
	
	public static void sendMessage(Player player, String message, boolean error) {
		if (message.isEmpty()) return;
		if (error)
			player.sendMessage(ChatColor.RED + "[Stargate] " + ChatColor.WHITE + message);
		else
			player.sendMessage(ChatColor.GREEN + "[Stargate] " + ChatColor.WHITE + message);
	}

	public static String getSaveLocation() {
		return portalFolder;
	}

	public static String getDefaultNetwork() {
		return defNetwork;
	}

	private void onButtonPressed(Player player, Portal portal) {
		Portal destination = portal.getDestination();
		
		// Always-open gate -- Do nothing
		if (portal.isAlwaysOn()) {
			return;
		}
		
		// Invalid destination
		if ((destination == null) || (destination == portal)) {
			Stargate.sendMessage(player, invMsg);
			return;
		}
		
		// Gate is already open
		if (portal.isOpen()) {
			// Close if this player opened the gate
			if (portal.getActivePlayer() == player) {
				portal.close(false);
			}
			return;
		}
		
		// Gate that someone else is using -- Deny access
		if ((!portal.isFixed()) && portal.isActive() &&  (portal.getActivePlayer() != player)) {
			Stargate.sendMessage(player, denyMsg);
			return;
		}
		
		// Check if the player can use the private gate
		if (portal.isPrivate() && !Stargate.canPrivate(player, portal)) {
			Stargate.sendMessage(player, denyMsg);
			return;
		}
		
		// Destination blocked
		if ((destination.isOpen()) && (!destination.isAlwaysOn())) {
			Stargate.sendMessage(player, blockMsg);
			return;
		}
		
		// Open gate
		portal.open(player, false);
	}

	/*
	 * Check whether the player has the given permissions.
	 */
	public static boolean hasPerm(Player player, String perm) {
		if (permissions != null) {
			return permissions.getHandler().has(player, perm);
		} else {
			return player.hasPermission(perm);
		}
	}
	
	/*
	 * Check whether player can teleport to dest world
	 */
	public static boolean canAccessWorld(Player player, String world) {
		// Can use all Stargate player features
		if (hasPerm(player, "stargate.use")) return true;
		// Can access all worlds
		if (hasPerm(player, "stargate.world")) return true;
		// Can access dest world
		if (hasPerm(player, "stargate.world." + world)) return true;
		return false;
	}
	
	/*
	 * Check whether player can use network
	 */
	public static boolean canAccessNetwork(Player player, String network) {
		// Can use all Stargate player features
		if (hasPerm(player, "stargate.use")) return true;
		// Can access all networks
		if (hasPerm(player, "stargate.network")) return true;
		// Can access this network
		if (hasPerm(player, "stargate.network." + network)) return true;
		return false;
	}
	
	/*
	 * Return true if the portal is free for the player
	 */
	public static boolean isFree(Player player, Portal src, Portal dest) {
		// This gate is free
		if (src.isFree()) return true;
		// Player gets free use
		if (hasPerm(player, "stargate.free") || Stargate.hasPerm(player,  "stargate.free.use")) return true;
		// Don't charge for free destination gates
		if (!iConomyHandler.chargeFreeDestination && dest.isFree()) return true;
		return false;
	}
	
	/*
	 * Check whether the player can see this gate (Hidden property check)
	 */
	public static boolean canSee(Player player, Portal portal) {
		// The gate is not hidden
		if (!portal.isHidden()) return true;
		// The player is an admin with the ability to see hidden gates
		if (hasPerm(player, "stargate.admin") || hasPerm(player, "stargate.admin.hidden")) return true;
		// The player is the owner of the gate
		if (portal.getOwner().equalsIgnoreCase(player.getName())) return true;
		return false;
	}
	
	/*
	 * Check if the player can use this private gate
	 */
	public static boolean canPrivate(Player player, Portal portal) {
		// Check if the player is the owner of the gate
		if (portal.getOwner().equalsIgnoreCase(player.getName())) return true;
		// The player is an admin with the ability to use private gates
		if (hasPerm(player, "stargate.admin") || hasPerm(player, "stargate.admin.private")) return true;
		return false;
	}
	
	/*
	 * Check if the player has access to {option}
	 */
	public static boolean canOption(Player player, String option) {
		// Check if the player can use all options
		if (hasPerm(player, "stargate.option")) return true;
		// Check if they can use this specific option
		if (hasPerm(player, "stargate.option." + option)) return true;
		return false;
	}
	
	/*
	 * Check if the player can create gates on {network}
	 */
	public static boolean canCreate(Player player, String network) {
		// Check for general create
		if (hasPerm(player, "stargate.create")) return true;
		// Check for all network create permission
		if (hasPerm(player, "stargate.create.network")) return true;
		// Check for this specific network
		if (hasPerm(player, "stargate.create.network." + network)) return true;
		
		// Check if this is a personal gate, and if the player has create.personal
		if (player.getName().substring(0,  11).equalsIgnoreCase(network) && hasPerm(player, "stargate.create.personal")) return true;
		return false;
	}
	
	/*
	 * Check if the player can destroy this gate
	 */
	public static boolean canDestroy(Player player, Portal portal) {
		// Check for general destroy
		if (hasPerm(player, "stargate.destroy")) return true;
		// Check for all network destroy permission
		if (hasPerm(player, "stargate.destroy.network")) return true;
		// Check for this specific network
		if (hasPerm(player, "stargate.destroy.network." + portal.getNetwork())) return true;
		// Check for personal gate
		if (player.getName().equalsIgnoreCase(portal.getOwner()) && hasPerm(player, "stargate.destroy.personal")) return true;
		return false;
	}
	
	/*
	 * Charge player for {action} if required, true on success, false if can't afford
	 */
	public static boolean chargePlayer(Player player, String target, String action, int cost) {
		// If cost is 0
		if (cost <= 0) return true;
		// iConomy is disabled
		if (!iConomyHandler.useiConomy()) return true;
		// Player gets free {action}
		if (hasPerm(player, "stargate.free") || hasPerm(player, "stargate.free." + action)) return true;
		// Charge player
		return iConomyHandler.chargePlayer(player.getName(), target, cost);
	}
	
	/*
	 * Determine the cost of a gate
	 */
	public static int getUseCost(Player player, Portal src, Portal dest) {
		// Not using iConomy
		if (!iConomyHandler.useiConomy()) return 0;
		// Portal is free
		if (src.isFree()) return 0;
		// Not charging for free destinations
		if (!iConomyHandler.chargeFreeDestination && dest.isFree()) return 0;
		// Cost is 0 if the player owns this gate and funds go to the owner
		if (src.getGate().getToOwner() && src.getOwner().equalsIgnoreCase(player.getName())) return 0;
		
		return src.getGate().getUseCost();
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
					Stargate.sendMessage(player, denyMsg);
					return;
				}
				
				Portal dest = portal.getDestination();
				if (dest == null) return;
				// Check if player has access to this network
				if (!canAccessNetwork(player, portal.getNetwork())) {
					Stargate.sendMessage(player, denyMsg);
					portal.close(false);
					return;
				}
				
				// Check if player has access to destination world
				if (!canAccessWorld(player, dest.getWorld().getName())) {
					Stargate.sendMessage(player, denyMsg);
					portal.close(false);
					return;
				}
				
				int cost = Stargate.getUseCost(player, portal, dest);
				if (cost > 0) {
					String target = portal.getGate().getToOwner() ? portal.getOwner() : null;
					if (!Stargate.chargePlayer(player, target, "use", cost)) {
						// Insufficient Funds
						Stargate.sendMessage(player, "Insufficient Funds");
						portal.close(false);
						return;
					}
					sendMessage(player, "Deducted " + iConomyHandler.format(cost), false);
					if (target != null) {
						Player p = server.getPlayer(target);
						if (p != null) {
							Stargate.sendMessage(p, "Obtained " + iConomyHandler.format(cost) + " from Stargate " + portal.getName(), false);
						}
					}
				}
				
				Stargate.sendMessage(player, teleMsg, false);
				dest.teleport(vehicle);
				portal.close(false);
			} else {
				Portal dest = portal.getDestination();
				if (dest == null) return;
				dest.teleport(vehicle);
			}
		}
	}
	
	private class pListener extends PlayerListener {
		@Override
		public void onPlayerPortal(PlayerPortalEvent event) {
			// Do a quick check for a stargate
			Location from = event.getFrom();
			World world = from.getWorld();
			int cX = from.getBlockX();
			int cY = from.getBlockY();
			int cZ = from.getBlockZ();
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					for (int k = 0; k < 3; k++) {
						Block b = world.getBlockAt(cX + i, cY + j, cZ + k);
						Portal portal = Portal.getByEntrance(b);
						if (portal != null) {
							event.setCancelled(true);
							return;
						}
					}
				}
			}
		}
		
		@Override
		public void onPlayerMove(PlayerMoveEvent event) {
			Player player = event.getPlayer();
			Portal portal = Portal.getByEntrance(event.getTo());
			
			// No portal or not open
			if (portal == null || !portal.isOpen()) return;

			// Not open for this player
			if (!portal.isOpenFor(player)) {
				if (!denyMsg.isEmpty()) {
					Stargate.sendMessage(player, denyMsg);
				}
				portal.teleport(player, portal, event);
				return;
			}
			
			Portal destination = portal.getDestination();
			if (destination == null) return;
			
			// Check if player has access to this network
			if (!canAccessNetwork(player, portal.getNetwork())) {
				Stargate.sendMessage(player, denyMsg);
				portal.teleport(player, portal, event);
				portal.close(false);
				return;
			}
			
			// Check if player has access to destination world
			if (!canAccessWorld(player, destination.getWorld().getName())) {
				Stargate.sendMessage(player, denyMsg);
				portal.teleport(player, portal, event);
				portal.close(false);
				return;
			}
			
			int cost = Stargate.getUseCost(player, portal, destination);
			if (cost > 0) {
				String target = portal.getGate().getToOwner() ? portal.getOwner() : null;
				if (!Stargate.chargePlayer(player, target, "use", cost)) {
					// Insufficient Funds
					Stargate.sendMessage(player, "Insufficient Funds");
					portal.close(false);
					return;
				}
				sendMessage(player, "Deducted " + iConomyHandler.format(cost), false);
				if (target != null) {
					Player p = server.getPlayer(target);
					if (p != null) {
						Stargate.sendMessage(p, "Obtained " + iConomyHandler.format(cost) + " from Stargate " + portal.getName(), false);
					}
				}
			}
			
			Stargate.sendMessage(player,  teleMsg);
			destination.teleport(player, portal, event);
			portal.close(false);
		}
		
		@Override
		public void onPlayerInteract(PlayerInteractEvent event) {
			Player player = event.getPlayer();
			Block block = event.getClickedBlock();
			
			// Right click
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (block.getType() == Material.WALL_SIGN) {
					Portal portal = Portal.getByBlock(block);
					if (portal == null) return;
					// Cancel item use
					event.setUseItemInHand(Result.DENY);
					event.setUseInteractedBlock(Result.DENY);
					
					if (!Stargate.canAccessNetwork(player,  portal.getNetwork())) {
						Stargate.sendMessage(player, denyMsg);
						return;
					}
					
					if ((!portal.isOpen()) && (!portal.isFixed())) {
						portal.cycleDestination(player);
					}
					return;
				}

				// Implement right-click to toggle a stargate, gets around spawn protection problem.
				if ((block.getType() == Material.STONE_BUTTON)) {
					Portal portal = Portal.getByBlock(block);
					if (portal == null) return;
					if (!Stargate.canAccessNetwork(player, portal.getNetwork())) {
						Stargate.sendMessage(player, denyMsg);
						return;
					}
					onButtonPressed(player, portal);
				}
				return;
			}
			
			// Left click
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				// Check if we're scrolling a sign
				if (block.getType() == Material.WALL_SIGN) {
					Portal portal = Portal.getByBlock(block);
					if (portal == null) return;
					// Cancel item use
					event.setUseItemInHand(Result.DENY);
					event.setUseInteractedBlock(Result.DENY);
					
					if (!Stargate.canAccessNetwork(player,  portal.getNetwork())) {
						Stargate.sendMessage(player, denyMsg);
						return;
					}
					
					if ((!portal.isOpen()) && (!portal.isFixed())) {
						portal.cycleDestination(player, -1);
					}
					return;
				}

				// Check if we're pushing a button.
				if (block.getType() == Material.STONE_BUTTON) {
					Portal portal = Portal.getByBlock(block);
					if (portal == null) return;
					if (!Stargate.canAccessNetwork(player, portal.getNetwork())) {
						Stargate.sendMessage(player, denyMsg);
						return;
					}
					onButtonPressed(player, portal);
				}
			}
		}
	}

	private class bListener extends BlockListener {
		@Override
		public void onSignChange(SignChangeEvent event) {
			Player player = event.getPlayer();
			Block block = event.getBlock();
			if (block.getType() != Material.WALL_SIGN) return;
			
			// Initialize a stargate -- Permission check is done in createPortal
			SignPost sign = new SignPost(new Blox(block));
			// Set sign text so we can create a gate with it.
			sign.setText(0, event.getLine(0));
			sign.setText(1, event.getLine(1));
			sign.setText(2, event.getLine(2));
			sign.setText(3, event.getLine(3));
			Portal portal = Portal.createPortal(sign, player);
			// Not creating a gate, just placing a sign
			if (portal == null)	return;

			Stargate.sendMessage(player, regMsg, false);
			Stargate.debug("onSignChange", "Initialized stargate: " + portal.getName());
			portal.drawSign();
			// Set event text so our new sign is instantly initialized
			event.setLine(0, sign.getText(0));
			event.setLine(1, sign.getText(1));
			event.setLine(2, sign.getText(2));
			event.setLine(3, sign.getText(3));
		}
		
		@Override
		public void onBlockBreak(BlockBreakEvent event) {
			if (event.isCancelled()) return;
			Block block = event.getBlock();
			Player player = event.getPlayer();
			if (block.getType() != Material.WALL_SIGN && block.getType() != Material.STONE_BUTTON && !Gate.isGateBlock(block.getTypeId())) {
				return;
			}

			Portal portal = Portal.getByBlock(block);
			if (portal == null) return;
			
			if (!Stargate.canDestroy(player, portal)) {
				Stargate.sendMessage(player, "Permission Denied");
				event.setCancelled(true);
				return;
			}
			
			if (!Stargate.chargePlayer(player, null,  "destroy", portal.getGate().getDestroyCost())) {
				Stargate.debug("onBlockBreak", "Insufficient Funds");
				Stargate.sendMessage(player,  iConomyHandler.inFundMsg);
				event.setCancelled(true);
				return;
			}
			
			if (portal.getGate().getDestroyCost() > 0) {
				Stargate.sendMessage(player, "Deducted " + iConomyHandler.format(portal.getGate().getDestroyCost()), false);
			} else if (portal.getGate().getDestroyCost() < 0) {
				Stargate.sendMessage(player, "Refunded " + iConomyHandler.format(-portal.getGate().getDestroyCost()), false);
			}
			
			portal.unregister(true);
			Stargate.sendMessage(player, dmgMsg, false);
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
				if (b.getType() != Material.WALL_SIGN && b.getType() != Material.STONE_BUTTON && !Gate.isGateBlock(b.getTypeId())) continue;
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
		// Going to leave this commented out until they fix EntityDamagebyBlock
		/*
		@Override
		public void onEntityDamage(EntityDamageEvent event) {
			if (!(event.getEntity() instanceof Player)) return;
			if (!(event instanceof EntityDamageByBlockEvent)) return;
			EntityDamageByBlockEvent bEvent = (EntityDamageByBlockEvent)event;
			Player player = (Player)bEvent.getEntity();
			Block block = bEvent.getDamager();
			// Fucking null blocks, we'll do it live! This happens for lava only, as far as I know.
			// So we're "borrowing" the code from World.java used to determine if we're intersecting a lava block
			if (block == null) {
				CraftEntity ce = (CraftEntity)event.getEntity();
				net.minecraft.server.Entity entity = ce.getHandle();
				AxisAlignedBB axisalignedbb = entity.boundingBox.b(-0.10000000149011612D, -0.4000000059604645D, -0.10000000149011612D); 
		        int minx = MathHelper.floor(axisalignedbb.a);
		        int maxx = MathHelper.floor(axisalignedbb.d + 1.0D);
		        int miny = MathHelper.floor(axisalignedbb.b);
		        int maxy = MathHelper.floor(axisalignedbb.e + 1.0D);
		        int minz = MathHelper.floor(axisalignedbb.c);
		        int maxz = MathHelper.floor(axisalignedbb.f + 1.0D);

		        for (int x = minx; x < maxx; ++x) {
		            for (int y = miny; y < maxy; ++y) {
		                for (int z = minz; z < maxz; ++z) {
		                	int blockType = player.getWorld().getBlockTypeIdAt(x, y, z);
		                    if (blockType == Material.LAVA.getId() || blockType == Material.STATIONARY_LAVA.getId()) {
		                        block = player.getWorld().getBlockAt(x, y, z);
		                        log.info("Found block! " + block);
		                        break;
		                    }
		                }
		                if (block != null) break;
		            }
		            if (block != null) break;
		        }
			}
			if (block == null) return;
			Portal portal = Portal.getByEntrance(block);
			if (portal == null) return;
			log.info("Found portal");
			bEvent.setDamage(0);
			bEvent.setCancelled(true);
		}
		
		@Override
		public void onEntityCombust(EntityCombustEvent event) {
			if (!(event.getEntity() instanceof Player)) return;
			Player player = (Player)event.getEntity();
			// WHY DOESN'T THIS CANCEL IF YOU CANCEL LAVA DAMAGE?!
			Block block = null;
			CraftEntity ce = (CraftEntity)event.getEntity();
			net.minecraft.server.Entity entity = ce.getHandle();
			AxisAlignedBB axisalignedbb = entity.boundingBox.b(-0.10000000149011612D, -0.4000000059604645D, -0.10000000149011612D); 
	        int minx = MathHelper.floor(axisalignedbb.a);
	        int maxx = MathHelper.floor(axisalignedbb.d + 1.0D);
	        int miny = MathHelper.floor(axisalignedbb.b);
	        int maxy = MathHelper.floor(axisalignedbb.e + 1.0D);
	        int minz = MathHelper.floor(axisalignedbb.c);
	        int maxz = MathHelper.floor(axisalignedbb.f + 1.0D);

	        for (int x = minx; x < maxx; ++x) {
	            for (int y = miny; y < maxy; ++y) {
	                for (int z = minz; z < maxz; ++z) {
	                	int blockType = player.getWorld().getBlockTypeIdAt(x, y, z);
	                    if (blockType == Material.LAVA.getId() || blockType == Material.STATIONARY_LAVA.getId()) {
	                        block = player.getWorld().getBlockAt(x, y, z);
	                        log.info("Found block! " + block);
	                        break;
	                    }
	                }
	                if (block != null) break;
	            }
	            if (block != null) break;
	        }
			if (block == null) return;
			log.info("What? " + block);
			Portal portal = Portal.getByEntrance(block);
			if (portal == null) return;
			log.info("What2?");
			event.setCancelled(true);
		}*/
	}
	
	private class sListener extends ServerListener {
		@Override
		public void onPluginEnable(PluginEnableEvent event) {
			if (iConomyHandler.useiConomy && iConomyHandler.iconomy == null) {
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
			if (iConomyHandler.useiConomy && event.getPlugin() == iConomyHandler.iconomy) {
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
				// Skip always open gates
				if (p.isAlwaysOn()) continue;
				if (time > p.getOpenTime() + Stargate.openTime) {
					p.close(false);
					iter.remove();
				}
			}
			// Deactivate active portals
			for (Iterator<Portal> iter = Stargate.activeList.iterator(); iter.hasNext();) {
				Portal p = iter.next();
				if (time > p.getOpenTime() + Stargate.activeTime) {
					p.deactivate();
					iter.remove();
				}
			}
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			Stargate.sendMessage((Player)sender, "Permission Denied");
			return true;
		}
		String cmd = command.getName();
		if (cmd.equalsIgnoreCase("sg")) {
			if (args.length != 1) return false;
			if (args[0].equalsIgnoreCase("reload")) {
				// Clear all lists
				activeList.clear();
				openList.clear();
				Portal.clearGates();
				Gate.clearGates();
				
				// Reload data
				reloadConfig();
				reloadGates();
				return true;
			}
			return false;
		}
		return false;
	}
}
