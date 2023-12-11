package com.zgamelogic.data.services.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Permission {
    private String server;
    private String permission;
}
