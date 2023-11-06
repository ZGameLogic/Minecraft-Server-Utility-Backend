package com.zgamelogic.data.services.setup;

import lombok.Getter;

@Getter
public class SetupStatusWithError extends SetupStatus {
    private final String error;
    public SetupStatusWithError(boolean setupFinished, String error) {
        super(setupFinished);
        this.error = error;
    }
}
