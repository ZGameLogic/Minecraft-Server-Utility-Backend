package com.zgamelogic.data.setup;

import lombok.Data;

@Data
public class SetupStatus {
    private final boolean websitePort;
    private final boolean initialUserUsername;
    private final boolean initialUserPassword;
}
