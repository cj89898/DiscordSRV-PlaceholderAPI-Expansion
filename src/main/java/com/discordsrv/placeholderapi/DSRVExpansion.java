package com.discordsrv.placeholderapi;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.core.OnlineStatus;
import github.scarsz.discordsrv.dependencies.jda.core.entities.*;

import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import github.scarsz.discordsrv.util.DiscordUtil;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class DSRVExpansion extends PlaceholderExpansion {
    private static final String discordSRV = "DiscordSRV";

    @Override
    public boolean persist() {
        return true;
    }

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
        return "@VERSION@";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (!DiscordSRV.isReady)
            return null;

        Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();

        if (mainGuild == null)
            return "";

        List<String> membersOnline = mainGuild.getMembers().stream()
                .filter(member -> member.getOnlineStatus() != OnlineStatus.OFFLINE)
                .map(member -> member.getUser().getId())
                .collect(Collectors.toList());
        Set<String> linkedAccounts = DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().keySet();

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
            case "guild_members_online":
                return String.valueOf(membersOnline.size());
            case "guild_members_total":
                return String.valueOf(mainGuild.getMembers().size());
            case "linked_online":
                return String.valueOf(linkedAccounts.stream().filter(membersOnline::contains).count());
            case "linked_total":
                return String.valueOf(linkedAccounts.size());
        }

        if (player == null)
            return "";

        String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());

        if (identifier.equals("user_id"))
            return orEmptyString(userId);

        if (userId == null)
            return "";

        User user = DiscordSRV.getPlugin().getJda().getUserById(userId);

        switch (identifier) {
            case "user_name":
                return user.getName();
            case "user_islinked":
                return getBoolean(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId()) != null);
        }

        Member member = mainGuild.getMember(user);

        if (member == null)
            return "";

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
            case "user_formatted":
                return member.getUser().getName() + "#" + member.getUser().getDiscriminator();
        }

        if (member.getRoles().isEmpty())
            return "";

        List<Role> selectedRoles = getRoles(member);
        if (selectedRoles.isEmpty())
            return "";

        Role topRole = selectedRoles.get(0);

        switch (identifier) {
            case "user_top_role_id":
                return topRole.getId();
            case "user_top_role_name":
                return topRole.getName();
            case "user_top_role_color":
                return getHex(topRole.getColor());
        }

        return null;
    }

    /**
     * Get roles from a member, filtered based on
     * Source: https://github.com/DiscordSRV/DiscordSRV/blob/6b8de4afb3bfecf9c63275d381c75b103e5543f3/src/main/java/github/scarsz/discordsrv/listeners/DiscordChatListener.java#L110-L122
     *
     * @param member The member to get the roles from
     * @return filtered list of roles
     */
    private List<Role> getRoles(Member member) {
        List<Role> selectedRoles;
        List<String> discordRolesSelection = DiscordSRV.config().getStringList("DiscordChatChannelRolesSelection");
        // if we have a whitelist in the config
        if (DiscordSRV.config().getBoolean("DiscordChatChannelRolesSelectionAsWhitelist")) {
            selectedRoles = member.getRoles().stream()
                    .filter(role -> discordRolesSelection.contains(DiscordUtil.getRoleName(role)))
                    .collect(Collectors.toList());
        } else { // if we have a blacklist in the settings
            selectedRoles = member.getRoles().stream()
                    .filter(role -> !discordRolesSelection.contains(DiscordUtil.getRoleName(role)))
                    .collect(Collectors.toList());
        }
        selectedRoles.removeIf(role -> role.getName().length() < 1);

        return selectedRoles;
    }

    private String getHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
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

    private String getBoolean(boolean input) {
        return input ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
    }
}
