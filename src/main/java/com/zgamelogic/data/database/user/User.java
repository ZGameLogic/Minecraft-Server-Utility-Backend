package com.zgamelogic.data.database.user;

import com.zgamelogic.data.services.discord.DiscordToken;
import com.zgamelogic.data.services.discord.DiscordUser;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
@Entity
@NoArgsConstructor
@Table(name = "msu_users")
public class User {

    @Id
    private String id;
    private String username;
    private String email;
    private String token;
    private String refreshToken;
    private Date lastLoggedIn;

    @ElementCollection
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "object_name")
    @Column(name = "permissions")
    private Map<String, String> permissions = new HashMap<>();

    public User(DiscordUser discordUser, String token, String refreshToken){
        id = discordUser.getId();
        username = discordUser.getUsername();
        email = discordUser.getEmail();
        this.token = token;
        this.refreshToken = refreshToken;
        lastLoggedIn = new Date();
        permissions = new HashMap<>();
    }

    public void addPermission(String obj, String perm){
        if(permissions.containsKey(obj)){
            permissions.put(obj, permissions.get(obj) + perm);
        } else {
            permissions.put(obj, perm);
        }
    }

    public void updateToken(DiscordToken discordToken){
        token = discordToken.getAccess_token();
        refreshToken = discordToken.getRefresh_token();
    }

    public void updateUserInfo(DiscordUser discordUser){
        username = discordUser.getUsername();
        email = discordUser.getEmail();
        lastLoggedIn = new Date();
    }
}
