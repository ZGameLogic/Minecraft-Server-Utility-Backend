package com.zgamelogic.data.database.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "msu_users")
public class User {

    @Id
    private String username;
    private String password;
    private Date lastLoggedIn;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> permissions;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void addPermission(String...permission){
        if(permissions == null) permissions = new LinkedList<>();
        permissions.addAll(List.of(permission));
    }
}
