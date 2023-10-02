package com.zgamelogic.data.database.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedList;
import java.util.List;

@Getter
@ToString
@Entity
@Table(name = "msu_users")
public class User {

    @Id
    private String username;
    private String password;

    @Embedded
    private UserSession session;

    @ElementCollection
    @CollectionTable(name="user_permissions")
    private LinkedList<String> permissions;

    public void addPermission(String...permission){
        if(permissions == null) permissions = new LinkedList<>();
        permissions.addAll(List.of(permission));
    }
}
