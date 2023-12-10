package com.zgamelogic.data.services.discord;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class DiscordToken {
    private String token_type;
    private String access_token;
    private Long expires_in;
    private String refresh_token;
    private String scope;
}
