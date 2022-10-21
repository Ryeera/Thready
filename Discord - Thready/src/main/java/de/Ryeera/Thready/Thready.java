package de.Ryeera.Thready;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import de.Ryeera.libs.DragoLogger;
import de.Ryeera.libs.JSONUtils;
import de.Ryeera.libs.SQLConnector;
import de.Ryeera.libs.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel.AutoArchiveDuration;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Thready extends ListenerAdapter{
	private static final String VERSION = "0.1.0";
	
	private static DragoLogger logger;
	
	private static final File configFile = new File("thready.json");
	private static JSONObject config = new JSONObject();
	
	private static JDA jda;
	private static SQLConnector sql;
	
	public static void main(String[] args) {
		try {
			new File("logs").mkdir();
			logger = new DragoLogger(new File("logs" + File.separator + "thready_" + Utils.formatTime(System.currentTimeMillis(), "yyyyMMdd_HHmmss") + ".log"));
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		logger.log("INFO", "Starting Thready v" + VERSION + "...");
		logger.log("INFO", "Setting up Configuration...");
		try {
			config = JSONUtils.readJSON(configFile);
		} catch (IOException e) {
			logger.log("SEVERE", "Couldn't read config-file! Halting...");
			logger.logStackTrace(e);
			return;
		}
		logger.log("INFO", "Config loaded.");
		
		logger.log("INFO", "Setting up SQL-Connection...");
		JSONObject sqlConfig = config.getJSONObject("sql");
		sql = new SQLConnector(
				sqlConfig.getString("host"), 
				sqlConfig.getInt("port"), 
				sqlConfig.getString("user"), 
				sqlConfig.getString("pass"), 
				sqlConfig.getString("database")
		);
		if (sql.executeUpdate("CREATE TABLE IF NOT EXISTS `thready`.`channels` ( "
				+ "`guild` BIGINT(20) UNSIGNED NOT NULL COMMENT 'The ID of the Guild this Channel belongs to.' , "
				+ "`channel` BIGINT(20) UNSIGNED NOT NULL COMMENT 'The ID of the Channel.' , "
				+ "`config` SMALLINT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'The binary-encoded config for this Channel.' , "
				+ "`enabled` TINYINT(1) UNSIGNED NOT NULL DEFAULT FALSE COMMENT 'Whether Thready for this Channel is enabled or not.' , "
				+ "`stat_threads_created` INT UNSIGNED NOT NULL DEFAULT '0' COMMENT 'Stats: How many threads were created by Thready in this Channel.' , "
				+ "PRIMARY KEY (`channel`)"
				+ ") ENGINE = InnoDB COMMENT = 'Information including config and stats for each Channel.';")) {
			logger.log("INFO", "New table created.");
		}
		logger.log("INFO", "SQL-Connection established.");
		
		logger.log("INFO", "Setting up Discord-Connection...");
		JDABuilder builder = JDABuilder.create(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
		builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.EMOJI, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.ROLE_TAGS, CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
		builder.setMemberCachePolicy(MemberCachePolicy.NONE);
		builder.setChunkingFilter(ChunkingFilter.NONE);
		builder.setGatewayEncoding(GatewayEncoding.ETF);
		builder.setStatus(OnlineStatus.ONLINE);
		builder.setToken(config.getString("token"));
		builder.setActivity(Activity.watching("for Threads to create"));
		builder.addEventListeners(new Thready());
		try {
			jda = builder.build();
			jda.awaitStatus(Status.INITIALIZING);
			logger.log("INFO", "Initializing...");
			jda.awaitStatus(Status.INITIALIZED);
			logger.log("INFO", "Initialized.");
			jda.awaitStatus(Status.LOGGING_IN);
			logger.log("INFO", "Logging in...");
			jda.awaitStatus(Status.CONNECTING_TO_WEBSOCKET);
			logger.log("INFO", "Logged in.");
			logger.log("INFO", "Connecting to Websocket...");
			jda.awaitStatus(Status.IDENTIFYING_SESSION);
			logger.log("INFO", "Connected to Websocket.");
			logger.log("INFO", "Identifying Session...");
			jda.awaitStatus(Status.AWAITING_LOGIN_CONFIRMATION);
			logger.log("INFO", "Session identified.");
			logger.log("INFO", "Awaiting login confirmation...");
			jda.awaitStatus(Status.LOADING_SUBSYSTEMS);
			logger.log("INFO", "Login confirmed.");
			logger.log("INFO", "Loading Subsystems...");
			jda.awaitStatus(Status.CONNECTED);
			logger.log("INFO", "Subsystems loaded.");
			logger.log("INFO", "Connected.");
			jda.awaitReady();
			logger.log("INFO", "Discord-Connection established.");
		} catch (InterruptedException e) {
			logger.log("ERROR", "Couldn't set up Discord-Connection! Exiting...");
			logger.logStackTrace(e);
			System.exit(1);
		}
		
		logger.log("INFO", "Creating commands...");
		CommandListUpdateAction commands = jda.updateCommands();
		commands.addCommands(
			Commands.slash("config", "Configure Thready for this channel.")
					.setDescriptionLocalization(DiscordLocale.GERMAN, "Konfiguriere Thready für diesen Kanal.")
					.setNameLocalization(DiscordLocale.GERMAN, "konfiguration")
					.setDefaultPermissions(DefaultMemberPermissions.DISABLED)
					.setGuildOnly(true),
			Commands.slash("stats", "Show stats about Thready.")
					.setDescriptionLocalization(DiscordLocale.GERMAN, "Zeige Statistiken über Thready.")
					.setNameLocalization(DiscordLocale.GERMAN, "statistiken")
					.setDefaultPermissions(DefaultMemberPermissions.ENABLED)
					.setGuildOnly(true)
		);
		int c = commands.complete().size();
		if (c < 2) {
			logger.log("INFO", "Failed to set up " + (2-c) + " commands! Exiting...");
			jda.shutdownNow();
			System.exit(3);
		} else {
			logger.log("INFO", c + " Commands set up successfully!");
		}
		
		logger.log("INFO", "Setting up scheduled tasks...");
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
			if (System.currentTimeMillis() - configFile.lastModified() < 90000)
				reloadConfig();
		}, 1, 1, TimeUnit.MINUTES);
		logger.log("INFO", "1 Scheduled Task set up.");
		
		logger.log("INFO", "Thready v" + VERSION + " started! I'm watching...");
	}
	
	private static void reloadConfig() {
		logger.log("INFO", "Reloading Configuration...");
		try {
			config = JSONUtils.readJSON(configFile);
		} catch (IOException e) {
			logger.log("SEVERE", "Couldn't read config-file! Halting...");
			logger.logStackTrace(e);
			return;
		}
	}
	
	private static ResultSet getChannelConfig(Guild guild, MessageChannel channel) throws SQLException {
		ResultSet channelConfig = sql.executeQuery("SELECT * FROM `channels` WHERE `channel`='" + channel.getId() + "';");
		if (channelConfig.next()) {
			return channelConfig;
		} else {
			channelConfig.close();
			sql.executeUpdate("INSERT INTO `channels` (`guild`, `channel`) VALUES ('" + guild.getId() + "', '" + channel.getId() + "');");
			channelConfig = sql.executeQuery("SELECT * FROM `channels` WHERE `channel`='" + channel.getId() + "';");
			if (channelConfig.next()) {
				return channelConfig;
			} else {
				throw new SQLException("The Data could not be retrieved from the server!");
			}
		}
	}
	
	private static MessageEmbed getChannelConfigEmbed(Guild guild, MessageChannel channel) throws SQLException {
		ResultSet channelConfig = getChannelConfig(guild, channel);
		
		int threadingConfig = channelConfig.getInt("config");
		boolean enabled = channelConfig.getBoolean("enabled");
		channelConfig.close();
		
		return new EmbedBuilder()
		.setColor(new Color(0x58, 0x65, 0xF2))
		.setDescription("Thready uses the following config for this channel. "
				+ "Use the selection-menu to select which option to change and then click either the Enable or Disable button. "
				+ "To completely enable or disable Thready in this channel, use the bottom button.")
		.setFooter("Thready v" + VERSION + " by Ryeera", "https://cdn.discordapp.com/avatars/918245386441863218/8db6a3786a9d495f4fbe2cec55521ac4.png?size=64")
		.setTimestamp(Instant.now())
		.setTitle("Thready Config for #" + channel.getName())
		.addField("Thready Enabled", enabled ? "✅ Yes" : "🚫 No", false)
		.addField("All Messages", 		(threadingConfig & 1) 	> 0 ? "✅ Yes" : "🚫 No", true)
		.addField("Links", 				(threadingConfig & 2) 	> 0 ? "✅ Yes" : "🚫 No", true)
		.addField("Images", 			(threadingConfig & 4) 	> 0 ? "✅ Yes" : "🚫 No", true)
		.addField("Videos", 			(threadingConfig & 8) 	> 0 ? "✅ Yes" : "🚫 No", true)
		.addField("Files", 				(threadingConfig & 16) 	> 0 ? "✅ Yes" : "🚫 No", true)
		.addField("Embeds", 			(threadingConfig & 32) 	> 0 ? "✅ Yes" : "🚫 No", true)
		.addField("Emotes", 			(threadingConfig & 64) 	> 0 ? "✅ Yes" : "🚫 No", true)
		.addField("User-Mentions", 		(threadingConfig & 128) > 0 ? "✅ Yes" : "🚫 No", true)
		.addField("Channel-Mentions", 	(threadingConfig & 256) > 0 ? "✅ Yes" : "🚫 No", true)
		.addField("Role-Mentions", 		(threadingConfig & 512) > 0 ? "✅ Yes" : "🚫 No", true)
		.build();
	}
	
	private static boolean setEnabled(MessageChannel channel, boolean enabled) {
		return sql.executeUpdate("UPDATE `channels` SET `enabled`=" + enabled + " WHERE `channel`='" + channel.getId() + "'");
	}
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		User sender = event.getUser();
		logger.log("INFO", "Command received from " + sender.getAsTag() + ": " + event.getCommandPath());
		Guild guild = event.getGuild();
		MessageChannel channel = event.getMessageChannel();
		String[] command = event.getCommandPath().split("/");
		if (command[0].equals("config")) {
			if (event.getChannelType() == ChannelType.GUILD_PUBLIC_THREAD || event.getChannelType() == ChannelType.GUILD_PRIVATE_THREAD || event.getChannelType() == ChannelType.GUILD_NEWS_THREAD) {
				event.reply("🚫 You can't use threads within threads!").queue();
				return;
			}
			
			SelectMenu menu = SelectMenu.create("config:" + channel.getId() + ":0")
				     .setPlaceholder("Select message-type...")
				     .addOption("Messages", 		"1", 	"En-/Disable Thready for all messages.")
				     .addOption("Links", 			"2", 	"En-/Disable Thready for messages containing links.")
				     .addOption("Images", 			"4", 	"En-/Disable Thready for messages containing images.")
				     .addOption("Videos", 			"8", 	"En-/Disable Thready for messages containing videos.")
				     .addOption("Files", 			"16", 	"En-/Disable Thready for messages containing files.")
				     .addOption("Embeds", 			"32", 	"En-/Disable Thready for messages containing embeds.")
				     .addOption("Emotes", 			"64", 	"En-/Disable Thready for messages containing emotes.")
				     .addOption("User-Mentions", 	"128", 	"En-/Disable Thready for messages containing user-mentions.")
				     .addOption("Channel-Mentions", "256", 	"En-/Disable Thready for messages containing channel-mentions.")
				     .addOption("Role-Mentions", 	"512", 	"En-/Disable Thready for messages containing role-mentions.")
				     .build();
			
			Button buttonEnable = Button.success("enableOption:" + channel.getId(), "Enable").asDisabled();
			Button buttonDisable = Button.danger("disableOption:" + channel.getId(), "Disable").asDisabled();
			
			try {
				ResultSet channelConfig = getChannelConfig(guild, channel);
				
				Button buttonChannel = channelConfig.getBoolean("enabled") ? 
						Button.danger("disable:" + channel.getId(), "Disable Thready in this Channel"):
						Button.success("enable:" + channel.getId(), "Enable Thready in this Channel");
				
				event.replyEmbeds(getChannelConfigEmbed(guild, channel))
						.addActionRow(menu)
						.addActionRow(buttonEnable, buttonDisable)
						.addActionRow(buttonChannel)
						.setEphemeral(true)
						.queue();
			} catch (SQLException e) {
				event.reply("🚫 There was an error trying to get your config! Please try again!").queue();
				logger.log("ERROR", "An error occured trying to get the config for channel " + channel.getId() + " in guild " + guild.getId() + "!");
				logger.logStackTrace(e);
			}
		} else if (command[0].equals("stats")) {
			event.reply("✅ Stats-Page will be here soon™!").setEphemeral(true).queue();
		} else {
			event.reply("🚫 This feature has not been implemented yet!").setEphemeral(true).queue();
		}
	}
	
	@Override
	public void onSelectMenuInteraction(SelectMenuInteractionEvent event) {
		Guild guild = event.getGuild();
		MessageChannel channel = event.getMessageChannel();
		List<ActionComponent> buttons = event.getMessage().getActionRows().get(1).getActionComponents();
		ActionRow buttonrow = event.getMessage().getActionRows().get(1);
		ActionRow buttonrow2 = event.getMessage().getActionRows().get(2);
		try {
			ResultSet channelConfig = getChannelConfig(guild, channel);
			int threadingConfig = channelConfig.getInt("config");
			if ((Integer.parseInt(event.getSelectedOptions().get(0).getValue()) & threadingConfig) > 0) {
				buttonrow = ActionRow.of(buttons.get(0).asDisabled(), buttons.get(1).asEnabled());
			} else {
				buttonrow = ActionRow.of(buttons.get(0).asEnabled(), buttons.get(1).asDisabled());
			}
			event.editComponents(
				ActionRow.of(
					event.getSelectMenu().createCopy()
						.setPlaceholder(event.getSelectedOptions().get(0).getLabel())
						.setId("config:" + channel.getId() + ":" + event.getSelectedOptions().get(0).getValue())
						.build()
				), 
				buttonrow, 
				buttonrow2
			).queue();
		} catch (SQLException e) {
			event.reply("🚫 There was an error trying to get your config! Please try again!").queue();
			logger.log("ERROR", "An error occured trying to get the config for channel " + channel.getId() + " in guild " + event.getGuild().getId() + "!");
			logger.logStackTrace(e);
		}
	}
	
	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		Guild guild = event.getGuild();
		MessageChannel channel = event.getMessageChannel();
		if (event.getButton().getId().startsWith("enable:")) {
			setEnabled(channel, true);
			event.editButton(Button.danger("disable:" + channel.getId(), "Disable Thready in this Channel")).queue();
		} else if (event.getButton().getId().startsWith("disable:")) {
			setEnabled(channel, false);
			event.editButton(Button.success("enable:" + channel.getId(), "Enable Thready in this Channel")).queue();
		} else if (event.getButton().getId().startsWith("enableOption:")) {
			SelectMenu menu = (SelectMenu) event.getMessage().getActionRows().get(0).getActionComponents().get(0);
			int config = Integer.parseInt(menu.getId().split(":")[2]);
			sql.executeUpdate("UPDATE `channels` SET `config`=(SELECT `config` FROM `channels` WHERE `channel`='" + channel.getId() + "')+" + config + " WHERE `channel`='" + channel.getId() + "'");
			List<ActionComponent> buttons = event.getMessage().getActionRows().get(1).getActionComponents();
			ActionRow buttonrow = event.getMessage().getActionRows().get(1);
			try {
				ResultSet channelConfig = getChannelConfig(guild, channel);
				int threadingConfig = channelConfig.getInt("config");
				if ((config & threadingConfig) > 0) {
					buttonrow = ActionRow.of(buttons.get(0).asDisabled(), buttons.get(1).asEnabled());
				} else {
					buttonrow = ActionRow.of(buttons.get(0).asEnabled(), buttons.get(1).asDisabled());
				}
				event.editComponents(event.getMessage().getActionRows().get(0), buttonrow, event.getMessage().getActionRows().get(2)).queue();
			} catch (SQLException e) {
				event.reply("🚫 There was an error trying to get your config! Please try again!").queue();
				logger.log("ERROR", "An error occured trying to get the config for channel " + channel.getId() + " in guild " + event.getGuild().getId() + "!");
				logger.logStackTrace(e);
			}
		} else if (event.getButton().getId().startsWith("disableOption:")) {
			SelectMenu menu = (SelectMenu) event.getMessage().getActionRows().get(0).getActionComponents().get(0);
			int config = Integer.parseInt(menu.getId().split(":")[2]);
			sql.executeUpdate("UPDATE `channels` SET `config`=(SELECT `config` FROM `channels` WHERE `channel`='" + channel.getId() + "')-" + config + " WHERE `channel`='" + channel.getId() + "'");
			List<ActionComponent> buttons = event.getMessage().getActionRows().get(1).getActionComponents();
			ActionRow buttonrow = event.getMessage().getActionRows().get(1);
			try {
				ResultSet channelConfig = getChannelConfig(guild, channel);
				int threadingConfig = channelConfig.getInt("config");
				if ((config & threadingConfig) > 0) {
					buttonrow = ActionRow.of(buttons.get(0).asDisabled(), buttons.get(1).asEnabled());
				} else {
					buttonrow = ActionRow.of(buttons.get(0).asEnabled(), buttons.get(1).asDisabled());
				}
				event.editComponents(event.getMessage().getActionRows().get(0), buttonrow, event.getMessage().getActionRows().get(2)).queue();
			} catch (SQLException e) {
				event.reply("🚫 There was an error trying to get your config! Please try again!").queue();
				logger.log("ERROR", "An error occured trying to get the config for channel " + channel.getId() + " in guild " + event.getGuild().getId() + "!");
				logger.logStackTrace(e);
			}
		} else {
			event.reply("🚫 Unknown Button pressed. Please tell Ryeera about this...").queue();
		}
		try {
			event.getHook().editOriginalEmbeds(getChannelConfigEmbed(guild, channel)).queue();
		} catch (SQLException e) {
			event.getHook().editOriginal("🚫 There was an error trying to get your config! Please try again!").queue();
			logger.log("ERROR", "An error occured trying to get the config for channel " + channel.getId() + " in guild " + guild.getId() + "!");
			logger.logStackTrace(e);
		}
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		// Don't react to own messages (will cause loops sometimes).
		if (event.getAuthor().getIdLong() == jda.getSelfUser().getIdLong()) return;
		// Threads can't be created within these types of channels.
		if (event.isFromThread()) return;
		if (event.getChannelType() == ChannelType.CATEGORY) return;
		if (event.getChannelType() == ChannelType.FORUM) return;
		if (event.getChannelType() == ChannelType.STAGE) return;
		if (event.getChannelType() == ChannelType.UNKNOWN) return;
		if (event.getChannelType() == ChannelType.VOICE) return;
		
		Message message = event.getMessage();
		MessageChannel channel = event.getChannel();
		Guild guild = event.getGuild();
		
		try {
			ResultSet channelConfig = getChannelConfig(guild, channel);
			int threadingConfig = channelConfig.getInt("config");
			if (threadingConfig != 0) {
				int messageConfig = 1;
				if (message.getContentRaw().contains("https://") || message.getContentRaw().contains("http://"))
					messageConfig += 2;
				for (Attachment a : message.getAttachments()) {
					if (a.isImage())
						messageConfig += 4;
					else if (a.isVideo())
						messageConfig += 8;
					else
						messageConfig += 16;
				}
				if (message.getEmbeds().size() > 0)
					messageConfig += 32;
				if (message.getMentions().getCustomEmojis().size() > 0)
					messageConfig += 64;
				if (message.getMentions().getUsers().size() > 0)
					messageConfig += 128;
				if (message.getMentions().getChannels().size() > 0)
					messageConfig += 256;
				if (message.getMentions().getRoles().size() > 0)
					messageConfig += 512;
				if ((messageConfig & threadingConfig) > 0) {
					logger.log("INFO", "Creating thread for message " + message.getId() + "...");
					message.createThreadChannel("Comments|Discussion").queue(t -> {
						t.addThreadMember(message.getAuthor()).queue();
						t.getManager().setAutoArchiveDuration(AutoArchiveDuration.TIME_24_HOURS).queue();
					});
				}
			}
		} catch (SQLException e) {
			logger.log("ERROR", "An error occured trying to get the config for channel " + channel.getId() + " in guild " + guild.getId() + "!");
			logger.logStackTrace(e);
		}
	}
}
