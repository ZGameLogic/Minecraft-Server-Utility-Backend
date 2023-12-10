package com.zgamelogic.data.database.user;

import com.zgamelogic.data.services.discord.DiscordUser;
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

    @ElementCollection
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "object_name")
    @Column(name = "permissions")
    private Map<String, String> permissions = new HashMap<>();

    public User(DiscordUser discordUser){
        id = discordUser.getId();
        username = discordUser.getUsername();
        email = discordUser.getEmail();
        lastLoggedIn = new Date();
        permissions = new HashMap<>();
    }

    public void updateUser(DiscordUser discordUser){
        username = discordUser.getUsername();
        email = discordUser.getEmail();
    }

    public void addPermission(String obj, String perm){
        if(permissions.containsKey(obj)){
            permissions.put(obj, permissions.get(obj) + perm);
        } else {
            permissions.put(obj, perm);
        }
    }
}
