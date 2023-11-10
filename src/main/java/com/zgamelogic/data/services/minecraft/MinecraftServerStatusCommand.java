package com.zgamelogic.data.services.minecraft;

import lombok.NonNull;

public record MinecraftServerStatusCommand(@NonNull String server, @NonNull String command) {}
