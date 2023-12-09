package com.zgamelogic.controllers;

import com.zgamelogic.data.database.user.User;
import com.zgamelogic.data.database.user.UserRepository;
import com.zgamelogic.data.services.discord.DiscordToken;
import com.zgamelogic.data.services.discord.DiscordUser;
import com.zgamelogic.data.services.discord.MSUUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.zgamelogic.services.DiscordService.*;

@Slf4j
@RestController
@PropertySource("File:msu.properties")
public class AuthenticationController {
    @Value("${client.id}") private String discordClientId;
    @Value("${client.secret}") private String discordClientSecret;
    @Value("${redirect.url}") private String discordRedirectUrl;

    private final UserRepository userRepository;

    @Autowired
    public AuthenticationController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/auth/login")
    private ResponseEntity<MSUUser> login(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "token", required = false) String refreshToken
    ){
        if(code == null && refreshToken == null) return ResponseEntity.badRequest().build();
        try {
            DiscordToken token = code != null ?
                    postForToken(code, discordClientId, discordClientSecret, discordRedirectUrl) :
                    refreshToken(refreshToken, discordClientId, discordClientSecret);
            DiscordUser user = getUserFromToken(token.getAccess_token());
            if(userRepository.existsById(user.getId())){
                User databaseUser = userRepository.getReferenceById(user.getId());
                databaseUser.updateUser(user);
                userRepository.save(databaseUser);
            } else {
                userRepository.save(new User(user));
            }
            return ResponseEntity.ok(new MSUUser(user, token));
        } catch (Exception e){
            return ResponseEntity.badRequest().build();
        }
    }
}
