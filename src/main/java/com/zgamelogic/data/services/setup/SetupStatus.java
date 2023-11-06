package com.zgamelogic.data.services.setup;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SetupStatus {
    @JsonProperty("setup finished")
    private final boolean setupFinished;
}
