package com.zgamelogic.data.services.discord;

import com.zgamelogic.data.database.user.NotificationConfiguration;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class MSUUser {
    private String username;
    private String avatar;
    private String id;
    private String refresh_token;
    private Map<String, String> permissions;
    private Map<String, NotificationConfiguration> notifications;

    public MSUUser(
            DiscordUser discordUser,
            DiscordToken discordToken,
            Map<String, String> permissions,
            Map<String, NotificationConfiguration> notifications
    ){
        username = discordUser.getUsername();
        avatar = discordUser.getAvatar();
        id = discordUser.getId();
        refresh_token = discordToken.getRefresh_token();
        this.permissions = permissions;
        this.notifications = notifications;
    }
}