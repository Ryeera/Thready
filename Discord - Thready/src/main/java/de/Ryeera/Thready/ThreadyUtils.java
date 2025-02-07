package de.Ryeera.Thready;

import java.awt.Color;
import java.sql.SQLException;
import java.time.Instant;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

public class ThreadyUtils {

	public static MessageEmbed getChannelConfigEmbed(Guild guild, MessageChannel channel) {
		try {
			int threadingConfig = Thready.sql.getThreadingConfig(guild, channel);
			
			return new EmbedBuilder()
					.setColor(new Color(0x58, 0x65, 0xF2))
					.setDescription("Thready uses the following config for this channel. "
							+ "Use the selection-menu to select which option to change and then click either the Enable or Disable button. "
							+ "To completely enable or disable Thready in this channel, use the bottom button.")
					.setFooter("Thready v" + Thready.VERSION + " by Ryeera", "https://cdn.discordapp.com/avatars/918245386441863218/8db6a3786a9d495f4fbe2cec55521ac4.png?size=64")
					.setTimestamp(Instant.now())
					.setTitle("Thready Config for #" + channel.getName())
					.addField("Thready Enabled", Thready.sql.isEnabled(guild, channel) ? "✅ Yes" : "🚫 No", false)
					.addField("All Messages", 	  (threadingConfig & 1)    > 0 ? "✅ Yes" : "🚫 No", true)
					.addField("Links", 			  (threadingConfig & 2)    > 0 ? "✅ Yes" : "🚫 No", true)
					.addField("Images", 		  (threadingConfig & 4)    > 0 ? "✅ Yes" : "🚫 No", true)
					.addField("Videos", 		  (threadingConfig & 8)    > 0 ? "✅ Yes" : "🚫 No", true)
					.addField("Files", 			  (threadingConfig & 16)   > 0 ? "✅ Yes" : "🚫 No", true)
					.addField("Embeds", 		  (threadingConfig & 32)   > 0 ? "✅ Yes" : "🚫 No", true)
					.addField("Emotes", 		  (threadingConfig & 64)   > 0 ? "✅ Yes" : "🚫 No", true)
					.addField("Stickers", 		  (threadingConfig & 1024) > 0 ? "✅ Yes" : "🚫 No", true)
					.addField("User-Mentions", 	  (threadingConfig & 128)  > 0 ? "✅ Yes" : "🚫 No", true)
					.addField("Channel-Mentions", (threadingConfig & 256)  > 0 ? "✅ Yes" : "🚫 No", true)
					.addField("Role-Mentions", 	  (threadingConfig & 512)  > 0 ? "✅ Yes" : "🚫 No", true)
					.build();
		} catch (SQLException e) {
			Thready.logger.log("CRIT", "Couldn't retrieve config for channel " + channel.getId() + " in guild " + guild.getId() + "!");
			Thready.logger.logStackTrace(e);
			return new EmbedBuilder()
					.setColor(new Color(0xFF, 0x55, 0x55))
					.setDescription("Thready uses the following config for this channel. "
							+ "Use the selection-menu to select which option to change and then click either the Enable or Disable button. "
							+ "To completely enable or disable Thready in this channel, use the bottom button.\n\n"
							+ "**WARNING:** The config for this channel could not be loaded. This is most likely a temporary issue and will be fixed soon! "
							+ "Run the `/config`-command again and if the issue persists, please report it either on <https://discord.gg/ffrArfErfH> or "
							+ "on <https://github.com/Ryeera/Thready>!")
					.setFooter("Thready v" + Thready.VERSION + " by Ryeera", "https://cdn.discordapp.com/avatars/918245386441863218/8db6a3786a9d495f4fbe2cec55521ac4.png?size=64")
					.setTimestamp(Instant.now())
					.setTitle("Thready Config for #" + channel.getName())
					.addField("Thready Enabled",  "⚡ Unknown", false)
					.addField("All Messages", 	  "⚡ Unknown", true)
					.addField("Links", 			  "⚡ Unknown", true)
					.addField("Images", 		  "⚡ Unknown", true)
					.addField("Videos", 		  "⚡ Unknown", true)
					.addField("Files", 			  "⚡ Unknown", true)
					.addField("Embeds", 		  "⚡ Unknown", true)
					.addField("Emotes", 		  "⚡ Unknown", true)
					.addField("Stickers", 		  "⚡ Unknown", true)
					.addField("User-Mentions", 	  "⚡ Unknown", true)
					.addField("Channel-Mentions", "⚡ Unknown", true)
					.addField("Role-Mentions", 	  "⚡ Unknown", true)
					.build();
		}
	}
	
