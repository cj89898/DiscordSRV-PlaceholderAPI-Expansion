package com.discordsrv.placeholderapi;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.google.common.collect.ImmutableMap;
import github.scarsz.discordsrv.dependencies.jda.api.OnlineStatus;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import github.scarsz.discordsrv.util.DiscordUtil;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
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

        Supplier<List<String>> membersOnline = () -> mainGuild.getMembers().stream()
                .filter(member -> member.getOnlineStatus() != OnlineStatus.OFFLINE)
                .map(member -> member.getUser().getId())
                .collect(Collectors.toList());
        Supplier<Set<String>> linkedAccounts = () -> DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().keySet();

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
                return getOrEmptyString(mainGuild.getOwner(), Member::getEffectiveName);
            case "guild_owner_nickname":
                return getOrEmptyString(mainGuild.getOwner(), Member::getNickname);
            case "guild_owner_game_name":
                return getOrEmptyString(mainGuild.getOwner(), member -> member.getActivities().stream().findFirst().map(Activity::getName).orElse(""));
            case "guild_owner_game_url":
                return getOrEmptyString(mainGuild.getOwner(), member -> member.getActivities().stream().findFirst().map(Activity::getUrl).orElse(""));
            case "guild_bot_effective_name":
                return mainGuild.getSelfMember().getEffectiveName();
            case "guild_bot_nickname":
                return orEmptyString(mainGuild.getSelfMember().getNickname());
            case "guild_bot_game_name":
                return getOrEmptyString(mainGuild.getSelfMember(), member -> member.getActivities().stream().findFirst().map(Activity::getName).orElse(""));
            case "guild_bot_game_url":
                return getOrEmptyString(mainGuild.getSelfMember(), member -> member.getActivities().stream().findFirst().map(Activity::getUrl).orElse(""));
            case "guild_members_online":
                return String.valueOf(membersOnline.get().size());
            case "guild_members_total":
                return String.valueOf(mainGuild.getMembers().size());
            case "linked_online":
                return String.valueOf(linkedAccounts.get().stream().filter(membersOnline.get()::contains).count());
            case "linked_total":
                return String.valueOf(linkedAccounts.get().size());
        }

        if (player == null)
            return "";

        String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        if (identifier.equals("user_id")) {
            return orEmptyString(userId);
        }

        if (userId == null) {
            return "";
        }

        User user = DiscordSRV.getPlugin().getJda().getUserById(userId);
        if (user == null) {
            return "";
        }

        switch (identifier) {
            case "user_name":
                return user.getName();
            case "user_islinked":
                return getBoolean(userId != null);
            case "user_tag":
                return user.getAsTag();
        }

        Member member = mainGuild.getMember(user);
        if (member == null) {
            return "";
        }

        switch (identifier) {
            case "user_effective_name":
                return member.getEffectiveName();
            case "user_nickname":
                return orEmptyString(member.getNickname());
            case "user_online_status":
                return member.getOnlineStatus().getKey();
            case "user_game_name":
                return member.getActivities().stream().findFirst().map(Activity::getName).orElse("");
            case "user_game_url":
                return member.getActivities().stream().findFirst().map(Activity::getUrl).orElse("");
        }

        if (member.getRoles().isEmpty()) {
            return "";
        }

        List<Role> selectedRoles = getRoles(member);
        if (selectedRoles.isEmpty()) {
            return "";
        }

        Role topRole = selectedRoles.get(0);

        switch (identifier) {
            case "user_top_role_id":
                return topRole.getId();
            case "user_top_role_name":
                return topRole.getName();
            case "user_top_role_color_hex":
                return getOrEmptyString(topRole.getColor(), this::getHex);
            case "user_top_role_color_code":
                return getRoleColor(topRole);
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

    /**
     * Default Discord {@link Color}.
     * Clone of https://github.com/DiscordSRV/DiscordSRV/blob/5111959c0ccdd6dc454011de0c4936be764f388f/src/main/java/github/scarsz/discordsrv/util/DiscordUtil.java#L509
     */
    private static final Color discordDefaultColor = new Color(153, 170, 181, 1);

    /**
     * Map of {@link java.awt.Color}s and {@link ChatColor}s.
     * Clone of https://github.com/DiscordSRV/DiscordSRV/blob/5111959c0ccdd6dc454011de0c4936be764f388f/src/main/java/github/scarsz/discordsrv/util/DiscordUtil.java#L510-L527
     */
    private static final Map<Color, ChatColor> minecraftColors = ImmutableMap.copyOf(new HashMap<Color, ChatColor>() {{
        put(new Color(0, 0, 0), ChatColor.BLACK);
        put(new Color(0, 0, 170), ChatColor.DARK_BLUE);
        put(new Color(0, 170, 0), ChatColor.DARK_GREEN);
        put(new Color(0, 170, 170), ChatColor.DARK_AQUA);
        put(new Color(170, 0, 0), ChatColor.DARK_RED);
        put(new Color(170, 0, 170), ChatColor.DARK_PURPLE);
        put(new Color(255, 170, 0), ChatColor.GOLD);
        put(new Color(170, 170, 170), ChatColor.GRAY);
        put(new Color(85, 85, 85), ChatColor.DARK_GRAY);
        put(new Color(85, 85, 255), ChatColor.BLUE);
        put(new Color(85, 255, 85), ChatColor.GREEN);
        put(new Color(85, 255, 255), ChatColor.AQUA);
        put(new Color(255, 85, 85), ChatColor.RED);
        put(new Color(255, 85, 255), ChatColor.LIGHT_PURPLE);
        put(new Color(255, 255, 85), ChatColor.YELLOW);
        put(new Color(255, 255, 255), ChatColor.WHITE);
    }});

    /**
     * Helper method.
     * Clone of https://github.com/DiscordSRV/DiscordSRV/blob/5111959c0ccdd6dc454011de0c4936be764f388f/src/main/java/github/scarsz/discordsrv/util/DiscordUtil.java#L529-L533
     */
    private static int colorDistance(Color color1, Color color2) {
        return (int) Math.sqrt((color1.getRed() - color2.getRed()) * (color1.getRed() - color2.getRed())
                + (color1.getGreen() - color2.getGreen()) * (color1.getGreen() - color2.getGreen())
                + (color1.getBlue() - color2.getBlue()) * (color1.getBlue() - color2.getBlue()));
    }

    /**
     * Gets the role color (code) string for a role.
     *
     * @param role the role.
     * @return the color (code) string
     */
    private String getRoleColor(Role role) {
        Color color = role.getColor() != null ? role.getColor() : discordDefaultColor;
        String hex = Integer.toHexString(color.getRGB()).toUpperCase();
        if (hex.length() == 8) hex = hex.substring(2);
        String translatedColor = DiscordSRV.getPlugin().getColors().get(hex);

        if (translatedColor == null) {
            if (DiscordSRV.config().getBooleanElse("Experiment_Automatic_Color_Translations", false)) {
                ChatColor determinedColor = minecraftColors.entrySet().stream()
                        .min(Comparator.comparingInt(entry -> colorDistance(color, entry.getKey())))
                        .map(Map.Entry::getValue)
                        .orElseThrow(() -> new RuntimeException("This should not be possible:tm:"));

                translatedColor = determinedColor.toString();
            } else {
                translatedColor = "";
            }
        }

        return translatedColor;
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
