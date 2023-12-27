package com.zgamelogic.data.services.discord;

import lombok.Data;

@Data
public class MessagePacket {
    private final Long guildId;
    private final Long channelId;
    private final String message;
    private final String[] mentionIds;
}
