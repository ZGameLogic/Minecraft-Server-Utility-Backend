package com.zgamelogic.data.database.user;

import com.zgamelogic.data.services.auth.Permission;
import com.zgamelogic.data.services.discord.DiscordToken;
import com.zgamelogic.data.services.discord.DiscordUser;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.*;

import static com.zgamelogic.data.Constants.MC_ADMIN_PERMISSION;
import static com.zgamelogic.data.Constants.MC_GENERAL_PERMISSION_CAT;

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

    @ElementCollection
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "object_name")
    @Column(name = "permissions")
    private Map<String, String> permissions = new HashMap<>();

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

    public boolean hasPermission(String obj, String permission){
        if(permissions.containsKey(MC_GENERAL_PERMISSION_CAT) && permissions.get(MC_GENERAL_PERMISSION_CAT).contains(MC_ADMIN_PERMISSION)) return true;
        if(!permissions.containsKey(obj)) return false;
        return permissions.get(obj).contains(permission);
    }
}
