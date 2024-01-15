package com.zgamelogic.data.services.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.zgamelogic.data.minecraft.*;
import com.zgamelogic.services.DiscordService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import static com.zgamelogic.data.Constants.*;
import static com.zgamelogic.services.BackendService.*;
import static com.zgamelogic.services.BackendService.findDir;
import static com.zgamelogic.services.MinecraftService.downloadServer;

@SuppressWarnings("unused")
@NoArgsConstructor
@Slf4j
@Data
public class MinecraftServer {
    private String filePath;
    private String status;
    private String name;
    private HashMap<String, String> serverProperties;
    private MinecraftServerConfig serverConfig;
    private LinkedList<String> online;

    @JsonIgnore
    private Process serverProcess;
    @JsonIgnore
    private BufferedReader processOutput;
    @JsonIgnore
    private BufferedReader processErrorOutput;
    @JsonIgnore
    private PrintStream processInput;
    @JsonIgnore
    private MinecraftServerSocketActions socketActions;
    @JsonIgnore
    private MinecraftServerPlayerNotificationAction playerNotification;
    @JsonIgnore
    private MinecraftServerStatusNotificationAction statusNotificationAction;

    public MinecraftServer(
            File serverDir,
            MinecraftServerSocketAction messageAction,
            MinecraftServerSocketAction statusAction,
            MinecraftServerSocketAction playerAction,
            MinecraftServerSocketAction updateAction,
            MinecraftServerPlayerNotificationAction playerNotification,
            MinecraftServerStatusNotificationAction statusNotificationAction
    ){
        this(serverDir, new MinecraftServerSocketActions(
                messageAction,
                statusAction,
                playerAction,
                updateAction
        ), playerNotification, statusNotificationAction);
    }

    public MinecraftServer(File serverDir, MinecraftServerSocketActions socketActions, MinecraftServerPlayerNotificationAction playerNotification, MinecraftServerStatusNotificationAction statusNotificationAction){
        filePath = serverDir.getPath();
        this.socketActions = socketActions;
        this.playerNotification = playerNotification;
        this.statusNotificationAction = statusNotificationAction;
        status = MC_SERVER_OFFLINE;
        online = new LinkedList<>();
        serverProperties = new HashMap<>();
        loadServerProperties();
        loadServerConfig();
        if(serverConfig == null){
            serverConfig = new MinecraftServerConfig();
            saveServerConfig();
            loadServerConfig();
        }
    }

    public MinecraftServerLog loadLog(){
        File logFile = new File(getFilePath() + "/logs/latest.log");
        StringBuilder log = new StringBuilder();
        try {
            Scanner input = new Scanner(logFile);
            input.useDelimiter("\n");
            input.forEachRemaining(line -> log.append(line).append("\n"));
            input.close();
        } catch (FileNotFoundException ignored) {}
        return new MinecraftServerLog(log.toString());
    }

    public MinecraftServerLog loadChatLog(){
        File logFile = new File(getFilePath() + "/logs/latest.log");
        StringBuilder log = new StringBuilder();
        try {
            Scanner input = new Scanner(logFile);
            input.useDelimiter("\n");
            input.forEachRemaining(line -> {
                if(line.contains("]: <")) {
                    log.append(line).append("\n");
                }
            });
            input.close();
        } catch (FileNotFoundException ignored) {}
        return new MinecraftServerLog(log.toString());
    }

