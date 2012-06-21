package net.TheDgtl.Stargate;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.TheDgtl.Stargate.event.StargateAccessEvent;
import net.TheDgtl.Stargate.event.StargateDestroyEvent;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

// Permissions
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * Stargate - A portal plugin for Bukkit
 * Copyright (C) 2011 Shaun (sturmeh)
 * Copyright (C) 2011 Dinnerbone
 * Copyright (C) 2011, 2012 Steven "Drakia" Scott <Contact@TheDgtl.net>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@SuppressWarnings("unused")
public class Stargate extends JavaPlugin {
	// Permissions
	private static Permissions permissions = null;
	
	public static Logger log;
	private FileConfiguration newConfig;
	private PluginManager pm;
	public static Server server;
	public static Stargate stargate;
	private static LangLoader lang;
	
	private static String portalFolder;
	private static String gateFolder;
	private static String langFolder;
	private static String defNetwork = "central";
	private static boolean destroyExplosion = false;
	public static int maxGates = 0;
	private static String langName = "en";
	private static int activeTime = 10;
	private static int openTime = 10;
	public static boolean destMemory = false;
	public static boolean handleVehicles = true;
	public static boolean sortLists = false;
	public static boolean protectEntrance = false;
	public static ChatColor signColor;
	
	// Temp workaround for snowmen, don't check gate entrance
	public static boolean ignoreEntrance = false;
	
	// Used for debug
	public static boolean debug = false;
	public static boolean permDebug = false;
	
	public static ConcurrentLinkedQueue<Portal> openList = new ConcurrentLinkedQueue<Portal>();
	public static ConcurrentLinkedQueue<Portal> activeList = new ConcurrentLinkedQueue<Portal>();
	
	// Used for populating gate open/closed material.
	public static Queue<BloxPopulator> blockPopulatorQueue = new LinkedList<BloxPopulator>();
	
	public void onDisable() {
		Portal.closeAllGates();
		Portal.clearGates();
		getServer().getScheduler().cancelTasks(this);
	}

