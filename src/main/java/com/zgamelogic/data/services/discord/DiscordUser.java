package com.zgamelogic.data.services.discord;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
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
