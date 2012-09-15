package com.gmail.zolljd.simplenightsensor.listener;

// Java Imports
import java.util.logging.Logger;

//Bukkit Imports
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

// Dependency Imports
import com.V10lator.lib24time.event.TimeChangeEvent;

// Local Imports
import com.gmail.zolljd.simplenightsensor.SignCache;
import com.gmail.zolljd.simplenightsensor.Util;

public class DaytimeChangeListener implements Listener {
	
	private SignCache signCache;
	private Logger logger;
	
	public DaytimeChangeListener(SignCache signCache, Logger logger) {
		this.signCache = signCache;
		this.logger = logger;
	}
	
	@EventHandler
	public void onTimeChange(TimeChangeEvent event) {
		// lib24time doesn't seem to provide time in a numeric format, so we do a little work here.
		// This seems silly, though. I could ask the world what time it is, but is that really any more efficient?
		signCache.doTimeChange(Util.parseTime(event.getNewTime()));
	}
}
