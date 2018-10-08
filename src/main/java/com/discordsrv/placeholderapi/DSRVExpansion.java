package com.discordsrv.placeholderapi;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.core.entities.*;
import java.util.function.Function;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class DSRVExpansion extends PlaceholderExpansion {
    private static final String discordSRV = "DiscordSRV";

    @Override
    public String getName() {
        return discordSRV;
    }

    @Override
    public String getRequiredPlugin() {
        return discordSRV;
    }

    @Override
    public String getIdentifier() {
        return discordSRV.toLowerCase();
    }

    @Override
    public String getAuthor() {
        return "Vankka";
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (!DiscordSRV.isReady)
            return null;

        Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();

        switch (identifier) {
            case "guild_id":
                return mainGuild.getId();
            case "guild_name":
                return mainGuild.getName();
            case "guild_icon_id":
                return orEmptyString(mainGuild.getIconId());
            case "guild_icon_url":
                return orEmptyString(mainGuild.getIconUrl());
            case "guild_splash_id":
                return orEmptyString(mainGuild.getSplashId());
            case "guild_splash_url":
                return orEmptyString(mainGuild.getSplashUrl());
            case "guild_owner_effective_name":
                return mainGuild.getOwner().getEffectiveName();
            case "guild_owner_nickname":
                return mainGuild.getOwner().getNickname();
            case "guild_owner_game_name":
                return getOrEmptyString(mainGuild.getOwner().getGame(), Game::getName);
            case "guild_owner_game_url":
                return getOrEmptyString(mainGuild.getOwner().getGame(), Game::getUrl);
            case "guild_bot_effective_name":
                return mainGuild.getSelfMember().getEffectiveName();
            case "guild_bot_nickname":
                return orEmptyString(mainGuild.getSelfMember().getNickname());
            case "guild_bot_game_name":
                return getOrEmptyString(mainGuild.getSelfMember().getGame(), Game::getName);
            case "guild_bot_game_url":
                return getOrEmptyString(mainGuild.getSelfMember().getGame(), Game::getUrl);
        }

        if (player == null)
            return null;

        String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());

        if (identifier.equals("user_id"))
            return userId;

        if (userId == null)
            return null;

        User user = DiscordSRV.getPlugin().getJda().getUserById(userId);

        if (identifier.equals("user_name"))
            return user.getName();

        Member member = mainGuild.getMember(user);

        if (member == null)
            return null;

        switch (identifier) {
            case "user_effective_name":
                return member.getEffectiveName();
            case "user_nickname":
                return orEmptyString(member.getNickname());
            case "user_online_status":
                return member.getOnlineStatus().getKey();
            case "user_game_name":
                return getOrEmptyString(member.getGame(), Game::getName);
            case "user_game_url":
                return getOrEmptyString(member.getGame(), Game::getUrl);
        }

        if (member.getRoles().size() < 1)
            return null;

        Role topRole = member.getRoles().get(0);

        switch (identifier) {
            case "user_top_role_id":
                return topRole.getId();
            case "user_top_role_name":
                return topRole.getName();
            case "user_top_role_color":
                return topRole.getColor().toString();
        }

        return null;
    }

    private <T> String getOrEmptyString(T input, Function<T, String> function) {
        if (input == null)
            return "";
        String output = function.apply(input);
        return output != null ? output : "";
    }

    private String orEmptyString(String input) {
        if (input == null)
            return "";
        return input;
    }
}
