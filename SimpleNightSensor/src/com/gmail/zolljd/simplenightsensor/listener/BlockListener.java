package com.gmail.zolljd.simplenightsensor.listener;

// Java Imports
import java.util.logging.Logger;

// Bukkit Imports
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

// Local Imports
import com.gmail.zolljd.simplenightsensor.SignCache;
import com.gmail.zolljd.simplenightsensor.Util;

public class BlockListener implements Listener {
	
	private Logger logger;
	private SignCache signCache;
	private World overWorld;
	
	public BlockListener(World overWorld, SignCache signCache, Logger logger) {
		this.logger = logger;
		this.signCache = signCache;
		this.overWorld = overWorld;
	}
	
	@EventHandler
	public void onSignChange(SignChangeEvent e) {
		// We have several nested levels of verification here.  In order to create a sensor,
		// the following conditions must be met:
		//
		// 1. The sign is in the NORMAL world (not the nether, the end).
		// 2. The sign is a Sign Post, not a Wall Sign
		// 3. The second line of the sign is "[NightSensor]"
		// 4. The player has the "simplenightsensor.create" permission.
		Location loc = e.getBlock().getLocation();
		if (loc.getWorld().getUID().equals(overWorld.getUID())) {
			if (e.getBlock().getTypeId() == Material.SIGN_POST.getId()) {
				String[] signLines = e.getLines();				
				if (signLines[1].equals("[NightSensor]")) {
					Player cPlayer = e.getPlayer();
					if (cPlayer.hasPermission("simplenightsensor.create")) {
						// All four requirements are met, so create the sensor and store the location and sign text.
						boolean shouldCancel = signCache.addLocation(loc, signLines);
						if (shouldCancel) {
							// If we made a sign at night, we got a cancel back since the script turned it into a torch after
							// storing the values.
							e.setCancelled(true);
						}
						logger.info(cPlayer.getName() + " created/updated a NightSensor at " + Util.getLocString(loc) + ".");
						cPlayer.sendMessage("SimpleNightSensor created.");
					} else {
						// In this case, the player does not have permission to create a sensor. Send a message
						// and cancel the event.
						cPlayer.sendMessage("You don't have permission to create a SimpleNightSensor!");
						e.setCancelled(true);
					}					
				} else {
					// In this case, a Sign Post in the overworld was modified but does not have "[NightSensor]" in the
					// second line. We just make sure to remove it from our monitoring location if it was monitored.
					if (signCache.hasLocation(loc)) {
						signCache.removeLocation(loc);
						logger.info(e.getPlayer().getName() + " removed a NightSensor at " + Util.getLocString(loc) + " by updating sign text.");
						e.getPlayer().sendMessage("SimpleNightSensor removed.");						
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		// Like the SignChance event, there are a number of nested requirements here
		// with a variety of outcomes. In order for a blockbreak event to matter, we need:
		//
		// 1. The event takes place in the NORMAL world.
		// 2. The event is a break of either a Sign Post or a lit Redstone Torch
		// 3. The event is of a monitored location.
		Location loc = e.getBlock().getLocation();
		if (loc.getWorld().getUID().equals(overWorld.getUID())) {
			int typeId = e.getBlock().getTypeId();
			if (typeId == Material.SIGN_POST.getId() || typeId == Material.REDSTONE_TORCH_ON.getId()) {
				if (signCache.hasLocation(loc)) {
					signCache.removeLocation(loc);
					String playerName = e.getPlayer().getName();
					logger.info(playerName + " removed a NightSensor at " + Util.getLocString(loc) + " by breaking the associated block.");
					e.getPlayer().sendMessage("SimpleNightSensor removed.");
					
				}
			}
		}
	}
}