	public void onEnable() {
		PluginDescriptionFile pdfFile = this.getDescription();
		pm = getServer().getPluginManager();
		newConfig = this.getConfig();
		log = Logger.getLogger("Minecraft");
		Stargate.server = getServer();
		Stargate.stargate = this;
		
		// Set portalFile and gateFolder to the plugin folder as defaults.
		portalFolder = getDataFolder().getPath().replaceAll("\\\\", "/") + "/portals/";
		gateFolder = getDataFolder().getPath().replaceAll("\\\\", "/") + "/gates/";
		langFolder = getDataFolder().getPath().replaceAll("\\\\", "/") + "/lang/";
		
		log.info(pdfFile.getName() + " v." + pdfFile.getVersion() + " is enabled.");
		
		// Register events before loading gates to stop weird things happening.
		pm.registerEvents(new pListener(), this);
		pm.registerEvents(new bListener(), this);
		
		pm.registerEvents(new vListener(), this);
		pm.registerEvents(new eListener(), this);
		pm.registerEvents(new wListener(), this);
		pm.registerEvents(new sListener(), this);
		
		this.loadConfig();
		this.migrate();
		this.reloadGates();
		lang = new LangLoader(langFolder, Stargate.langName);
		
		// Check to see if iConomy/Permissions is loaded yet.
		permissions = (Permissions)checkPlugin("Permissions");
		if (permissions != null && (permissions.getDescription().getVersion().equals("2.7.2") ||  permissions.getDescription().getVersion().equals("2.7.7"))) {
			log.info("[Stargate] Permissions is 2.7.2/2.7.7, most likely a bridge, disabling.");
			permissions = null;
		}
		if (iConomyHandler.setupeConomy(pm)) {
			if (iConomyHandler.register != null)
				log.info("[Stargate] Register v" + iConomyHandler.register.getDescription().getVersion() + " found");
			if (iConomyHandler.economy != null)
				log.info("[Stargate] Vault v" + iConomyHandler.vault.getDescription().getVersion() + " found");
        }
		
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new SGThread(), 0L, 100L);
		getServer().getScheduler().scheduleSyncRepeatingTask(this, new BlockPopulatorThread(), 0L, 1L);
	}

	public void loadConfig() {
		reloadConfig();
		newConfig = this.getConfig();
		// Copy default values if required
		newConfig.options().copyDefaults(true);
		
		// Load values into variables
		portalFolder = newConfig.getString("portal-folder");
		gateFolder = newConfig.getString("gate-folder");
		defNetwork = newConfig.getString("default-gate-network").trim();
		destroyExplosion = newConfig.getBoolean("destroyexplosion");
		maxGates = newConfig.getInt("maxgates");
		langName = newConfig.getString("lang");
		destMemory = newConfig.getBoolean("destMemory");
		ignoreEntrance = newConfig.getBoolean("ignoreEntrance");
		handleVehicles = newConfig.getBoolean("handleVehicles");
		sortLists = newConfig.getBoolean("sortLists");
		protectEntrance = newConfig.getBoolean("protectEntrance");
		// Sign color
		String sc = newConfig.getString("signColor");
		try {
			signColor = ChatColor.valueOf(sc.toUpperCase());
		} catch (Exception ignore) {
			log.warning("[Stargate] You have specified an invalid color in your config.yml. Defaulting to BLACK");
			signColor = ChatColor.BLACK;
		}
		// Debug
		debug = newConfig.getBoolean("debug");
		permDebug = newConfig.getBoolean("permdebug");
		// iConomy
		iConomyHandler.useiConomy = newConfig.getBoolean("useiconomy");
		iConomyHandler.createCost = newConfig.getInt("createcost");
		iConomyHandler.destroyCost = newConfig.getInt("destroycost");
		iConomyHandler.useCost = newConfig.getInt("usecost");
		iConomyHandler.toOwner = newConfig.getBoolean("toowner");
		iConomyHandler.chargeFreeDestination = newConfig.getBoolean("chargefreedestination");
		iConomyHandler.freeGatesGreen = newConfig.getBoolean("freegatesgreen");
		
		this.saveConfig();
	}
	
	public void reloadGates() {
		// Close all gates prior to reloading
		for (Portal p : openList) {
			p.close(true);
		}
		
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
	
	public static void sendMessage(CommandSender player, String message) {
		sendMessage(player, message, true);
	}
	
	public static void sendMessage(CommandSender player, String message, boolean error) {
		if (message.isEmpty()) return;
		message = message.replaceAll("(&([a-f0-9]))", "\u00A7$2");
		if (error)
			player.sendMessage(ChatColor.RED + Stargate.getString("prefix") + ChatColor.WHITE + message);
		else
			player.sendMessage(ChatColor.GREEN + Stargate.getString("prefix") + ChatColor.WHITE + message);
	}
	
	public static void setLine(Sign sign, int index, String text) {
		sign.setLine(index, Stargate.signColor + text);
	}

	public static String getSaveLocation() {
		return portalFolder;
	}
	
	public static String getGateFolder() {
		return gateFolder;
	}

	public static String getDefaultNetwork() {
		return defNetwork;
	}
	
	public static String getString(String name) {
		return lang.getString(name);
	}

	public static void openPortal(Player player, Portal portal) {
		Portal destination = portal.getDestination();
		
		// Always-open gate -- Do nothing
		if (portal.isAlwaysOn()) {
			return;
		}
		
		// Random gate -- Do nothing
		if (portal.isRandom())
			return;
		
		// Invalid destination
		if ((destination == null) || (destination == portal)) {
			Stargate.sendMessage(player, Stargate.getString("invalidMsg"));
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
			Stargate.sendMessage(player, Stargate.getString("denyMsg"));
			return;
		}
		
		// Check if the player can use the private gate
		if (portal.isPrivate() && !Stargate.canPrivate(player, portal)) {
			Stargate.sendMessage(player, Stargate.getString("denyMsg"));
			return;
		}
		
		// Destination blocked
		if ((destination.isOpen()) && (!destination.isAlwaysOn())) {
			Stargate.sendMessage(player, Stargate.getString("blockMsg"));
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
			if (permDebug)
				Stargate.debug("hasPerm::Permissions(" + player.getName() + ")", perm + " => " + permissions.getHandler().has(player, perm));
			return permissions.getHandler().has(player, perm);
		} else {
			if (permDebug)
				Stargate.debug("hasPerm::SuperPerm(" + player.getName() + ")", perm + " => " + player.hasPermission(perm));
			return player.hasPermission(perm);
		}
	}
	
	/*
	 * Check a deep permission, this will check to see if the permissions is defined for this use
	 * If using Permissions it will return the same as hasPerm
	 * If using SuperPerms will return true if the node isn't defined
	 * Or the value of the node if it is
	 */
	public static boolean hasPermDeep(Player player, String perm) {
		if (permissions != null) {
			if (permDebug)
				Stargate.debug("hasPermDeep::Permissions", perm + " => " + permissions.getHandler().has(player, perm));
			return permissions.getHandler().has(player,  perm);
		} else {
			if (!player.isPermissionSet(perm)) {
				if (permDebug)
					Stargate.debug("hasPermDeep::SuperPerm", perm + " => true");
				return true;
			}
			if (permDebug)
				Stargate.debug("hasPermDeep::SuperPerms", perm + " => " + player.hasPermission(perm));
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
		if (hasPerm(player, "stargate.world")) {
			// Do a deep check to see if the player lacks this specific world node
			if (!hasPermDeep(player, "stargate.world." + world)) return false;
			return true;
		}
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
		if (hasPerm(player, "stargate.network")) {
			// Do a deep check to see if the player lacks this specific network node
			if (!hasPermDeep(player, "stargate.network." + network)) return false;
			return true;
		}
		// Can access this network
		if (hasPerm(player, "stargate.network." + network)) return true;
		// Is able to create personal gates (Assumption is made they can also access them)
		String playerName = player.getName();
		if (playerName.length() > 11) playerName = playerName.substring(0, 11);
		if (network.equals(playerName) && hasPerm(player, "stargate.create.personal")) return true;
		return false;
	}
	
	/*
	 * Call the StargateAccessPortal event, used for other plugins to bypass Permissions checks
	 */
	public static boolean canAccessPortal(Player player, Portal portal, boolean deny) {
		StargateAccessEvent event = new StargateAccessEvent(player, portal, deny);
		Stargate.server.getPluginManager().callEvent(event);
		if (event.getDeny()) return false;
		return true;
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
		if (dest != null && !iConomyHandler.chargeFreeDestination && dest.isFree()) return true;
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
		if (hasPerm(player, "stargate.create.network")) {
			// Do a deep check to see if the player lacks this specific network node
			if (!hasPermDeep(player, "stargate.create.network." + network)) return false;
			return true;
		}
		// Check for this specific network
		if (hasPerm(player, "stargate.create.network." + network)) return true;
		
		return false;
	}
	
	/*
	 * Check if the player can create a personal gate
	 */
	public static boolean canCreatePersonal(Player player) {
		// Check for general create
		if (hasPerm(player, "stargate.create")) return true;
		// Check for personal
		if (hasPerm(player, "stargate.create.personal")) return true;
		return false;
	}
	
	/*
	 * Check if the player can create this gate layout
	 */
	public static boolean canCreateGate(Player player, String gate) {
		// Check for general create
		if (hasPerm(player, "stargate.create")) return true;
		// Check for all gate create permissions
		if (hasPerm(player, "stargate.create.gate")) {
			// Do a deep check to see if the player lacks this specific gate node
			if (!hasPermDeep(player, "stargate.create.gate." + gate)) return false;
			return true;
		}
		// Check for this specific gate
		if (hasPerm(player, "stargate.create.gate." + gate)) return true;
		
		return false;
	}
	
	/*
	 * Check if the player can destroy this gate
	 */
	public static boolean canDestroy(Player player, Portal portal) {
		String network = portal.getNetwork();
		// Check for general destroy
		if (hasPerm(player, "stargate.destroy")) return true;
		// Check for all network destroy permission
		if (hasPerm(player, "stargate.destroy.network")) {
			// Do a deep check to see if the player lacks permission for this network node
			if (!hasPermDeep(player, "stargate.destroy.network." + network)) return false;
			return true;
		}
		// Check for this specific network
		if (hasPerm(player, "stargate.destroy.network." + network)) return true;
		// Check for personal gate
		if (player.getName().equalsIgnoreCase(portal.getOwner()) && hasPerm(player, "stargate.destroy.personal")) return true;
		return false;
	}
	
	/*
	 * Charge player for {action} if required, true on success, false if can't afford
	 */
	public static boolean chargePlayer(Player player, String target, int cost) {
		// If cost is 0
		if (cost == 0) return true;
		// iConomy is disabled
		if (!iConomyHandler.useiConomy()) return true;
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
		// Player gets free gate use
		if (hasPerm(player, "stargate.free") || hasPerm(player, "stargate.free.use")) return 0;
		
		return src.getGate().getUseCost();
	}
	
	/*
	 * Determine the cost to create the gate
	 */
	public static int getCreateCost(Player player, Gate gate) {
		// Not using iConomy
		if (!iConomyHandler.useiConomy()) return 0;
		// Player gets free gate destruction
		if (hasPerm(player, "stargate.free") || hasPerm(player, "stargate.free.create")) return 0;
		
		return gate.getCreateCost();
	}
	
	/*
	 * Determine the cost to destroy the gate
	 */
	public static int getDestroyCost(Player player, Gate gate) {
		// Not using iConomy
		if (!iConomyHandler.useiConomy()) return 0;
		// Player gets free gate destruction
		if (hasPerm(player, "stargate.free") || hasPerm(player, "stargate.free.destroy")) return 0;
		
		return gate.getDestroyCost();
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
	
	/*
	 * Parse a given text string and replace the variables
	 */
	public static String replaceVars(String format, String[] search, String[] replace) {
		if (search.length != replace.length) return "";
		for (int i = 0; i < search.length; i++) {
			format = format.replace(search[i], replace[i]);
		}
		return format;
	}
	
	private class vListener implements Listener {
		@EventHandler
		public void onVehicleMove(VehicleMoveEvent event) {
			if (!handleVehicles) return;
			Entity passenger = event.getVehicle().getPassenger();
			Vehicle vehicle = event.getVehicle();
			
			Portal portal = Portal.getByEntrance(event.getTo());
			if (portal == null || !portal.isOpen()) return;
			
			if (passenger instanceof Player) {
				Player player = (Player)passenger;
				if (!portal.isOpenFor(player)) {
					Stargate.sendMessage(player, Stargate.getString("denyMsg"));
					return;
				}
				
				Portal dest = portal.getDestination(player);
				if (dest == null) return;
				boolean deny = false;
				// Check if player has access to this network
				if (!canAccessNetwork(player, portal.getNetwork())) {
					deny = true;
				}
				
				// Check if player has access to destination world
				if (!canAccessWorld(player, dest.getWorld().getName())) {
					deny = true;
				}
				
				if (!canAccessPortal(player, portal, deny)) {
					Stargate.sendMessage(player, Stargate.getString("denyMsg"));
					portal.close(false);
					return;
				}
				
				int cost = Stargate.getUseCost(player, portal, dest);
				if (cost > 0) {
					String target = portal.getGate().getToOwner() ? portal.getOwner() : null;
					if (!Stargate.chargePlayer(player, target, cost)) {
						// Insufficient Funds
						Stargate.sendMessage(player, Stargate.getString("inFunds"));
						portal.close(false);
						return;
					}
					String deductMsg = Stargate.getString("ecoDeduct");
					deductMsg = Stargate.replaceVars(deductMsg, new String[] {"%cost%", "%portal%"}, new String[] {iConomyHandler.format(cost), portal.getName()});
					sendMessage(player, deductMsg, false);
					if (target != null) {
						Player p = server.getPlayer(target);
						if (p != null) {
							String obtainedMsg = Stargate.getString("ecoObtain");
							obtainedMsg = Stargate.replaceVars(obtainedMsg, new String[] {"%cost%", "%portal%"}, new String[] {iConomyHandler.format(cost), portal.getName()});
							Stargate.sendMessage(p, obtainedMsg, false);
						}
					}
				}
				
				Stargate.sendMessage(player, Stargate.getString("teleportMsg"), false);
				dest.teleport(vehicle);
				portal.close(false);
			} else {
				Portal dest = portal.getDestination();
				if (dest == null) return;
				dest.teleport(vehicle);
			}
		}
	}
	
	private class pListener implements Listener {
		@EventHandler
		public void onPlayerPortal(PlayerPortalEvent event) {
			if (event.isCancelled()) return;
			// Do a quick check for a stargate
			Location from = event.getFrom();
			if (from == null) {
				Stargate.debug("onPlayerPortal", "From location is null. Stupid Bukkit");
				return;
			}
			World world = from.getWorld();
			int cX = from.getBlockX();
			int cY = from.getBlockY();
			int cZ = from.getBlockZ();
			for (int i = -2; i < 2; i++) {
				for (int j = -2; j < 2; j++) {
					for (int k = -2; k < 2; k++) {
						Block b = world.getBlockAt(cX + i, cY + j, cZ + k);
						// We only need to worry about portal mat
						if (b.getType() != Material.PORTAL) continue;
						Portal portal = Portal.getByEntrance(b);
						if (portal != null) {
							event.setCancelled(true);
							return;
						}
					}
				}
			}
		}
		
		@EventHandler
		public void onPlayerMove(PlayerMoveEvent event) {
			if (event.isCancelled()) return;
			Player player = event.getPlayer();
			Portal portal = Portal.getByEntrance(event.getTo());
			
			// No portal or not open
			if (portal == null || !portal.isOpen()) return;

			// Not open for this player
			if (!portal.isOpenFor(player)) {
				Stargate.sendMessage(player, Stargate.getString("denyMsg"));
				portal.teleport(player, portal, event);
				return;
			}
			
			Portal destination = portal.getDestination(player);
			if (destination == null) return;
			
			boolean deny = false;
			// Check if player has access to this network
			if (!canAccessNetwork(player, portal.getNetwork())) {
				deny = true;
			}
			
			// Check if player has access to destination world
			if (!canAccessWorld(player, destination.getWorld().getName())) {
				deny = true;
			}
			
			if (!canAccessPortal(player, portal, deny)) {
				Stargate.sendMessage(player, Stargate.getString("denyMsg"));
				portal.teleport(player, portal, event);
				portal.close(false);
				return;
			}
			
			int cost = Stargate.getUseCost(player, portal, destination);
			if (cost > 0) {
				String target = portal.getGate().getToOwner() ? portal.getOwner() : null;
				if (!Stargate.chargePlayer(player, target, cost)) {
					// Insufficient Funds
					Stargate.sendMessage(player, "Insufficient Funds");
					portal.close(false);
					return;
				}
				String deductMsg = Stargate.getString("ecoDeduct");
				deductMsg = Stargate.replaceVars(deductMsg, new String[] {"%cost%", "%portal%"}, new String[] {iConomyHandler.format(cost), portal.getName()});
				sendMessage(player, deductMsg, false);
				if (target != null) {
					Player p = server.getPlayer(target);
					if (p != null) {
						String obtainedMsg = Stargate.getString("ecoObtain");
						obtainedMsg = Stargate.replaceVars(obtainedMsg, new String[] {"%cost%", "%portal%"}, new String[] {iConomyHandler.format(cost), portal.getName()});
						Stargate.sendMessage(p, obtainedMsg, false);
					}
				}
			}
			
			Stargate.sendMessage(player, Stargate.getString("teleportMsg"), false);
			destination.teleport(player, portal, event);
			portal.close(false);
		}
		
		@EventHandler
		public void onPlayerInteract(PlayerInteractEvent event) {
			if (event.isCancelled()) return;
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
					
					boolean deny = false;
					if (!Stargate.canAccessNetwork(player, portal.getNetwork())) {
						deny = true;
					}
					
					if (!Stargate.canAccessPortal(player, portal, deny)) {
						Stargate.sendMessage(player, Stargate.getString("denyMsg"));
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
					
					// Cancel item use
					event.setUseItemInHand(Result.DENY);
					event.setUseInteractedBlock(Result.DENY);
					
					boolean deny = false;
					if (!Stargate.canAccessNetwork(player, portal.getNetwork())) {
						deny = true;
					}
					
					if (!Stargate.canAccessPortal(player, portal, deny)) {
						Stargate.sendMessage(player, Stargate.getString("denyMsg"));
						return;
					}
					
					openPortal(player, portal);
					if (portal.isOpenFor(player)) {
						event.setUseInteractedBlock(Result.ALLOW);
					}
				}
				return;
			}
			
			// Left click
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				// Check if we're scrolling a sign
				if (block.getType() == Material.WALL_SIGN) {
					Portal portal = Portal.getByBlock(block);
					if (portal == null) return;
					
					event.setUseInteractedBlock(Result.DENY);
					// Only cancel event in creative mode
					if (player.getGameMode().equals(GameMode.CREATIVE)) {
						event.setCancelled(true);
					}
					
					boolean deny = false;
					if (!Stargate.canAccessNetwork(player, portal.getNetwork())) {
						deny = true;
					}
					
					if (!Stargate.canAccessPortal(player, portal, deny)) {
						Stargate.sendMessage(player, Stargate.getString("denyMsg"));
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
					
					event.setUseInteractedBlock(Result.DENY);
					if (player.getGameMode().equals(GameMode.CREATIVE)) {
						event.setCancelled(true);
					}
					
					boolean deny = false;
					if (!Stargate.canAccessNetwork(player, portal.getNetwork())) {
						deny = true;
					}
					
					if (!Stargate.canAccessPortal(player, portal, deny)) {
						Stargate.sendMessage(player, Stargate.getString("denyMsg"));
						return;
					}
					openPortal(player, portal);
				}
			}
		}
	}

	private class bListener implements Listener {
		@EventHandler
		public void onSignChange(SignChangeEvent event) {
			if (event.isCancelled()) return;
			Player player = event.getPlayer();
			Block block = event.getBlock();
			if (block.getType() != Material.WALL_SIGN) return;
			
			final Portal portal = Portal.createPortal(event, player);
			// Not creating a gate, just placing a sign
			if (portal == null)	return;

			Stargate.sendMessage(player, Stargate.getString("createMsg"), false);
			Stargate.debug("onSignChange", "Initialized stargate: " + portal.getName());
			Stargate.server.getScheduler().scheduleSyncDelayedTask(stargate, new Runnable() {
				public void run() {
					portal.drawSign();
				}
			}, 1);
		}
		
		// Switch to HIGHEST priority so as to come after block protection plugins (Hopefully)
		@EventHandler(priority = EventPriority.HIGHEST)
		public void onBlockBreak(BlockBreakEvent event) {
			if (event.isCancelled()) return;
			Block block = event.getBlock();
			Player player = event.getPlayer();

			Portal portal = Portal.getByBlock(block);
			if (portal == null && protectEntrance)
				portal = Portal.getByEntrance(block);
			if (portal == null) return;
			
			boolean deny = false;
			String denyMsg = "";
			
			if (!Stargate.canDestroy(player, portal)) {
				denyMsg = "Permission Denied"; // TODO: Change to Stargate.getString()
				deny = true;
				Stargate.log.info("[Stargate] " + player.getName() + " tried to destroy gate");
			}
			
			int cost = Stargate.getDestroyCost(player,  portal.getGate());
			
			StargateDestroyEvent dEvent = new StargateDestroyEvent(portal, player, deny, denyMsg, cost);
			Stargate.server.getPluginManager().callEvent(dEvent);
			if (dEvent.isCancelled()) {
				event.setCancelled(true);
				return;
			}
			if (dEvent.getDeny()) {
				Stargate.sendMessage(player, dEvent.getDenyReason());
				event.setCancelled(true);
				return;
			}
			
			cost = dEvent.getCost();
			
			if (cost != 0) {
				if (!Stargate.chargePlayer(player, null, cost)) {
					Stargate.debug("onBlockBreak", "Insufficient Funds");
					Stargate.sendMessage(player, Stargate.getString("inFunds"));
					event.setCancelled(true);
					return;
				}
				
				if (cost > 0) {
					String deductMsg = Stargate.getString("ecoDeduct");
					deductMsg = Stargate.replaceVars(deductMsg, new String[] {"%cost%", "%portal%"}, new String[] {iConomyHandler.format(cost), portal.getName()});
					sendMessage(player, deductMsg, false);
				} else if (cost < 0) {
					String refundMsg = Stargate.getString("ecoRefund");
					refundMsg = Stargate.replaceVars(refundMsg, new String[] {"%cost%", "%portal%"}, new String[] {iConomyHandler.format(-cost), portal.getName()});
					sendMessage(player, refundMsg, false);
				}
			}
			
			portal.unregister(true);
			Stargate.sendMessage(player, Stargate.getString("destroyMsg"), false);
		}

		@EventHandler
		public void onBlockPhysics(BlockPhysicsEvent event) {
			Block block = event.getBlock();
			Portal portal = Portal.getByEntrance(block);
			if (portal != null) event.setCancelled(true);
			portal = Portal.getByControl(block);
			if (portal != null) event.setCancelled(true);
		}

		@EventHandler
		public void onBlockFromTo(BlockFromToEvent event) {
			Portal portal = Portal.getByEntrance(event.getBlock());

			if (portal != null) {
				event.setCancelled((event.getBlock().getY() == event.getToBlock().getY()));
			}
		}
		
		@EventHandler
		public void onPistonExtend(BlockPistonExtendEvent event) {
			for(Block block : event.getBlocks()) {
				Portal portal = Portal.getByBlock(block);
				if (portal != null) {
					event.setCancelled(true);
					return;
				}
			}
		}
		
		@EventHandler
		public void onPistonRetract(BlockPistonRetractEvent event) {
			if (!event.isSticky()) return;
			Block affected = event.getRetractLocation().getBlock();
			Portal portal = Portal.getByBlock(affected);
			if (portal != null) event.setCancelled(true);
		}
	}
	
	private class wListener implements Listener {
		@EventHandler
		public void onWorldLoad(WorldLoadEvent event) {
			World w = event.getWorld();
			// We have to make sure the world is actually loaded. This gets called twice for some reason.
			if (w.getBlockAt(w.getSpawnLocation()).getWorld() != null) {
				Portal.loadAllGates(w);
			}
		}
		
		// We need to reload all gates on world unload, boo
		@EventHandler
		public void onWorldUnload(WorldUnloadEvent event) {
			Stargate.debug("onWorldUnload", "Reloading all Stargates");
			World w = event.getWorld();
			Portal.clearGates();
			for (World world : server.getWorlds()) {
				if (world.equals(w)) continue;
				Portal.loadAllGates(world);
			}
		}
	}
	
	private class eListener implements Listener {
		@EventHandler
		public void onEntityExplode(EntityExplodeEvent event) {
			if (event.isCancelled()) return;
			for (Block b : event.blockList()) {
				Portal portal = Portal.getByBlock(b);
				if (portal == null) continue;
				if (destroyExplosion) {
					portal.unregister(true);
				} else {
					Stargate.blockPopulatorQueue.add(new BloxPopulator(new Blox(b), b.getTypeId(), b.getData()));
					event.setCancelled(true);
				}
			}
		}
		// TODO: Uncomment when Bukkit pulls SnowmanTrailEvent
		/*
		@Override
		public void onSnowmanTrail(SnowmanTrailEvent event) {
			Portal p = Portal.getByEntrance(event.getBlock());
			if (p != null) event.setCancelled(true);
		}
		*/
		
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
	
	private class sListener implements Listener {
		@EventHandler
		public void onPluginEnable(PluginEnableEvent event) {
			if (iConomyHandler.setupRegister(event.getPlugin())) {
				log.info("[Stargate] Register v" + iConomyHandler.register.getDescription().getVersion() + " found");
			}
			if (iConomyHandler.setupVault(event.getPlugin())) {
				log.info("[Stargate] Vault v" + iConomyHandler.vault.getDescription().getVersion() + " found");
			}
			if (permissions == null) {
				PluginDescriptionFile desc = event.getPlugin().getDescription();
				if (desc.getName().equalsIgnoreCase("Permissions")) {
					if (desc.getVersion().equals("2.7.2") || desc.getVersion().equals("2.7.7")) {
						log.info("[Stargate] Permissions is 2.7.2/2.7.7, most likely a bridge, disabling.");
						return;
					}
					permissions = (Permissions)checkPlugin(event.getPlugin());
				}
			}
		}
		
		@EventHandler
		public void onPluginDisable(PluginDisableEvent event) {
			if (iConomyHandler.checkLost(event.getPlugin())) {
				log.info("[Stargate] Register/Vault plugin lost.");
			}
			if (event.getPlugin() == permissions) {
				log.info("[Stargate] Permissions plugin lost.");
				permissions = null;
			}
		}
	}
	
	private class BlockPopulatorThread implements Runnable {
		public void run() {
			long sTime = System.nanoTime();
			while (System.nanoTime() - sTime < 50000000) {
				BloxPopulator b = Stargate.blockPopulatorQueue.poll();
				if (b == null) return;
				b.getBlox().getBlock().setTypeId(b.getMat());
				b.getBlox().getBlock().setData(b.getData());
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
				if (!p.isOpen()) continue;
				if (time > p.getOpenTime() + Stargate.openTime) {
					p.close(false);
					iter.remove();
				}
			}
			// Deactivate active portals
			for (Iterator<Portal> iter = Stargate.activeList.iterator(); iter.hasNext();) {
				Portal p = iter.next();
				if (!p.isActive()) continue;
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
			Player p = (Player)sender;
			if (!hasPerm(p, "stargate.admin") && !hasPerm(p, "stargate.admin.reload")) {
				sendMessage(sender, "Permission Denied");
				return true;
			}
		}
		String cmd = command.getName();
		if (cmd.equalsIgnoreCase("sg")) {
			if (args.length != 1) return false;
			if (args[0].equalsIgnoreCase("reload")) {
				// Deactivate portals
				for (Portal p : activeList) {
					p.deactivate();
				}
				// Close portals
				for (Portal p : openList) {
					p.close(true);
				}
				// Clear all lists
				activeList.clear();
				openList.clear();
				Portal.clearGates();
				Gate.clearGates();
				
				// Reload data
				loadConfig();
				reloadGates();
				lang.setLang(langName);
				lang.reload();
				
				// Load iConomy support if enabled/clear if disabled
				if (iConomyHandler.useiConomy && iConomyHandler.register == null && iConomyHandler.economy == null) {
					if (iConomyHandler.setupeConomy(pm)) {
						if (iConomyHandler.register != null)
							log.info("[Stargate] Register v" + iConomyHandler.register.getDescription().getVersion() + " found");
						if (iConomyHandler.economy != null)
							log.info("[Stargate] Vault v" + iConomyHandler.vault.getDescription().getVersion() + " found");
			        }
				}
				if (!iConomyHandler.useiConomy) {
					iConomyHandler.vault = null;
					iConomyHandler.register = null;
					iConomyHandler.economy = null;
				}
				sendMessage(sender, "Stargate reloaded");
				return true;
			}
			return false;
		}
		return false;
	}
}
