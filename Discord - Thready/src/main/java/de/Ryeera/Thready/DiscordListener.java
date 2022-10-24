package de.Ryeera.Thready;

import java.sql.SQLException;
import java.util.List;

import de.Ryeera.libs.DragoLogger;
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
		logger.log("INFO", "Command received from " + sender.getAsTag() + ": " + event.getCommandPath());
		Guild guild = event.getGuild();
		MessageChannel channel = event.getMessageChannel();
		String[] command = event.getCommandPath().split("/");
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
			event.reply("✅ Stats-Page will be here soon™!").setEphemeral(true).queue();
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
		} else if (event.getButton().getId().startsWith("enableOption:")) {
			SelectMenu menu = (SelectMenu) actionRows.get(0).getActionComponents().get(0);
			int config = Integer.parseInt(menu.getId().split(":")[2]);
			sql.enableThreadingOption(channel, config);
			List<ActionComponent> buttons = actionRows.get(1).getActionComponents();
			ActionRow buttonrow = actionRows.get(1);
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
		} else if (event.getButton().getId().startsWith("disableOption:")) {
			SelectMenu menu = (SelectMenu) actionRows.get(0).getActionComponents().get(0);
			int config = Integer.parseInt(menu.getId().split(":")[2]);
			sql.disableThreadingOption(channel, config);
			List<ActionComponent> buttons = actionRows.get(1).getActionComponents();
			ActionRow buttonrow = actionRows.get(1);
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
		if (event.getChannelType() == ChannelType.CATEGORY) return;
		if (event.getChannelType() == ChannelType.FORUM) return;
		if (event.getChannelType() == ChannelType.STAGE) return;
		if (event.getChannelType() == ChannelType.UNKNOWN) return;
		if (event.getChannelType() == ChannelType.VOICE) return;
		
		Message message = event.getMessage();
		MessageChannel channel = event.getChannel();
		Guild guild = event.getGuild();
		
		try {
			int threadingConfig = sql.getThreadingConfig(guild, channel);
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
					message.createThreadChannel("Comments | Discussion").queue(t -> {
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
