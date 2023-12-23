package com.zgamelogic.data.database.user;

import jakarta.persistence.Embeddable;
import lombok.Data;
import com.zgamelogic.data.services.auth.NotificationMessage.Toggle;

@Embeddable
@Data
public class NotificationConfiguration {
    private boolean player; // join leave
    private boolean chat; // chat notifications
    private boolean live; // live activity
    private boolean status; // server is turned off, crashed, on

    public void toggle(Toggle notification){
        switch (notification){
            case PLAYER -> player = !player;
            case CHAT -> chat = !chat;
            case LIVE -> live = !live;
            case STATUS -> status = !status;
        }
    }
}
