package com.zgamelogic.data;

public abstract class Constants {
    public final static String MC_SERVER_OFFLINE = "Offline";
    public final static String MC_SERVER_ONLINE = "Online";
    public final static String MC_SERVER_STARTING = "Starting";
    public final static String MC_SERVER_STOPPING = "Stopping";
    public final static String MC_SERVER_RESTARTING = "Restarting";
    public final static String MC_SERVER_CRASHED = "Crashed";
    public final static String MC_SERVER_UPDATING = "Updating";

    public final static String MC_NAME_REGEX = "^[a-zA-Z0-9]*$";

    public final static String MC_SERVER_CREATE_SUCCESS = "Server created successfully.";
    public final static String MC_SERVER_CREATE_PORT_CONFLICT = "Port already in use";
    public final static String MC_SERVER_CREATE_NAME_CONFLICT = "Name already in use";
    public final static String MC_SERVER_CREATE_VERSION_DOESNT_EXIST = "Version does not exist";
    public final static String MC_SERVER_CREATE_CONFLICT = "Unable to create server.";
    public final static String MC_SERVER_CREATE_PORT_RANGE = "port must be between 25500 and 29999";

    public final static String MC_GENERAL_PERMISSION_CAT = "General Permissions";

    public final static String MC_CREATE_SERVER_PERMISSION = "C";
    public final static String MC_USER_MANAGEMENT_PERMISSION = "U";
    public final static String MC_ADMIN_PERMISSION = "A";

    public final static String MC_USE_CONSOLE_SERVER_PERMISSION = "c";
    public final static String MC_EDIT_SERVER_PROPERTIES_PERMISSION = "e";
    public final static String MC_ISSUE_COMMANDS_SERVER_PERMISSION = "s"; // s for start stop idk
}
