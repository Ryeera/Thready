package de.Ryeera.Thready;

import java.sql.ResultSet;
import java.sql.SQLException;

import de.Ryeera.libs.DragoLogger;
import de.Ryeera.libs.SQLConnector;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class SQLConfigManager {

	private SQLConnector sql;
	private DragoLogger logger;
	private ResultSet cache;
	private long cacheChannel = 0l;
	
	public SQLConfigManager(String host, int port, String user, String pass, String database, DragoLogger logger) {
		this.logger = logger;
		this.sql = new SQLConnector(host, port, user, pass, database);
		if (this.sql.executeUpdate("CREATE TABLE IF NOT EXISTS `thready`.`channels` ( "
				+ "`guild` BIGINT(20) UNSIGNED NOT NULL COMMENT 'The ID of the Guild this Channel belongs to.' , "
				+ "`channel` BIGINT(20) UNSIGNED NOT NULL COMMENT 'The ID of the Channel.' , "
				+ "`config` SMALLINT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'The binary-encoded config for this Channel.' , "
				+ "`enabled` TINYINT(1) UNSIGNED NOT NULL DEFAULT FALSE COMMENT 'Whether Thready for this Channel is enabled or not.' , "
				+ "`stat_threads_created` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'Stats: How many threads were created by Thready in this Channel.' , "
				+ "PRIMARY KEY (`channel`)"
				+ ") ENGINE = InnoDB COMMENT = 'Information including config and stats for each Channel.';")) {
			this.logger.log("INFO", "New table created.");
		}
	}
	
	private void clearCache() {
		try {
			if (cache != null)
				cache.close();
			cache = null;
			cacheChannel = 0l;
		} catch (SQLException e) {}
	}
	
	private ResultSet getChannelConfig(Guild guild, MessageChannel channel) throws SQLException {
		if (cacheChannel == channel.getIdLong()) {
			return cache;
		} else if (cacheChannel != 0l) {
			cache.close();
		}
		ResultSet channelConfig = sql.executeQuery("SELECT * FROM `channels` WHERE `channel`='" + channel.getId() + "';");
		if (channelConfig.next()) {
			cache = channelConfig;
			cacheChannel = channel.getIdLong();
			return channelConfig;
		} else {
			channelConfig.close();
			if (sql.executeUpdate("INSERT INTO `channels` (`guild`, `channel`) VALUES ('" + guild.getId() + "', '" + channel.getId() + "');")) {
				logger.log("INFO", "Created new config for channel " + channel.getId() + " in guild " + guild.getId());
			} else {
				logger.log("ERROR", "Failed to create a new config for channel " + channel.getId() + " in guild " + guild.getId() + "!");
				throw new SQLException("Couldn't create new config for channel " + channel.getId() + "!");
			}
			channelConfig = sql.executeQuery("SELECT * FROM `channels` WHERE `channel`='" + channel.getId() + "';");
			if (channelConfig.next()) {
				cache = channelConfig;
				cacheChannel = channel.getIdLong();
				return channelConfig;
			} else {
				throw new SQLException("The Data could not be retrieved from the server!");
			}
		}
	}
	
	public int getThreadingConfig(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelConfig(guild, channel).getInt("config");
	}
	
	public boolean isEnabled(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelConfig(guild, channel).getBoolean("enabled");
	}
	
	public boolean setEnabled(MessageChannel channel, boolean enabled) {
		clearCache();
		return sql.executeUpdate("UPDATE `channels` SET `enabled`=" + enabled + " WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean enableThreadingOption(MessageChannel channel, int option) {
		clearCache();
		return sql.executeUpdate("UPDATE `channels` SET `config`=(SELECT `config` FROM `channels` WHERE `channel`='" + channel.getId() + "')+" + option + " WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean disableThreadingOption(MessageChannel channel, int option) {
		clearCache();
		return sql.executeUpdate("UPDATE `channels` SET `config`=(SELECT `config` FROM `channels` WHERE `channel`='" + channel.getId() + "')-" + option + " WHERE `channel`='" + channel.getId() + "'");
	}
}
