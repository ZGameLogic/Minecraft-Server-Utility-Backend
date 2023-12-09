package com.zgamelogic.data.services.discord;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MSUUser {
    private String username;
    private String avatar;
    private String id;
    private String refresh_token;

    public MSUUser(DiscordUser discordUser, DiscordToken discordToken){
        username = discordUser.getUsername();
        avatar = discordUser.getAvatar();
        id = discordUser.getId();
        refresh_token = discordToken.getRefresh_token();
    }
}
