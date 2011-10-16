package net.TheDgtl.Stargate;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.nijikokun.register.Register;
import com.nijikokun.register.payment.Method;
import com.nijikokun.register.payment.Method.MethodAccount;
import com.nijikokun.register.payment.Methods;

/**
 * iConomyHandler.java
 * @author Steven "Drakia" Scott
 */

public class iConomyHandler {
	public static String pName = "Stargate";
	public static boolean useiConomy = false;
	public static Register register = null;
	
	public static int useCost = 0;
	public static int createCost = 0;
	public static int destroyCost = 0;
	public static boolean toOwner = false;
	public static boolean chargeFreeDestination = true;
	public static boolean freeGatesGreen = false;
	
	public static double getBalance(String player) {
		if (useiConomy && register != null) {
			Method method = Methods.getMethod();
			if (method == null) {
				return 0;
			}
			
			MethodAccount acc = method.getAccount(player);
			if (acc == null) {
				Stargate.debug("ich::getBalance", "Error fetching Register account for " + player);
				return 0;
			}
			return acc.balance();
		}
		return 0;
	}
	
	public static boolean chargePlayer(String player, String target, double amount) {
		if (useiConomy && register != null) {
			// Check for a payment method
			Method method = Methods.getMethod();
			if (method == null) {
				return true;
			}
			// No point going from a player to themself
			if (player.equals(target)) return true;
			
			MethodAccount acc = method.getAccount(player);
			if (acc == null) {
				Stargate.debug("ich::chargePlayer", "Error fetching Register account for " + player);
				return false;
			}
			
			if (!acc.hasEnough(amount)) return false;
			acc.subtract(amount);
			
			if (target != null) {
				MethodAccount tAcc = method.getAccount(target);
				if (tAcc != null) {
					tAcc.add(amount);
				}
			}
			return true;
		}
		return true;
	}
	
	public static boolean useiConomy() {
		return (useiConomy && register != null && Methods.getMethod() != null);
	}
	
	public static String format(int amt) {
		Method method = Methods.getMethod();
		if (method == null) {
			return Integer.toString(amt);
		}
		return method.format(amt);
	}
	
	public static boolean setupiConomy(PluginManager pm) {
		if (!useiConomy) return false;
		Plugin p = pm.getPlugin("Register");
        return setupiConomy(p);
	}
	
	public static boolean setupiConomy(Plugin p) {
		if (!useiConomy) return false;
		if (p == null || !p.isEnabled()) return false;
		if (!p.getDescription().getName().equals("Register")) return false;
		register = (Register)p;
		return true;
	}
	
	public static boolean checkLost(Plugin p) {
		if (p.equals(register)) {
			register = null;
			return true;
		}
		return false;
	}
}
