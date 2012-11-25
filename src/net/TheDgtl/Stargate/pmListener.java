package net.TheDgtl.Stargate;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class pmListener implements PluginMessageListener {

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("SGBungee") || !Stargate.enableBungee) return;
		
		// Message should be destination gate name.
		Portal dest = Portal.getBungeeGate(new String(message));
		
		// Specified an invalid gate. For now we'll just let them connect at their current location
		if (dest == null) {
			return;
		}
		
		// Teleport the player to their destination portal
		dest.teleport(player, dest, null);
	}
}
