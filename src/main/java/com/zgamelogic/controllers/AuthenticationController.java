package com.zgamelogic.controllers;

import com.zgamelogic.data.services.discord.DiscordToken;
import com.zgamelogic.data.services.discord.DiscordUser;
import com.zgamelogic.services.DiscordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@PropertySource("File:msu.properties")
public class AuthenticationController {

    @Value("${client.id}") private String discordClientId;
    @Value("${client.secret}") private String discordClientSecret;
    @Value("${redirect.url}") private String discordRedirectUrl;

    @PostMapping("/auth/login")
    private DiscordToken login(@RequestParam("code") String code){
        return DiscordService.postForToken(code, discordClientId, discordClientSecret, discordRedirectUrl);
    }

    @GetMapping("/auth/user/{token}")
    private DiscordUser getUser(@PathVariable String token){
        log.info(token);
        return DiscordService.getUserFromToken(token);
    }
}
