package com.gmail.zolljd.simplenightsensor;

// Java Imports
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

// Bukkit Imports
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.plugin.PluginManager;


// Local Imports
import com.gmail.zolljd.simplenightsensor.Util;

public class SignCache {
	
	private ConcurrentMap<Location, String[]> sensors = new ConcurrentHashMap<Location, String[]>();
	private Logger logger;
	private File dataFolder;
	private boolean torchesOn;
	private World overWorld;
	private PluginManager pm;
	private SimpleNightSensor sns;
	
	public SignCache(PluginManager pm, SimpleNightSensor sns, World overWorld, File dataFolder, Logger logger) throws IOException {
		// Store initialization parameters
		this.dataFolder = dataFolder;
		this.logger = logger;
		this.overWorld = overWorld;
		this.pm = pm;
		this.sns = sns;
		
		// Initialize the torchesOn boolean based on world time.
		long curTime = this.overWorld.getTime();
		this.torchesOn = (curTime > 12500 && curTime < 23500) ? true : false;
		
		// Load the existing sensor data from file, if it exists.
		this.loadData();
	}
	
	private void loadData() throws IOException{
		// First, see if the data folder exists. If it doesn't, then create it.		
		if (!this.dataFolder.exists()) {
			dataFolder.mkdir();
		}
		
		// Next, if the data file itself exists, we need to load from it. Otherwise, we are done.
		// The data file is just X,Y,Z,line1,line2,line3,line4 pairs stored in with newlines after each piece of data.
		File dataFile = new File(dataFolder + File.separator + "sensors.dat");
		try {			
			FileInputStream fstream = new FileInputStream(dataFile);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
			
			String sensorLine;
			String [] signLines = new String[4];
			double[] locCoords = new double[3];	
			int curLine = 0;
			
			while ((sensorLine = br.readLine()) != null) {			
				if (curLine < 3) {
					locCoords[curLine] = Double.parseDouble(sensorLine);
				} else {
					signLines[curLine - 3] = sensorLine;
				}
				curLine++;
				
				// Every 7 lines, we reset our counter and use the accrued information to add a new monitored location.
				if (curLine == 7) {
					Location newLoc = new Location(this.overWorld,locCoords[0],locCoords[1],locCoords[2]);
					this.addLocation(newLoc, signLines);		 
					curLine = 0;
					signLines = new String[4];			
				}
			}
			
		} catch (FileNotFoundException e) {
			this.logger.info("No sensors.dat file found. Is this the first run of this plugin?");
		}
	}
	
	private void saveData() {
		// First, see if the data folder exists. If it doesn't, then create it.		
		if (!this.dataFolder.exists()) {
			dataFolder.mkdir();
		}
		
		// Now write the file. Each sign takes 7 lines.
		try {
			File dataFile = new File(dataFolder + File.separator + "sensors.dat");
			BufferedWriter out = new BufferedWriter(new FileWriter(dataFile));
			
			Iterator<Location> locIter = this.sensors.keySet().iterator();
			while (locIter.hasNext()) {
				Location cLoc = locIter.next();
				String[] signText = (String[])sensors.get(cLoc);
				
				// Write the location.
				out.write(Double.toString(cLoc.getX()));
				out.newLine();
				out.write(Double.toString(cLoc.getY()));
				out.newLine();
				out.write(Double.toString(cLoc.getZ()));
				out.newLine();
				
				// Write the Sign Lines
				for (int i = 0; i < 4; i++) {
					out.write(signText[i]);
					out.newLine();
				}
			}
			out.close();
		} catch(IOException e) {
			this.logger.severe("IOExcept while attempting to save sensor data. Stack trace follows. Disabling Plugin.");
			this.pm.disablePlugin(sns);
			e.printStackTrace();
		}
	}
	
	private void setTorchesOn(boolean tOn) {
		// Iterates over all monitored locations and calls the function
		// to update them based on the value of tOn.
		Iterator<Location> locIter = this.sensors.keySet().iterator();
		while (locIter.hasNext()) {
			Location cLoc = locIter.next();
			this.updateSensorAtLoc(cLoc, tOn);		
		}		
		this.torchesOn = tOn;
	}
	
