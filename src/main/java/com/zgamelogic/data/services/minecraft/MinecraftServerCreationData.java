package com.zgamelogic.data.services.minecraft;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @NotNull(message = "startCommand is required")
    private String startCommand;

    @NotNull(message = "port is required")
    @Min(value = 25500, message = MC_SERVER_CREATE_PORT_RANGE)
    @Max(value = 29999, message = MC_SERVER_CREATE_PORT_RANGE)
    private Integer port;
    @NotNull(message = "name is required")
    @Pattern(regexp = MC_NAME_REGEX, message = "name can only contain alphanumeric values")
    private String name;
}
