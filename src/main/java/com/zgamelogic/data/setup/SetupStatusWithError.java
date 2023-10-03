package com.zgamelogic.data.setup;

import lombok.Getter;

@Getter
public class SetupStatusWithError extends SetupStatus {
    private final String error;
    public SetupStatusWithError(boolean websitePort, boolean initialUser, boolean initialPass, String error) {
        super(websitePort, initialUser, initialPass);
        this.error = error;
    }
}
