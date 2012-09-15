package com.gmail.zolljd.simplenightsensor;

// Bukkit Imports
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class Util {
	private Util() {}
	
	public static int parseTime(String sTime) {
		// Returns an integer representation of a 24 hour time (HH:MM) format.
		// For example, 18:30 returns 1830, while 05:30 returns 530.
		String[] aTime = sTime.split(":");		
		return (Integer.parseInt(aTime[0]) * 100) + Integer.parseInt(aTime[1]);
	}
	
	public static String getLocString(Location loc) {
		// Returns a string representation of the integer coordinates of a location object.
		// X, Z, Y in that order are distances east from origin, south from origin, and up from world bottom.
		return "X:" + loc.getBlockX() + ", Z:" + loc.getBlockZ() + ", Y:" + loc.getBlockY(); 
	}
	
	public static Sign getSign(Block b) {
		// Returns sign information if a block is a sign. In all other cases,
		// returns null;		
		Sign sign = null;		
		int typeId = b.getTypeId();
		if (typeId == Material.SIGN_POST.getId()) {
			sign = (Sign)b.getState();
		}		
		return sign;
	}
}
