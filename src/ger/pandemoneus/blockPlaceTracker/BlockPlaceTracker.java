package ger.pandemoneus.blockPlaceTracker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This plugin tracks placing of configurable blocks.
 * 
 * @author Pandemoneus
 */
public class BlockPlaceTracker extends JavaPlugin implements Listener {
	private Logger logger;
	private File logFile;
	
	private static final String ITEM_REGEX = "[1-9][0-9]*(;[0-9]+)?"; // this format: 344 or 344;5
	
	private final Map<Integer, HashSet<Integer>> blocksToTrace = new HashMap<Integer, HashSet<Integer>>();
	
	public void onEnable() {
		logger = getLogger();
		logFile = new File(getDataFolder() + File.separator + "Log.txt");
		
		try {
			getDataFolder().mkdirs();
			logFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		getServer().getPluginManager().registerEvents(this, this);
		
		setupConfig();
	}
	
	public void onDisable() {
		
	}	
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPlace(final BlockPlaceEvent event) {
		final Block placedBlock = event.getBlockPlaced();
		final Location placedBlockLocation = placedBlock.getLocation();
		final Player placer = event.getPlayer();
		
		final int blockID = placedBlock.getTypeId();
		final int dataValue = placedBlock.getData();
		
		if (blocksToTrace.containsKey(blockID) && blocksToTrace.get(blockID).contains(dataValue)) {
			final Date date = new Date();
			final StringBuilder sb = new StringBuilder("[").append(date.toString()).append("] ").append(blockID).append(":").append(dataValue).append(" placed by ").append(placer.getName()).append(" at World=").append(placedBlockLocation.getWorld().getName()).append(", X=").append(placedBlockLocation.getBlockX()).append(", Y=").append(placedBlockLocation.getBlockY()).append(", Z=").append(placedBlockLocation.getBlockZ());

			try {
				final FileWriter fw = new FileWriter(logFile, true);
				fw.write(sb.toString());
				fw.write("\n");
				fw.flush();
				fw.close();
			} catch (IOException e) {
				logger.severe("Could not write to log file. Is it used by another process?");
				e.printStackTrace();
			}
		}
	}
	
	private void setupConfig() {
		final FileConfiguration config = getConfig();

		config.options().copyDefaults(true);
		config.options().header("Put the blocks that are to be tracked into the list at the bottom using this format: 334;3");
		
		saveConfig();
		
		setupTrackedBlocks(config.getStringList("blocksToTrack"));
	}
	
	private void setupTrackedBlocks(List<String> blockTrackList) {		
		if (blockTrackList == null || blockTrackList.isEmpty())
			return;
	
		// parse all Strings in the list
		for (final String s : blockTrackList) {
			if (s.matches(ITEM_REGEX)) {
				int itemID;
				int dataValue = 0;
				
				if (s.contains(";")) {
					final String[] splitted = s.split(";");
					
					itemID = Integer.parseInt(splitted[0]);
					dataValue = Integer.parseInt(splitted[1]);
				} else {
					itemID = Integer.parseInt(s);
				}
				
				HashSet<Integer> dataValues = blocksToTrace.get(itemID);
				
				if (dataValues == null)
					dataValues = new HashSet<Integer>();
				
				dataValues.add(dataValue);
				blocksToTrace.put(itemID, dataValues);
			} else {
				logger.warning("Found a String in the config.yml that does not match the block pattern: " + s);
			}
		}
	}
}
