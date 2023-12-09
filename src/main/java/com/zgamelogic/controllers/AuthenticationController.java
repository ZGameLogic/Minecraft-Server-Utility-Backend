package com.zgamelogic.controllers;

import com.zgamelogic.data.database.user.User;
import com.zgamelogic.data.database.user.UserRepository;
import com.zgamelogic.data.services.discord.DiscordToken;
import com.zgamelogic.data.services.discord.DiscordUser;
import com.zgamelogic.services.DiscordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Optional;

import static com.zgamelogic.services.DiscordService.refreshToken;

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
    private DiscordToken login(@RequestParam("code") String code){
        return DiscordService.postForToken(code, discordClientId, discordClientSecret, discordRedirectUrl);
    }

    @PostMapping("/auth/verify/{token}/{refresh}")
    private ResponseEntity<DiscordToken> reauthenticate(@PathVariable String token, @PathVariable String refresh){
        Optional<User> databaseUser = userRepository.findUserBySessionAndRefreshToken(token, refresh);
        if(databaseUser.isEmpty()) return ResponseEntity.notFound().build();
        DiscordToken newToken = refreshToken(refresh, discordClientId, discordClientSecret);
        User user = databaseUser.get();
        user.updateToken(newToken);
        userRepository.save(user);
        return ResponseEntity.ok().body(newToken);
    }

    @GetMapping("/auth/user/{token}/{refresh}")
    private ResponseEntity<DiscordUser> getUser(@PathVariable String token, @PathVariable String refresh){
        try {
            DiscordUser user = DiscordService.getUserFromToken(token);
            userRepository.findById(user.getId()).ifPresentOrElse(u -> {
                u.updateUserInfo(user);
                userRepository.save(u);
            }, () -> userRepository.save(new User(user, token, refresh)));
            return ResponseEntity.ok(user);
        } catch(HttpClientErrorException.Unauthorized e){
            return ResponseEntity.badRequest().build();
        }
    }
}
