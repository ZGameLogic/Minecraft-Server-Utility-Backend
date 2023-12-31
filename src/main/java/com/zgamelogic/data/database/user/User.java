package com.zgamelogic.data.database.user;

import com.zgamelogic.data.services.auth.Permission;
import com.zgamelogic.data.services.discord.DiscordToken;
import com.zgamelogic.data.services.discord.DiscordUser;
import com.zgamelogic.data.services.auth.NotificationMessage.Toggle;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.*;

@Data
@Entity
@ToString
@NoArgsConstructor
@Table(name = "msu_users")
public class User {

    @Id
    private String id;
    private String username;
    private String email;
    private Date lastLoggedIn;
    private String refreshToken;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "object_name")
    @Column(name = "permissions")
    private Map<String, String> permissions = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_notifications", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "server_name")
    @Column(name = "notifications")
    private Map<String, NotificationConfiguration> notifications = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_device_ids", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "device")
    private Set<String> deviceIds;

    public User(DiscordUser discordUser, DiscordToken token){
        id = discordUser.getId();
        username = discordUser.getUsername();
        email = discordUser.getEmail();
        lastLoggedIn = new Date();
        permissions = new HashMap<>();
        refreshToken = token.getRefresh_token();
    }

    public void updateUser(DiscordUser discordUser, DiscordToken token){
        username = discordUser.getUsername();
        email = discordUser.getEmail();
        refreshToken = token.getRefresh_token();
    }

    public void addPermission(String obj, String perm){
        if(permissions.containsKey(obj)){
            permissions.put(obj, permissions.get(obj) + perm);
        } else {
            permissions.put(obj, perm);
        }
    }

    public void addPermission(Permission permission){
        addPermission(permission.getServer(), permission.getPermission());
    }

    public void removePermission(String obj, String perm){
        if(!permissions.containsKey(obj)) return;
        String newPerms = permissions.get(obj).replace(perm, "");
        permissions.put(obj, newPerms);
    }

    public void removePermission(Permission permission){
        removePermission(permission.getServer(), permission.getPermission());
    }

    public void registerDevice(String id){
        deviceIds.add(id);
    }

    public void unregisterDevice(String id){
        deviceIds.remove(id);
    }

    public void toggleNotification(String server, Toggle notification){
        if(notifications.containsKey(server)){
            NotificationConfiguration n = notifications.get(server);
            n.toggle(notification);
            notifications.put(server, n);
        } else {
            NotificationConfiguration n = new NotificationConfiguration();
            n.toggle(notification);
            notifications.put(server, n);
        }
    }

    public boolean hasNotificationEnabled(String server, Toggle notification){
        if(!notifications.containsKey(server)) return false;
        return notifications.get(server).enabled(notification);
    }

    public void createNotificationPermission(String server){
        if(!notifications.containsKey(server)) notifications.put(server, new NotificationConfiguration());
        if(!permissions.containsKey(server)) permissions.put(server, "");
    }
}