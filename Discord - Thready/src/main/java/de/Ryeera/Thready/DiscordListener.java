package de.Ryeera.Thready;

import java.sql.SQLException;
import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel.AutoArchiveDuration;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

public class DiscordListener extends ListenerAdapter {

	private DragoLogger logger;
	private SQLConfigManager sql;
	
	public DiscordListener(SQLConfigManager sql, DragoLogger logger) {
		this.sql = sql;
		this.logger = logger;
	}
	
	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		User sender = event.getUser();
		logger.log("INFO", "Command received from " + sender.getGlobalName() + " (" + sender.getId() + "): " + event.getFullCommandName());
		Guild guild = event.getGuild();
		MessageChannel channel = event.getMessageChannel();
		String[] command = event.getFullCommandName().split(" ");
		if (command[0].equals("config")) {
			if (event.getChannelType().isThread()) {
				event.reply("🚫 You can't use threads within threads!").queue();
				return;
			}
			
			SelectMenu menu = ThreadyUtils.getOptionMenu(channel);
			Button buttonEnable = Button.success("enableOption:" + channel.getId(), "Enable").asDisabled();
			Button buttonDisable = Button.danger("disableOption:" + channel.getId(), "Disable").asDisabled();
			
			try {
				Button buttonChannel = sql.isEnabled(guild, channel) ? 
						Button.danger("disable:" + channel.getId(), "Disable Thready in this Channel"):
						Button.success("enable:" + channel.getId(), "Enable Thready in this Channel");
				
				event.replyEmbeds(ThreadyUtils.getChannelConfigEmbed(guild, channel))
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
			event.replyEmbeds(ThreadyUtils.getChannelStatsEmbed(guild, channel)).setEphemeral(true).queue();
		} else {
			event.reply("🚫 This feature has not been implemented yet!").setEphemeral(true).queue();
		}
	}
	
	@Override
	public void onStringSelectInteraction(StringSelectInteractionEvent event) {
		Guild guild = event.getGuild();
		MessageChannel channel = event.getMessageChannel();
		List<ActionComponent> buttons = event.getMessage().getActionRows().get(1).getActionComponents();
		ActionRow buttonrow = event.getMessage().getActionRows().get(1);
		try {
			if ((Integer.parseInt(event.getSelectedOptions().get(0).getValue()) & sql.getThreadingConfig(guild, channel)) > 0) {
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
				event.getMessage().getActionRows().get(2)
			).queue();
		} catch (SQLException e) {
			event.reply("🚫 There was an error trying to get your config! Please try again!").queue();
			logger.log("ERROR", "An error occured trying to get the config for channel " + channel.getId() + " in guild " + guild.getId() + "!");
			logger.logStackTrace(e);
		}
	}
	
	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		Guild guild = event.getGuild();
		MessageChannel channel = event.getMessageChannel();
		List<ActionRow> actionRows = event.getMessage().getActionRows();
		if (event.getButton().getId().startsWith("enable:")) {
			sql.setEnabled(channel, true);
			event.editButton(Button.danger("disable:" + channel.getId(), "Disable Thready in this Channel")).queue();
		} else if (event.getButton().getId().startsWith("disable:")) {
			sql.setEnabled(channel, false);
			event.editButton(Button.success("enable:" + channel.getId(), "Enable Thready in this Channel")).queue();
		} else if (event.getButton().getId().contains("Option:")) {
			SelectMenu menu = (SelectMenu) actionRows.get(0).getActionComponents().get(0);
			int config = Integer.parseInt(menu.getId().split(":")[2]);
			if (event.getButton().getId().startsWith("enable")) {
				sql.enableThreadingOption(channel, config);
			} else {
				sql.disableThreadingOption(channel, config);
			}
			List<ActionComponent> buttons = actionRows.get(1).getActionComponents();
			ActionRow buttonrow;
			try {
				if ((config & sql.getThreadingConfig(guild, channel)) > 0) {
					buttonrow = ActionRow.of(buttons.get(0).asDisabled(), buttons.get(1).asEnabled());
				} else {
					buttonrow = ActionRow.of(buttons.get(0).asEnabled(), buttons.get(1).asDisabled());
				}
				event.editComponents(actionRows.get(0), buttonrow, actionRows.get(2)).queue();
			} catch (SQLException e) {
				event.reply("🚫 There was an error trying to get your config! Please try again!").queue();
				logger.log("ERROR", "An error occured trying to get the config for channel " + channel.getId() + " in guild " + event.getGuild().getId() + "!");
				logger.logStackTrace(e);
			}
		} else {
			event.reply("🚫 Unknown Button pressed. Please tell Ryeera about this...").queue();
		}
		event.getHook().editOriginalEmbeds(ThreadyUtils.getChannelConfigEmbed(guild, channel)).queue();
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		// Threads can't be created within these types of channels.
		if (event.isFromThread()) return;
		// ChannelTypes are whitelisted to prioritize stability of the bot over support for new ChannelTypes
		if (!event.isFromType(ChannelType.NEWS)
				&& !event.isFromType(ChannelType.TEXT)) return;
		
		Message message = event.getMessage();
		MessageChannel channel = event.getChannel();
		Guild guild = event.getGuild();
		
		try {
			int threadingConfig = sql.getThreadingConfig(guild, channel);
			int messageConfig = 1;
			sql.addMessage(channel);
			if (message.getContentRaw().contains("https://") || message.getContentRaw().contains("http://")) {
				messageConfig += 2;
				sql.addLink(channel);
			}
			boolean hasImage = false, hasVideo = false;
			for (Attachment a : message.getAttachments()) {
				if (a.isImage())
					hasImage = true;
				else if (a.isVideo())
					hasVideo = true;
			}
			if (hasImage) {
				messageConfig += 4;
				sql.addImage(channel);
			}
			if (hasVideo) {
				messageConfig += 8;
				sql.addVideo(channel);
			}
			if (message.getAttachments().size() > 0) {
				messageConfig += 16;
				sql.addFile(channel);
			}
			if (message.getEmbeds().size() > 0) {
				messageConfig += 32;
				sql.addEmbed(channel);
			}
			if (message.getMentions().getCustomEmojis().size() > 0) {
				messageConfig += 64;
				sql.addEmote(channel);
			}
			if (message.getMentions().getUsers().size() > 0) {
				messageConfig += 128;
				sql.addUserMention(channel);
			}
			if (message.getMentions().getChannels().size() > 0) {
				messageConfig += 256;
				sql.addChannelMention(channel);
			}
			if (message.getMentions().getRoles().size() > 0) {
				messageConfig += 512;
				sql.addRoleMention(channel);
			}
			if (message.getStickers().size() > 0) {
				messageConfig += 1024;
				sql.addSticker(channel);
			}
			if (threadingConfig != 0 && (messageConfig & threadingConfig) > 0) {
				logger.log("INFO", "Creating thread for message " + message.getId() + "...");
				message.createThreadChannel("Comments | Discussion").queue(t -> {
					t.addThreadMember(message.getAuthor()).queue();
					t.getManager().setAutoArchiveDuration(AutoArchiveDuration.TIME_24_HOURS).queue();
				});
				sql.addThread(channel);
			}
		} catch (SQLException e) {
			logger.log("ERROR", "An error occured trying to get the config for channel " + channel.getId() + " in guild " + guild.getId() + "!");
			logger.logStackTrace(e);
		}
	}
}