	public static SelectMenu getOptionMenu(MessageChannel channel) {
		return StringSelectMenu.create("config:" + channel.getId() + ":0")
	     .setPlaceholder("Select message-type...")
	     .addOption("Messages", 		"1", 	"En-/Disable Thready for all messages.")
	     .addOption("Links", 			"2", 	"En-/Disable Thready for messages containing links.")
	     .addOption("Images", 			"4", 	"En-/Disable Thready for messages containing images.")
	     .addOption("Videos", 			"8", 	"En-/Disable Thready for messages containing videos.")
	     .addOption("Files", 			"16", 	"En-/Disable Thready for messages containing files.")
	     .addOption("Embeds", 			"32", 	"En-/Disable Thready for messages containing embeds.")
	     .addOption("Emotes", 			"64", 	"En-/Disable Thready for messages containing emotes.")
	     .addOption("Stickers", 		"1024",	"En-/Disable Thready for messages containing stickers.")
	     .addOption("User-Mentions", 	"128", 	"En-/Disable Thready for messages containing user-mentions.")
	     .addOption("Channel-Mentions", "256", 	"En-/Disable Thready for messages containing channel-mentions.")
	     .addOption("Role-Mentions", 	"512", 	"En-/Disable Thready for messages containing role-mentions.")
	     .build();
	}
	
	public static MessageEmbed getChannelStatsEmbed(Guild guild, MessageChannel channel) {
		try {
			return new EmbedBuilder()
					.setColor(new Color(0x58, 0x65, 0xF2))
					.setDescription("These stats only count since Thready joined this server and only since Thready reached version 1.1.0!")
					.setFooter("Thready v" + Thready.VERSION + " by Ryeera", "https://cdn.discordapp.com/avatars/918245386441863218/8db6a3786a9d495f4fbe2cec55521ac4.png?size=64")
					.setTimestamp(Instant.now())
					.setTitle("Thready Stats for #" + channel.getName())
					.addField("Created Threads",       String.valueOf(Thready.sql.getThreadCount(guild, channel)), true)
					.addField("Sent Messages",         String.valueOf(Thready.sql.getMessageCount(guild, channel)), true)
					.addField("Sent Links",            String.valueOf(Thready.sql.getLinkCount(guild, channel)), true)
					.addField("Sent Images",           String.valueOf(Thready.sql.getImageCount(guild, channel)), true)
					.addField("Sent Videos",           String.valueOf(Thready.sql.getVideoCount(guild, channel)), true)
					.addField("Sent Files",            String.valueOf(Thready.sql.getFileCount(guild, channel)), true)
					.addField("Sent Embeds",           String.valueOf(Thready.sql.getEmbedCount(guild, channel)), true)
					.addField("Sent Emotes",           String.valueOf(Thready.sql.getEmoteCount(guild, channel)), true)
					.addField("Sent Stickers",         String.valueOf(Thready.sql.getStickerCount(guild, channel)), true)
					.addField("Sent User-Mentions",    String.valueOf(Thready.sql.getUserMentionCount(guild, channel)), true)
					.addField("Sent Channel-Mentions", String.valueOf(Thready.sql.getChannelMentionCount(guild, channel)), true)
					.addField("Sent Role-Mentions",    String.valueOf(Thready.sql.getRoleMentionCount(guild, channel)), true)
					.build();
		} catch (SQLException e) {
			Thready.logger.log("CRIT", "Couldn't retrieve stats for channel " + channel.getId() + " in guild " + guild.getId() + "!");
			Thready.logger.logStackTrace(e);
			return new EmbedBuilder()
					.setColor(new Color(0xFF, 0x55, 0x55))
					.setDescription("**WARNING:** The config for this channel could not be loaded. This is most likely a temporary issue and will be fixed soon! "
							+ "Run the `/stats`-command again and if the issue persists, please report it either on <https://discord.gg/ffrArfErfH> or "
							+ "on <https://github.com/Ryeera/Thready>!")
					.setFooter("Thready v" + Thready.VERSION + " by Ryeera", "https://cdn.discordapp.com/avatars/918245386441863218/8db6a3786a9d495f4fbe2cec55521ac4.png?size=64")
					.setTimestamp(Instant.now())
					.setTitle("Thready Stats for #" + channel.getName())
					.addField("Created Threads",       "⚡ Unknown", true)
					.addField("Sent Messages",         "⚡ Unknown", true)
					.addField("Sent Links",            "⚡ Unknown", true)
					.addField("Sent Images",           "⚡ Unknown", true)
					.addField("Sent Videos",           "⚡ Unknown", true)
					.addField("Sent Files",            "⚡ Unknown", true)
					.addField("Sent Embeds",           "⚡ Unknown", true)
					.addField("Sent Emotes",           "⚡ Unknown", true)
					.addField("Sent Stickers",         "⚡ Unknown", true)
					.addField("Sent User-Mentions",    "⚡ Unknown", true)
					.addField("Sent Channel-Mentions", "⚡ Unknown", true)
					.addField("Sent Role-Mentions",    "⚡ Unknown", true)
					.build();
		}
	}
}
