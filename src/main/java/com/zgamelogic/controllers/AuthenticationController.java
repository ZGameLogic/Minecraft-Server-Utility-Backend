package com.zgamelogic.controllers;

import com.zgamelogic.data.database.user.NotificationConfiguration;
import com.zgamelogic.data.database.user.User;
import com.zgamelogic.data.database.user.UserRepository;
import com.zgamelogic.data.services.auth.NotificationMessage;
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

import java.util.List;
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
            @RequestParam(value = "id", required = false) String id
    ){
        if(code == null && id == null) return ResponseEntity.badRequest().build();
        try {
            DiscordToken token;
            if(code != null){
                token = postForToken(code, discordClientId, discordClientSecret, discordRedirectUrl);
            } else {
                Optional<User> user = userRepository.findById(id);
                if(user.isPresent()){
                    token = refreshToken(user.get().getRefreshToken(), discordClientId, discordClientSecret);
                } else {
                    return ResponseEntity.status(404).build();
                }
            }
            DiscordUser user = getUserFromToken(token.getAccess_token());
            if(userRepository.existsById(user.getId())){
                User databaseUser = userRepository.getReferenceById(user.getId());
                databaseUser.updateUser(user, token);
                userRepository.save(databaseUser);
            } else {
                User databaseUser = new User(user, token);
                if(userRepository.count() == 0) {
                    databaseUser.addPermission(MC_GENERAL_PERMISSION_CAT, MC_CREATE_SERVER_PERMISSION);
                    databaseUser.addPermission(MC_GENERAL_PERMISSION_CAT, MC_USER_MANAGEMENT_PERMISSION);
                    databaseUser.addPermission(MC_GENERAL_PERMISSION_CAT, MC_ADMIN_PERMISSION);
                }
                userRepository.save(databaseUser);
            }
            Map<String, String> permissions = userRepository.getReferenceById(user.getId()).getPermissions();
            Map<String, NotificationConfiguration> notifications = userRepository.getReferenceById(user.getId()).getNotifications();
            return ResponseEntity.ok(new MSUUser(user, token, permissions, notifications));
        } catch (Exception e){
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/permissions")
    private ResponseEntity<List<User>> userPermissions(
            @RequestHeader(name = "user", required=false) String userId
    ){
        if(!userRepository.userHasPermission(userId, MC_GENERAL_PERMISSION_CAT, MC_USER_MANAGEMENT_PERMISSION)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(userRepository.findAll());
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("/user/notifications/toggle")
    private ResponseEntity toggleUserNotification(
            @RequestHeader(name = "user") String userId,
            @RequestBody NotificationMessage notificationMessage
    ){
        if(userId == null) return ResponseEntity.status(401).build();
        if(!userRepository.existsById(userId)) return ResponseEntity.status(404).build();
        User user = userRepository.getReferenceById(userId);
        user.toggleNotification(notificationMessage.getServer(), notificationMessage.getNotification());
        userRepository.save(user);
        return ResponseEntity.status(200).build();
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("/user/permissions/add/{id}")
    private ResponseEntity addUserPermissions(
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
        user.addPermission(permission);
        userRepository.save(user);
        return ResponseEntity.status(200).build();
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("/user/permissions/remove/{id}")
    private ResponseEntity removeUserPermissions(
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
        user.removePermission(permission);
        userRepository.save(user);
        return ResponseEntity.status(200).build();
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("user/devices/register/{device}")
    private ResponseEntity registerDevice(
            @RequestHeader(name = "user") String userId,
            @PathVariable String device
    ){
        if(!userRepository.existsById(userId)) return ResponseEntity.status(404).build();
        User user = userRepository.getReferenceById(userId);
        user.registerDevice(device);
        userRepository.save(user);
        return ResponseEntity.status(200).build();
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("user/devices/unregister/{device}")
    private ResponseEntity unregisterDevice(
            @RequestHeader(name = "user") String userId,
            @PathVariable String device
    ){
        if(!userRepository.existsById(userId)) return ResponseEntity.status(404).build();
        User user = userRepository.getReferenceById(userId);
        user.unregisterDevice(device);
        userRepository.save(user);
        return ResponseEntity.status(200).build();
    }
}
