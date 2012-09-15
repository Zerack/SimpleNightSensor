package com.gmail.zolljd.simplenightsensor;

// Java Imports
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

// Bukkit Imports
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

// Local Imports
import com.gmail.zolljd.simplenightsensor.SignCache;
import com.gmail.zolljd.simplenightsensor.listener.BlockListener;
import com.gmail.zolljd.simplenightsensor.listener.DaytimeChangeListener;

// Dependency Imports
import com.V10lator.lib24time.lib24time;

public class SimpleNightSensor extends JavaPlugin {
	
	private BlockListener blockListener;
	private DaytimeChangeListener daytimeChangeListener;	
	private SignCache signCache;
	private Logger logger;
	private World overWorld;	
	
	@Override
	public void onEnable(){
		// When the plugin is enabled, which make sure our dependency (lib24time)
		// is also available. If it is not, spit a log message and disable ourselves.		
		if (!lib24time.isInitialized()) {
			getLogger().info("lib24time is not ready!");
			getLogger().info("Dependencies nonfunctional. Disabling plugin.");
			getServer().getPluginManager().disablePlugin(this);
		} else {
			// Dependencies are available, so continue initialization. The first step is to find
			// the NORMAL world of the running server, since we only permit signs to be made
			// in the normal world.
			this.overWorld = null;
			Iterator<World> worlds = Bukkit.getWorlds().iterator();
			while (worlds.hasNext()) {
				World cWorld = worlds.next();
				if (cWorld.getEnvironment() == Environment.NORMAL) {
					this.overWorld = cWorld;
					break;
				}
			}
			
			// Next, initialize the logger and then the required sign/sensor Cache as well as
			// the block and time listeners.
			this.logger = getLogger();
			try {
				this.signCache = new SignCache(getServer().getPluginManager(), this, this.overWorld, getDataFolder(), this.logger);
			} catch (IOException e) {
				this.logger.info("An IO exception occurred while loading sensor data. Stack trace follows. Disabling plugin.");
				getServer().getPluginManager().disablePlugin(this);
				e.printStackTrace();
				return;
			}
			this.blockListener = new BlockListener(this.overWorld, this.signCache, this.logger);
			this.daytimeChangeListener = new DaytimeChangeListener(this.signCache, this.logger);
			
			// Finally, register events on the listeners.
			getServer().getPluginManager().registerEvents(this.blockListener, this);
			getServer().getPluginManager().registerEvents(this.daytimeChangeListener, this);
		}		
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("sns")) {
			if (args.length != 1) {
				return false;
			} else if (!args[0].equals("count")) {
				return false;
			} else {
				int numSensors = this.signCache.getLocCount();
				sender.sendMessage("There " + (numSensors == 1 ? "is" : "are") + " currently " + this.signCache.getLocCount() + " active SimpleNightSensor" + (numSensors == 1 ? "" : "s") + ".");
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onDisable() {
		// Currently, no action is required when the plugin is disabled. The Sign Cache keeps track of itself on disk,
		// and there is no other state cleanup that needs to happen.
	}
}
