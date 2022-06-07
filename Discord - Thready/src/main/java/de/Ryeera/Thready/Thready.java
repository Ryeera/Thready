package de.Ryeera.Thready;

import java.io.File;
import java.io.IOException;

import javax.security.auth.login.LoginException;

import org.json.JSONObject;

import de.Ryeera.libs.DragoLogger;
import de.Ryeera.libs.JSONUtils;
import de.Ryeera.libs.Utils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Thready extends ListenerAdapter{
	private static final String VERSION = "0.0.1";
	
	private static DragoLogger logger;
	
	private static final File configFile = new File("thready.json");
	private static JSONObject config = new JSONObject();
	
	private static JDA jda;
	
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
		
		logger.log("INFO", "Setting up Discord-Connection...");
		JDABuilder builder = JDABuilder.create(GatewayIntent.GUILD_MESSAGES);
		builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.EMOTE, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS, CacheFlag.ROLE_TAGS, CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
		builder.setToken(config.getString("token"));
		builder.setActivity(Activity.watching("for Threads to create"));
		builder.addEventListeners(new Thready());
		try {
			jda = builder.build();
			jda.awaitReady();
		} catch (LoginException | InterruptedException e) {
			logger.logStackTrace(e);
			System.exit(1);
		}
		
		logger.log("INFO", "Creating commands...");
		CommandListUpdateAction commands = jda.updateCommands();
		
		commands.addCommands(
			Commands.slash("config", "Configure auto-threading for this channel."),
			Commands.slash("stats", "Show stats about Thready.")
		);
		
		commands.queue(c -> {
			logger.log("INFO", c.size() + " Commands set up successfully!");
		}, f -> {
			logger.log("INFO", "Failed to set up commands! Halting...");
			jda.shutdownNow();
			System.exit(3);
		});
		
		logger.log("INFO", "Thready started! I'm watching...");
	}
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		User sender = event.getUser();
		logger.log("INFO", "Command received from " + sender.getAsTag() + ": " + event.getCommandPath());
		Guild guild = event.getGuild();
		Member member = guild == null ? null : guild.getMember(sender);
		String[] command = event.getCommandPath().split("/");
		if (command[0].equals("config")) {
			logger.log("DEBUG", "Channel Type = " + event.getChannelType().toString());
			if (event.getChannelType() == ChannelType.GUILD_PUBLIC_THREAD || event.getChannelType() == ChannelType.GUILD_PRIVATE_THREAD || event.getChannelType() == ChannelType.GUILD_NEWS_THREAD) {
				event.reply("🚫 You can't use threads within threads (kekw)!").queue();
				return;
			}
			event.reply("✅ Config-Dialogue will be here soon™!").queue();
		} else if (command[0].equals("")) {
			event.reply("✅ Config-Page will be here soon™!").queue();
		} else {
			event.reply("🚫 This feature has not been implemented yet!").queue();
		}
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.isFromThread()) return;
		Message message = event.getMessage();
		TextChannel channel = event.getTextChannel();
		JSONObject channelConfig = config.getJSONObject("channels").optJSONObject(channel.getId());
		if (channelConfig == null) return;
		boolean hasImage = false;
		boolean hasVideo = false;
		boolean hasLink = message.getContentRaw().contains("https://") || message.getContentRaw().contains("http://");
		for (Attachment a : message.getAttachments()) {
			if (a.isImage()) hasImage = true;
			if (a.isVideo()) hasVideo = true;
		}
		if (channelConfig.getBoolean("videos") && hasVideo) {
			logger.log("INFO", "Creating video-thread for message " + message.getId() + "...");
			message.createThreadChannel("Comments").queue(t -> {
				t.addThreadMember(message.getAuthor()).queue();
			});
		} else if (channelConfig.getBoolean("images") && hasImage) {
			logger.log("INFO", "Creating image-thread for message " + message.getId() + "...");
			message.createThreadChannel("Comments").queue(t -> {
				t.addThreadMember(message.getAuthor()).queue();
			});
		} else if (channelConfig.getBoolean("links") && hasLink) {
			logger.log("INFO", "Creating link-thread for message " + message.getId() + "...");
			message.createThreadChannel("Comments").queue(t -> {
				t.addThreadMember(message.getAuthor()).queue();
			});
		} else if (channelConfig.getBoolean("messages")) {
			logger.log("INFO", "Creating message-thread for message " + message.getId() + "...");
			message.createThreadChannel("Comments").queue(t -> {
				t.addThreadMember(message.getAuthor()).queue();
			});
		}
	}
}
