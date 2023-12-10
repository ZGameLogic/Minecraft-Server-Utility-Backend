package com.zgamelogic.data.services.discord;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DiscordUser {
    private String locale;
    private boolean verified;
    private String username;
    private String global_name;
    private String email;
    private String avatar;
    private String id;
}
