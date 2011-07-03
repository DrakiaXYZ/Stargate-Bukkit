package net.TheDgtl.Stargate;

import com.iConomy.*;
import com.iConomy.system.Account;
import com.iConomy.system.Holdings;

/**
 * iConomyHandler.java
 * @author Steven "Drakia" Scott
 */

public class iConomyHandler {
	public static String pName = "Stargate";
	public static boolean useiConomy = false;
	public static iConomy iconomy = null;
	
	public static int useCost = 0;
	public static int createCost = 0;
	public static int destroyCost = 0;
	public static String inFundMsg = "Insufficient Funds.";
	public static boolean toOwner = false;
	public static boolean chargeFreeDestination = true;
	public static boolean freeGatesGreen = false;
	
	public static double getBalance(String player) {
		if (useiConomy && iconomy != null) {
			Account acc = iConomy.getAccount(player);
			if (acc == null) {
				Stargate.log.info("[" + pName + "::ich::getBalance] Error fetching iConomy account for " + player);
				return 0;
			}
			return acc.getHoldings().balance();
		}
		return 0;
	}
	
	public static boolean chargePlayer(String player, String target, double amount) {
		if (useiConomy && iconomy != null) {
			// No point going from a player to themself
			if (player.equals(target)) return true;
			
			Account acc = iConomy.getAccount(player);
			if (acc == null) {
				Stargate.log.info("[" + pName + "::ich::chargePlayer] Error fetching iConomy account for " + player);
				return false;
			}
			Holdings hold = acc.getHoldings();
			
			if (!hold.hasEnough(amount)) return false;
			hold.subtract(amount);
			
			if (target != null) {
				Account tAcc = iConomy.getAccount(target);
				if (tAcc != null) {
					Holdings tHold = tAcc.getHoldings();
					tHold.add(amount);
				}
			}
			return true;
		}
		return true;
	}
	
	public static boolean useiConomy() {
		return (useiConomy && iconomy != null);
	}
	
	public static String format(int amt) {
		return iConomy.format(amt);
	}
}
