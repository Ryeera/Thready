package de.Ryeera.Thready;

import java.io.File;

import de.Ryeera.libs.DragoLogger;
import de.Ryeera.libs.Utils;

public class Thready {
	
	public static final String VERSION = "0.1.1";

	public static FileConfigManager config;
	public static SQLConfigManager sql;
	public static DiscordConnector discord;
	public static DragoLogger logger;
	
	public static void main(String[] args) {
		try {
			new File("logs").mkdir();
			logger = new DragoLogger(new File("logs" + File.separator + "thready_" + Utils.formatTime(System.currentTimeMillis(), "yyyyMMdd_HHmmss") + ".log"));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		logger.log("INFO", "Starting Thready v" + VERSION + "...");
		config = new FileConfigManager(new File("thready.json"), logger);
		
		logger.log("INFO", "Setting up SQL-Connection...");
		sql = new SQLConfigManager(config.getSQLHost(), config.getSQLPort(), config.getSQLUser(), config.getSQLPass(), config.getSQLDatabase(), logger);
		logger.log("INFO", "SQL-Connection established.");
		
		logger.log("INFO", "Setting up Discord-Connection...");
		discord = new DiscordConnector(config.getToken(), new DiscordListener(sql, logger), logger);
		logger.log("INFO", "Discord-Connection established.");
		
		logger.log("INFO", "Thready v" + VERSION + " started! I'm watching...");
	}
}
