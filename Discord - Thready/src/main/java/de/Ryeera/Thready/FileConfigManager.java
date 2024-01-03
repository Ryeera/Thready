package de.Ryeera.Thready;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

public class FileConfigManager {

	private File configFile;
	private JSONObject config;
	private DragoLogger logger;
	
	public FileConfigManager(File configFile, DragoLogger logger) {
		this.configFile = configFile;
		this.logger = logger;
		if(!reloadConfig()) {
			this.logger.log("ERROR", "Couldn't read config-file! Exiting...");
			System.exit(2);
		}
		this.logger.log("INFO", "Setting up scheduled tasks...");
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
			if (System.currentTimeMillis() - this.configFile.lastModified() < 90000)
				reloadConfig();
		}, 1, 1, TimeUnit.MINUTES);
		this.logger.log("INFO", "1 Scheduled Task set up.");
	}
	
	private boolean reloadConfig() {
		logger.log("INFO", "Loading Config...");
		try {
			JSONObject config = JSONUtils.readJSON(this.configFile);
			logger.log("INFO", "Validating Config...");
			if (checkConfig(config)) {
				logger.log("INFO", "Config validated!");
				this.config = config;
				logger.log("INFO", "Config loaded!");
				return true;
			} else {
				logger.log("WARN", "Config invalid! Keeping old config loaded!");
				return false;
			}
		} catch (IOException e) {
			logger.log("ERROR", "Couldn't read config-file!");
			logger.logStackTrace(e);
			return false;
		}
	}
	
	private boolean checkConfig(JSONObject config) {
		try {
			// Check /token
			if (!config.has("token") 
					|| config.isNull("token") 
					|| !config.getString("token").matches(".{24}\\..{6}\\..{27}")) return false;
			
			// Check /sql
			if (!config.has("sql") 
					|| config.isNull("sql")) return false;
			JSONObject sql = config.getJSONObject("sql");
			
			// Check /sql/host
			if (!sql.has("host") 
					|| sql.isNull("host") 
					|| sql.getString("host").isBlank()) return false;
			
			// Check /sql/port
			if (!sql.has("port") 
					|| sql.isNull("port") 
					|| sql.getInt("port") > 65535 
					|| sql.getInt("port") < 1) return false;
			
			// Check /sql/user
			if (!sql.has("user") 
					|| sql.isNull("user") 
					|| sql.getString("user").isBlank()) return false;
			
			// Check /sql/pass
			if (!sql.has("pass") 
					|| sql.isNull("pass")
					|| sql.getString("pass").length() < 0) return false;
			
			// Check /sql/database
			if (!sql.has("database") 
					|| sql.isNull("database") 
					|| sql.getString("database").isBlank()) return false;
		} catch (JSONException e) {
			return false;
		}
		return true;
	}
	
	public JSONObject getRawConfig() {
		return config;
	}
	
	public String getToken() {
		return config.getString("token");
	}
	
	public String getSQLHost() {
		return config.getJSONObject("sql").getString("host");
	}
	
	public int getSQLPort() {
		return config.getJSONObject("sql").getInt("port");
	}
	
	public String getSQLUser() {
		return config.getJSONObject("sql").getString("user");
	}
	
	public String getSQLPass() {
		return config.getJSONObject("sql").getString("pass");
	}
	
	public String getSQLDatabase() {
		return config.getJSONObject("sql").getString("database");
	}
}