	private void updateSensorAtLoc(Location cLoc, boolean tOn) {
		if (tOn) {
			// We need to do a little bit of checking when turning on torches. Since we don't clog the server up
			// on BlockBreak checking the block at Y+1 every time a block is broken, we verify here that the location being 
			// referenced still contains a SimpleNightSensor sign. If it does, then either it wasn't destroyed _or_ it was destroyed, 
			// recreated, and re-registered. If it doesn't contain the sign that we expect, then it was destroyed, 
			// so remove it from our cache and don't turn it into a torch.
			Sign cSign = Util.getSign(cLoc.getBlock());
			if (cSign == null || !cSign.getLine(1).equals("[NightSensor]")) {
				this.logger.info("Didn't find expected sign at " + Util.getLocString(cLoc) + ". Removing location from cache.");
				this.removeLocation(cLoc);
			} else {
				this.setBlockTorch(cLoc);
			}
		} else {
			// Here the checking is different. If the block is a powered redstone torch then we are in good shape,
			// otherwise remove it and change the block to a sign.
			int cTypeId = cLoc.getBlock().getTypeId();
			if (cTypeId != Material.REDSTONE_TORCH_ON.getId()) {
				this.logger.info("Didn't find expected torch at " + Util.getLocString(cLoc) + ". Removing location from cache.");
				this.removeLocation(cLoc);
			} else {
				String[] cLines = (String[])sensors.get(cLoc);
				this.setBlockSign(cLoc, cLines);
			}
		}		
	}
	
	private void setBlockSign(Location cLoc, String[] cLines) {
		// Sets the block at the given location to a sign with 4 lines matching
		// the first four lines of cLines.
		cLoc.getBlock().setTypeId(Material.SIGN_POST.getId());
		Sign newSign = (Sign)cLoc.getBlock().getState();
		for (int i = 0; i < 4; i++) {
			newSign.setLine(i, cLines[i]);
		}					
		newSign.update(true);
	}
	
	private void setBlockTorch(Location cLoc) {
		// Sets the block at the given location to a powered redstone torch.
		cLoc.getBlock().setTypeId(Material.REDSTONE_TORCH_ON.getId());
	}
	
	public boolean hasLocation(Location loc) {
		// Returns true if loc represents a location that is currently
		// through to be a NightSensor.
		return sensors.containsKey(loc);
	}
	
	public void removeLocation(Location loc) {
		// Removes a location from monitoring, and saves the updated
		// list of sensors to disk.
		sensors.remove(loc);
		this.saveData();
	}
	
	public boolean addLocation(Location loc, String[] signLines) {
		// Adds a location to monitoring, save to disk, and then
		// changes the newly created sign to a torch if the sign
		// was created at night.
		sensors.put(loc, signLines);
		this.saveData();
		if (this.torchesOn) {
			this.setBlockTorch(loc);
			return true;
		}
		return false;
	}
	
	public int getLocCount() {
		// Returns a count of the size of sensors.
		return this.sensors.size();
	}
	
	public void doTimeChange(int iTime) {		
		// When we detect a timeChangeEvent, check whether the new time is night or day.
		// If it is day and we haven't yet switched things to signs, do so. If it is night
		// and everything is still signs, change it to torches.
		//
		// For our purposes, the nighttime on state is between 1830 and 0530. This matches the
		// world time values used in intialization.
		if ((iTime < 530 || iTime > 1830) && !this.torchesOn) {
			logger.info("Nighttime detected. Replacing " + this.sensors.size() + " signs with torches.");
			this.setTorchesOn(true);
		} else if (iTime >= 530 && iTime <= 1830 && this.torchesOn) {
			logger.info("Daytime detected. Replacing " + this.sensors.size() + " torches with signs.");
			this.setTorchesOn(false);
		}
	}
}
