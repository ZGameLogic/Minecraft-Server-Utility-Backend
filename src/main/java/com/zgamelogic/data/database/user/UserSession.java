package com.zgamelogic.data.database.user;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class UserSession {
    private String sessionToken;
    private Date expires;
}
