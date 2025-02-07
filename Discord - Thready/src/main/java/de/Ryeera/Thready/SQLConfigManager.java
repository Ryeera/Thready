package de.Ryeera.Thready;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class SQLConfigManager {

	private static final int CONFIG_VERSION = 2;
	
	private SQLConnector sql;
	private DragoLogger logger;
	private ResultSet cache;
	private long cacheChannel = 0;
	private ResultSet statsCache;
	private long statsCacheChannel = 0;
	
	public SQLConfigManager(String host, int port, String user, String pass, String database, DragoLogger logger) {
		this.logger = logger;
		sql = new SQLConnector(host, port, user, pass, database, logger);
		if (sql.executeUpdate("CREATE TABLE IF NOT EXISTS `thready`.`thready` ( "
				+ "`config-version` INT NOT NULL COMMENT 'The config-version currently used by this database.' "
				+ ") ENGINE = InnoDB COMMENT = 'Only has one row. Contains all settings for Thready itself.';"))
			this.logger.log("INFO", "New table \"thready\" created.");
		if (sql.executeUpdate("CREATE TABLE IF NOT EXISTS `thready`.`channels` ( "
				+ "`guild` BIGINT(20) UNSIGNED NOT NULL COMMENT 'The ID of the Guild this Channel belongs to.' , "
				+ "`channel` BIGINT(20) UNSIGNED NOT NULL COMMENT 'The ID of the Channel.' , "
				+ "`config` SMALLINT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'The binary-encoded config for this Channel.' , "
				+ "`enabled` TINYINT(1) UNSIGNED NOT NULL DEFAULT FALSE COMMENT 'Whether Thready for this Channel is enabled or not.' , "
				+ "PRIMARY KEY (`channel`)"
				+ ") ENGINE = InnoDB COMMENT = 'The configuration for each Channel.';"))
			this.logger.log("INFO", "New table \"channel\" created.");
		if (sql.executeUpdate("CREATE TABLE IF NOT EXISTS `thready`.`channel-stats` ( "
				+ "`channel` BIGINT UNSIGNED NOT NULL COMMENT 'The ID of the Channel.' , "
				+ "`count-threads` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many threads have been created in this Channel by Thready.' , "
				+ "`count-messages` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many messages have been sent in this Channel since Thready joined.' , "
				+ "`count-links` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many links have been sent in this Channel since Thready joined.' , "
				+ "`count-images` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many images have been sent in this Channel since Thready joined.' , "
				+ "`count-videos` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many videos have been sent in this Channel since Thready joined.' , "
				+ "`count-files` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many files have been sent in this Channel since Thready joined.' , "
				+ "`count-embeds` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many embeds have been sent in this Channel since Thready joined.' , "
				+ "`count-emotes` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many emotes have been sent in this Channel since Thready joined.' , "
				+ "`count-stickers` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many stickers have been sent in this Channel since Thready joined.' , "
				+ "`count-user-mentions` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many user-mentions have been sent in this Channel since Thready joined.' , "
				+ "`count-channel-mentions` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many channel-mentions have been sent in this Channel since Thready joined.' , "
				+ "`count-role-mentions` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'How many role-mentions have been sent in this Channel since Thready joined.' , "
				+ "PRIMARY KEY (`channel`)"
				+ ") ENGINE = InnoDB COMMENT = 'Stats about Channels.';"))
			this.logger.log("INFO", "New table \"channel-stats\" created.");
		try {
			applyMigrations();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void applyMigrations() throws SQLException {
		ResultSet threadyConfig = sql.executeQuery("SELECT * FROM `thready`;");
		if (threadyConfig.next()) {
			int source = threadyConfig.getInt("config-version");
			if (source < CONFIG_VERSION)
				logger.log("INFO", "Outdated config detected! Applying migrations...");
			if (source == 1) {
				logger.log("INFO", "Migrating config-version 1 -> 2...");
				if (sql.executeUpdate("ALTER TABLE `channels` DROP `stat_threads_created`;"))
					logger.log("INFO", "\"channels.stat_threads_created\" deleted (no data was ever written to it, so it's save).");
				if (sql.executeUpdate("INSERT INTO `channel-stats` (`channel`) SELECT `channel` FROM `channels`;"))
					logger.log("INFO", "Set up empty stats-table for known channels.");
			}
			if (source < CONFIG_VERSION && sql.executeUpdate("UPDATE `thready` SET `config-version` = " + CONFIG_VERSION + " WHERE `config-version` = " + source + ";"))
				logger.log("INFO", "Migrations applied!");
		} else {
			if (sql.executeUpdate("INSERT INTO `thready` (`config-version`) VALUES ('" + CONFIG_VERSION + "');")) {
				logger.log("INFO", "Fresh Database created! Using config-version " + CONFIG_VERSION + ".");
			}
		}
	}
	
	private void clearCache() {
		try {
			if (cache != null && !cache.isClosed())
				cache.close();
			cache = null;
			cacheChannel = 0;
		} catch (SQLException e) {
			logger.log("WARN", "Exception while closing the ResultSet!");
			logger.logStackTrace(e);
		}
	}
	
	private ResultSet getChannelConfig(Guild guild, MessageChannel channel) throws SQLException {
		if (cacheChannel == channel.getIdLong()) {
			return cache;
		} else if (cacheChannel != 0) {
			clearCache();
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
			if (sql.executeUpdate("INSERT INTO `channel-stats` (`channel`) VALUES ('" + channel.getId() + "');")) {
				logger.log("INFO", "Created new stats for channel " + channel.getId() + " in guild " + guild.getId());
			} else {
				logger.log("ERROR", "Failed to create new stats for channel " + channel.getId() + " in guild " + guild.getId() + "!");
				throw new SQLException("Couldn't create new stats for channel " + channel.getId() + "!");
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
	
	private void clearStatsCache() {
		try {
			if (statsCache != null && !statsCache.isClosed())
				statsCache.close();
			statsCache = null;
			statsCacheChannel = 0;
		} catch (SQLException e) {
			logger.log("WARN", "Exception while closing the ResultSet!");
			logger.logStackTrace(e);
		}
	}
	
	private ResultSet getChannelStats(Guild guild, MessageChannel channel) throws SQLException {
		if (statsCacheChannel == channel.getIdLong()) {
			return statsCache;
		} else if (statsCacheChannel != 0) {
			clearStatsCache();
		}
		ResultSet channelStats = sql.executeQuery("SELECT * FROM `channel-stats` WHERE `channel`='" + channel.getId() + "';");
		if (channelStats.next()) {
			statsCache = channelStats;
			statsCacheChannel = channel.getIdLong();
			return channelStats;
		} else {
			channelStats.close();
			if (sql.executeUpdate("INSERT INTO `channels` (`guild`, `channel`) VALUES ('" + guild.getId() + "', '" + channel.getId() + "');")) {
				logger.log("INFO", "Created new config for channel " + channel.getId() + " in guild " + guild.getId());
			} else {
				logger.log("ERROR", "Failed to create a new config for channel " + channel.getId() + " in guild " + guild.getId() + "!");
				throw new SQLException("Couldn't create new config for channel " + channel.getId() + "!");
			}
			if (sql.executeUpdate("INSERT INTO `channel-stats` (`channel`) VALUES ('" + channel.getId() + "');")) {
				logger.log("INFO", "Created new stats for channel " + channel.getId() + " in guild " + guild.getId());
			} else {
				logger.log("ERROR", "Failed to create new stats for channel " + channel.getId() + " in guild " + guild.getId() + "!");
				throw new SQLException("Couldn't create new stats for channel " + channel.getId() + "!");
			}
			channelStats = sql.executeQuery("SELECT * FROM `channels` WHERE `channel`='" + channel.getId() + "';");
			if (channelStats.next()) {
				statsCache = channelStats;
				statsCacheChannel = channel.getIdLong();
				return channelStats;
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
		if (channel.getIdLong() == cacheChannel)
			clearCache();
		return sql.executeUpdate("UPDATE `channels` SET `enabled`=" + enabled + " WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean enableThreadingOption(MessageChannel channel, int option) {
		if (channel.getIdLong() == cacheChannel)
			clearCache();
		return sql.executeUpdate("UPDATE `channels` SET `config`=(SELECT `config` FROM `channels` WHERE `channel`='" + channel.getId() + "')+" + option + " WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean disableThreadingOption(MessageChannel channel, int option) {
		if (channel.getIdLong() == cacheChannel)
			clearCache();
		return sql.executeUpdate("UPDATE `channels` SET `config`=(SELECT `config` FROM `channels` WHERE `channel`='" + channel.getId() + "')-" + option + " WHERE `channel`='" + channel.getId() + "'");
	}
	
	public int getThreadCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-threads");
	}
	
	public int getMessageCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-messages");
	}
	
	public int getLinkCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-links");
	}
	
	public int getImageCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-images");
	}
	
	public int getVideoCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-videos");
	}
	
	public int getFileCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-files");
	}
	
	public int getEmbedCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-embeds");
	}
	
	public int getEmoteCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-emotes");
	}
	
	public int getStickerCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-stickers");
	}
	
	public int getUserMentionCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-user-mentions");
	}
	
	public int getChannelMentionCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-channel-mentions");
	}
	
	public int getRoleMentionCount(Guild guild, MessageChannel channel) throws SQLException {
		return getChannelStats(guild, channel).getInt("count-role-mentions");
	}
	
	public boolean addThread(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-threads`=(SELECT `count-threads` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addMessage(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-messages`=(SELECT `count-messages` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addLink(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-links`=(SELECT `count-links` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addImage(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-images`=(SELECT `count-images` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addVideo(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-videos`=(SELECT `count-videos` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addFile(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-files`=(SELECT `count-files` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addEmbed(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-embeds`=(SELECT `count-embeds` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addEmote(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-emotes`=(SELECT `count-emotes` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addSticker(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-stickers`=(SELECT `count-stickers` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addUserMention(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-user-mentions`=(SELECT `count-user-mentions` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addChannelMention(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-channel-mentions`=(SELECT `count-channel-mentions` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
	
	public boolean addRoleMention(MessageChannel channel) {
		if (channel.getIdLong() == statsCacheChannel)
			clearStatsCache();
		return sql.executeUpdate("UPDATE `channel-stats` SET `count-role-mentions`=(SELECT `count-role-mentions` FROM `channel-stats` WHERE `channel`='" + channel.getId() + "')+1 WHERE `channel`='" + channel.getId() + "'");
	}
}
