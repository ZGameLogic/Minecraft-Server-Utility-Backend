package com.zgamelogic.data;

public abstract class Constants {
    public final static String MC_SERVER_OFFLINE = "Offline";
    public final static String MC_SERVER_ONLINE = "Online";
    public final static String MC_SERVER_STARTING = "Starting";
    public final static String MC_SERVER_STOPPING = "Stopping";
    public final static String MC_SERVER_RESTARTING = "Restarting";
    public final static String MC_SERVER_CRASHED = "Crashed";
    public final static String MC_SERVER_UPDATING = "Updating";

    public final static String MC_JOIN_GAME_REGEX = "\\[.*] \\[Server thread/INFO]: ([^\\[<].*?) joined the game$";
    public final static String MC_LEFT_GAME_REGEX = "\\[.*] \\[Server thread/INFO]: ([^\\[<].*?) left the game$";

    public final static String MC_SERVER_CREATE_SUCCESS = "Server created successfully.";
    public final static String MC_SERVER_CREATE_PORT_CONFLICT = "Unable to create server due to port conflict.";
    public final static String MC_SERVER_CREATE_NAME_CONFLICT = "Unable to create server due to name conflict.";
    public final static String MC_SERVER_CREATE_CONFLICT = "Unable to create server. Server name or port already in use. Archive existing servers to open up the port or name.";
}
