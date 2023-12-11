package com.zgamelogic.controllers;

import com.zgamelogic.data.database.user.User;
import com.zgamelogic.data.database.user.UserRepository;
import com.zgamelogic.data.services.auth.Permission;
import com.zgamelogic.data.services.discord.DiscordToken;
import com.zgamelogic.data.services.discord.DiscordUser;
import com.zgamelogic.data.services.discord.MSUUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

import static com.zgamelogic.data.Constants.*;
import static com.zgamelogic.services.DiscordService.*;

@Slf4j
@RestController
@PropertySource("File:msu.properties")
public class AuthenticationController {
    @Value("${client.id}") private String discordClientId;
    @Value("${client.secret}") private String discordClientSecret;
    @Value("${redirect.url}") private String discordRedirectUrl;

    @Value("${admin.code}") private String adminCode;

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
                User databaseUser = new User(user);
                if(userRepository.count() == 0) {
                    databaseUser.addPermission(MC_GENERAL_PERMISSION_CAT, MC_CREATE_SERVER_PERMISSION);
                    databaseUser.addPermission(MC_GENERAL_PERMISSION_CAT, MC_USER_MANAGEMENT_PERMISSION);
                }
                userRepository.save(databaseUser);
            }
            Map<String, String> permissions = userRepository.getReferenceById(user.getId()).getPermissions();
            return ResponseEntity.ok(new MSUUser(user, token, permissions));
        } catch (Exception e){
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("user/permissions/{id}")
    private ResponseEntity<Map<String, String>> userPermissions(
            @PathVariable(required = false) String id,
            @RequestHeader(name = "user", required=false) String userId
    ){
        if(!userRepository.userHasPermission(userId, MC_GENERAL_PERMISSION_CAT, MC_USER_MANAGEMENT_PERMISSION)) return ResponseEntity.status(401).build();
        Optional<User> optionalUser = userRepository.findById(id);
        return optionalUser.map(
                user -> ResponseEntity.ok(user.getPermissions()))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("user/permissions/{id}")
    private ResponseEntity updateUserPermissions(
            @RequestHeader(name = "user", required = false) String userId,
            @RequestHeader(name = "admin-code", required = false) String adminCode,
            @PathVariable String id,
            @RequestBody Permission permission
    ){
        if(userId == null && adminCode == null) return ResponseEntity.status(401).build();
        if(userId != null && !userRepository.userHasPermission(userId, MC_GENERAL_PERMISSION_CAT, MC_USER_MANAGEMENT_PERMISSION)) return ResponseEntity.status(401).build();
        if(adminCode != null && !adminCode.equals(this.adminCode)) return ResponseEntity.status(401).build();
        if(!userRepository.existsById(id)) return ResponseEntity.status(404).build();
        User user = userRepository.getReferenceById(id);
        user.addPermission(permission.getServer(), permission.getPermission());
        userRepository.save(user);
        return ResponseEntity.status(200).build();
    }
}
