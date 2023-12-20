package com.zgamelogic.data.services.minecraft;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.zgamelogic.data.Constants.MC_NAME_REGEX;
import static com.zgamelogic.data.Constants.MC_SERVER_CREATE_PORT_RANGE;

@Data
@NoArgsConstructor
public class MinecraftServerCreationData {
    @NotNull(message = "autoStart is required")
    private Boolean autoStart;
    @NotNull(message = "autoUpdate is required")
    private Boolean autoUpdate;
    @NotNull(message = "version is required")
    private String version;
    @NotNull(message = "category is required")
    private String category;
    @NotNull(message = "start command is required")
    @NotEmpty(message = "start command cannot be empty")
    private String startCommand;

    @NotNull(message = "port is required")
    @Min(value = 25500, message = MC_SERVER_CREATE_PORT_RANGE)
    @Max(value = 29999, message = MC_SERVER_CREATE_PORT_RANGE)
    private Integer port;
    @NotNull(message = "name is required")
    @NotEmpty(message = "name cannot be empty")
    @Pattern(regexp = MC_NAME_REGEX, message = "name can only contain alphanumeric values")
    private String name;
}
