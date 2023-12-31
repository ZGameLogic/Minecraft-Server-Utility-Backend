package com.zgamelogic.data.database.user;

import jakarta.persistence.Embeddable;
import lombok.Data;
import com.zgamelogic.data.services.auth.NotificationMessage.Toggle;

@Embeddable
@Data
public class NotificationConfiguration {
    private boolean player; // join leave
    private boolean live; // live activity
    private boolean status; // server is turned off, crashed, on

    public void toggle(Toggle notification){
        switch (notification){
            case PLAYER -> player = !player;
            case LIVE -> live = !live;
            case STATUS -> status = !status;
        }
    }

    public boolean enabled(Toggle notification){
        switch (notification){
            case PLAYER -> {
                return player;
            }
            case LIVE -> {
                return live;
            }
            case STATUS -> {
                return status;
            }
        }
        return false;
    }
}