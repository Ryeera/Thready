package de.Ryeera.Thready;

import de.Ryeera.libs.DragoLogger;
import net.dv8tion.jda.api.GatewayEncoding;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordConnector {
	
	private JDA jda;
	private DragoLogger logger;
	
	public DiscordConnector(String token, ListenerAdapter listener, DragoLogger logger) {
		this.logger = logger;
		setupJDA(token, listener);
		setupCommands();
	}
	
	private void setupJDA(String token, ListenerAdapter listener) {
		JDABuilder builder = JDABuilder.create(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT);
		builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.EMOJI, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.ROLE_TAGS, CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
		builder.setMemberCachePolicy(MemberCachePolicy.NONE);
		builder.setChunkingFilter(ChunkingFilter.NONE);
		builder.setGatewayEncoding(GatewayEncoding.ETF);
		builder.setStatus(OnlineStatus.ONLINE);
		builder.setToken(token);
		builder.setActivity(Activity.watching("for Threads to create"));
		builder.addEventListeners(listener);
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
		} catch (InterruptedException e) {
			logger.log("ERROR", "Couldn't set up Discord-Connection! Exiting...");
			logger.logStackTrace(e);
			System.exit(1);
		}
	}
	
	private void setupCommands() { 
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
	}
}