    public void startServer(){
        if(status.equals(MC_SERVER_STARTING) ||
                status.equals(MC_SERVER_ONLINE) ||
                status.equals(MC_SERVER_STOPPING) ||
                status.equals(MC_SERVER_UPDATING)) return;
        online = new LinkedList<>();
        status = MC_SERVER_STARTING;
        log.debug("Starting " + name);
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(filePath));
        if(serverConfig.getStartCommand().endsWith(".bat")){
            String bat = new File(filePath).getAbsolutePath() + "/" + serverConfig.getStartCommand();
            pb.command(bat);
        } else {
            pb.command(serverConfig.getStartCommand().split(" "));
        }
        try {
            serverProcess = pb.start();
            processOutput = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));
            processErrorOutput = new BufferedReader(new InputStreamReader(serverProcess.getErrorStream()));
            processInput = new PrintStream(serverProcess.getOutputStream());
            serverWatch();
        } catch (IOException e) {
            log.error("Unable to start server process", e);
        }
    }

    public void stopServer(){
        log.debug("Stopping " + name);
        status = MC_SERVER_STOPPING;
        sendServerCommand("stop");
    }

    public void restartServer() {
        new Thread(() -> {
            stopServer();
            while(!status.equals(MC_SERVER_OFFLINE)) {
                sleep(250);
            }
            startServer();
        }, "restart").start();
    }

    public void setServerConfig(MinecraftServerConfig config){
        serverConfig = config;
        saveServerConfig();
    }

    public void setServerProperty(String key, String value){
        File properties = new File(filePath + "/server.properties");
        if(!properties.exists()) {
            try {
                properties.createNewFile();
            } catch (IOException ignored) {}
        }
        try {
            // output file
            PrintWriter out = new PrintWriter(properties);
            serverProperties.put(key, value);
            serverProperties.forEach((k, v) -> out.println(k + "=" + v));
            out.flush();
            out.close();
        } catch (FileNotFoundException ignored) {}
        loadServerProperties();
    }

    public void sendServerCommand(String command){
        processInput.println(command);
        processInput.flush();
    }

    public void updateServerVersion(String version, String download, File masterBackupDir){
        if(status.equals(MC_SERVER_UPDATING)) return;
        log.debug("Updating " + name + " to " + version);
        if(status.equals(MC_SERVER_ONLINE)) stopServer();
        blockThreadUntilOffline();
        status = MC_SERVER_UPDATING;
        if(serverConfig.getCategory().equals("vanilla")) {
            updateVanillaServer(download, version, masterBackupDir);
        } else if(serverConfig.getCategory().contains("ATM9")) {
            updateATM9Server(download, version, masterBackupDir);
        }
    }

    @JsonIgnore
    public HashMap<String, String> getUsernameCache(){
        File file = new File(filePath + "/usernamecache.json");
        if(!file.exists()) return new HashMap<>();
        try {
            ObjectMapper om = new ObjectMapper();
            return om.readValue(file, HashMap.class);
        } catch (Exception e){
            return new HashMap<>();
        }
    }

    @JsonIgnore
    public HashMap<String, JsonNode> getUserStats(){
        String levelName = serverProperties.get("level-name");
        File userStatsDir = new File(filePath + "/" + levelName + "/stats");
        if(!userStatsDir.exists()) return new HashMap<>();

        HashMap<String, JsonNode> stats = new HashMap<>();
        ObjectMapper om = new ObjectMapper();
        for(File userStatsFile: userStatsDir.listFiles()){
            try {
                stats.put(userStatsFile.getName().replace(".json", ""), om.readValue(userStatsFile, JsonNode.class));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return stats;
    }

    public void backupWorld(File masterBackupDir){
        log.debug("Backup up world " + serverProperties.get("level-name"));
        File backupDir = new File(masterBackupDir.getAbsolutePath() + "/" + name);
        backupDir.mkdirs();
        String worldName = serverProperties.get("level-name");
        SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd");
        File backupDest = new File(backupDir + "/" + format.format(new Date()) + ".zip");
        int i = 0;
        while(backupDest.exists()) backupDest = new File(backupDir + "/" + format.format(new Date()) + "-" + (i++) + ".zip");
        zip(filePath + "/" + worldName, backupDest.getPath());
    }

    private void updateATM9Server(String download, String version, File masterBackupDir){
        new Thread(() -> {
            updateMessage("Backing up world", 0.0);
            backupWorld(masterBackupDir);
            File serverDir = new File(filePath);
            File tempDir = new File(serverDir.getParentFile().getParentFile().getPath() + "/temp/" + name + "-temp");
            File backDir = new File(serverDir.getParentFile().getParentFile().getPath() + "/temp/" + name + "-backup");
            backDir.mkdirs();
            tempDir.mkdirs(); // create temp dir
            updateMessage("Downloading server", 0.0);
            downloadServer(tempDir, download); // download server to temp dir
            updateMessage("Unpacking server", 0.17);
            unzipFile(tempDir + "/server.jar"); // unzip download
            unfoldDir(findDir(tempDir)); // unfold download
            new File(tempDir.getPath() + "/server.jar").delete(); // delete download
            updateMessage("Backing up old server", 0.34);
            for(File file: tempDir.listFiles()){ // move files that exist in both tempDir and serverDir to backDir
                File old = new File(serverDir.getPath() + "/" + file.getName());
                if(old.getName().equals("config") || !old.exists()) continue;
                try {
                    Files.move(old.toPath(), new File(backDir.getPath() + "/" + old.getName()).toPath());
                } catch (IOException ignored) {}
            }
            updateMessage("Moving files", 0.51);
            for(File file: tempDir.listFiles()){ // move files from temp dir to server dir
                File newF = new File(serverDir.getPath() + "\\" + file.getName());
                if(newF.getName().equals("config")) continue;
                try {
                    Files.move(file.toPath(), newF.toPath());
                } catch (IOException e) {
                    log.error("error moving file", e);
                }
            }
            for(File file: new File(tempDir.getPath() + "/config").listFiles()){
                try {
                    Files.move(file.toPath(), new File(serverDir.getPath() + "/config/" + file.getName()).toPath());
                } catch (IOException ignored) {}
            }
            new File(filePath + "\\libraries").delete();
            File startServerBat = new File(serverDir + "/startserver.bat");
            StringBuilder newStartServerBat = new StringBuilder();
            try{
                Scanner in = new Scanner(startServerBat);
                while(in.hasNextLine()) {
                    String line = in.nextLine();
                    if(line.startsWith(":START")) break;
                    newStartServerBat.append(line).append("\n");
                }
                in.close();
                PrintWriter out = new PrintWriter(startServerBat);
                out.println(newStartServerBat);
                out.flush();
                out.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            updateMessage("Installing forge", 0.68);
            startScriptAndBlock("startserver.bat", filePath, 60L); // run script to install new forge
            updateMessage("Messing with some properties", 0.85);
            File runbat = new File(serverDir.getPath() + "\\run.bat");
            try { editRunBat(runbat); } catch (FileNotFoundException ignored) {}
            FileSystemUtils.deleteRecursively(tempDir); // remove temp dir
            FileSystemUtils.deleteRecursively(backDir); // remove back dir
            setServerProperty("motd", "All the Mods 9\\: " + version.split("-")[2]);
            updateMessage("Finished updating server", 1.0);
            serverConfig.setVersion(version);
            saveServerConfig();
            status = MC_SERVER_OFFLINE;
            if(getServerConfig().isAutoStart()) startServer();
            DiscordService.updateMessage("Updated ATM9 to " + version.split("-")[2]);
        }, "Update").start();
    }

    private void updateVanillaServer(String download, String version, File masterBackupDir){
        new Thread(() -> {
            File loggerFile = new File(filePath + "\\msu_update.log");
            if(loggerFile.exists()) loggerFile.delete();
            SimpleLogger l = new SimpleLogger(loggerFile);
            l.info("Backing up world");
            backupWorld(masterBackupDir);
            File serverDir = new File(filePath);
            File serverJar = new File(filePath + "/server.jar");
            l.info("Deleting old jar");
            updateMessage("Deleting old jar", 0.0);
            serverJar.delete();
            updateMessage("Downloading new server", 0.5);
            l.info("Downloading new server");
            downloadServer(serverDir, download);
            setServerProperty("motd", "Vanilla\\: " + version);
            l.info("Complete");
            updateMessage("Complete", 1.0);
            serverConfig.setVersion(version);
            saveServerConfig();
            status = MC_SERVER_OFFLINE;
            if(getServerConfig().isAutoStart()) startServer();
        }, "Update").start();
    }

    private void processServerLine(String line){
        if(line.contains("]: Done (")){
            status = MC_SERVER_ONLINE;
            loadServerProperties();
        } else if(line.contains("Stopping server")){
            status = MC_SERVER_STOPPING;
        } else if(line.endsWith(" joined the game") && !line.contains("<") && !line.contains(">")){
            String username = extractUsername(line);
            online.add(username);
            socketActions.getPlayerAction().action(name, new MinecraftServerPlayersPacket(online));
            playerNotification.action(name, username, true, online);
        } else if(line.endsWith(" left the game") && !line.contains("<") && !line.contains(">")){
            String username = extractUsername(line);
            online.remove(username);
            socketActions.getPlayerAction().action(name, new MinecraftServerPlayersPacket(online));
            playerNotification.action(name, username, false, online);
        }
    }

    private String extractUsername(String line){
        return line.split(":")[3].trim().split(" ")[0];
    }

    public int getPlayersOnline(){
        return online.size();
    }

    private void serverWatch(){
        new Thread(() -> { // Thread for handling input from the server console
            while(serverProcess.isAlive()){
                try {
                    String line = processOutput.readLine();
                    if(line == null) continue;
                    log.debug(line);
                    processServerLine(line);
                    socketActions.getMessageAction().action(name, line);
                } catch (IOException ignored) {}
            }
            if(status.equals(MC_SERVER_STOPPING)){
                status = MC_SERVER_OFFLINE;
            } else {
                status = MC_SERVER_CRASHED;
            }
            online.clear();
        }, name + " watch").start();
        new Thread(() -> { // Thread for watching server status.
            socketActions.getStatusAction().action(name, status);
            statusNotificationAction.action(name, status);
            String currentStatus = status;
            while(true){
                sleep(1000);
                if(currentStatus.equals(status)) continue;
                currentStatus = status;
                socketActions.getStatusAction().action(name, status);
                statusNotificationAction.action(name, status);
            }
        }, name + " status watch").start();
        new Thread(() -> { // Thread for watching for stopping status
            while(true){
                if(status.equals(MC_SERVER_STOPPING)){
                    sleep(40000);
                    if(status.equals(MC_SERVER_STOPPING)){
                        serverProcess.destroyForcibly();
                        status = MC_SERVER_CRASHED;
                    }
                }
                sleep(1000);
            }
        }, "Stopping thread").start();
    }

    private void loadServerProperties(){
        serverProperties = new HashMap<>();
        File properties = new File(filePath + "/server.properties");
        name = properties.getParentFile().getName();
        try {
            Scanner input = new Scanner(properties);
            while(input.hasNextLine()){
                String line = input.nextLine();
                if(!line.contains("=")) continue;
                String[] spaces = line.split("=");
                String key = spaces[0];
                String value = spaces.length > 1 ? line.split("=")[1] : "";
                serverProperties.put(key.trim(), value.trim());
            }
            input.close();
        } catch (FileNotFoundException e) {
            log.error("Error reloading server data", e);
        }
    }

    private void loadServerConfig(){
        File config = new File(filePath + "/msu_config.json");
        ObjectMapper om = new ObjectMapper();
        try {
            serverConfig = om.readValue(config, MinecraftServerConfig.class);
        } catch (IOException e) {
            log.error("Unable to read server config", e);
        }
    }

    private void saveServerConfig(){
        File config = new File(filePath + "/msu_config.json");
        ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
        try {
            ow.writeValue(config, serverConfig);
        } catch (IOException e) {
            log.error("Unable to write server config", e);
        }
    }

    private void blockThreadUntilOffline(){
        while(!status.equals(MC_SERVER_OFFLINE)) {
            sleep(100);
        }
    }

    private void updateMessage(String stage, double percentage){
        HashMap<String, String> vals = new HashMap<>();
        vals.put("stage", stage);
        vals.put("percentage", percentage + "");
        socketActions.getUpdateAction().action(name, vals);
    }
}
