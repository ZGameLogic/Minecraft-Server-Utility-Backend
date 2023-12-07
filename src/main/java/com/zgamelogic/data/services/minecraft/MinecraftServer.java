package com.zgamelogic.data.services.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.zgamelogic.data.minecraft.MinecraftServerSocketAction;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.nio.file.Files;
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
    private MinecraftServerSocketAction messageAction;
    @JsonIgnore
    private MinecraftServerSocketAction statusAction;
    @JsonIgnore
    private MinecraftServerSocketAction playerAction;
    @JsonIgnore
    private MinecraftServerSocketAction updateAction;

    public MinecraftServer(
            File serverDir,
            MinecraftServerSocketAction messageAction,
            MinecraftServerSocketAction statusAction,
            MinecraftServerSocketAction playerAction,
            MinecraftServerSocketAction updateAction
    ){
        filePath = serverDir.getPath();
        this.messageAction = messageAction;
        this.statusAction = statusAction;
        this.playerAction = playerAction;
        this.updateAction = updateAction;
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

    public void startServer(){
        if(!status.equals(MC_SERVER_OFFLINE) && !status.equals(MC_SERVER_CRASHED)) return;
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

    public void updateServerVersion(String version, String download){
        log.debug("Updating " + name + " to " + version);
        if(status.equals(MC_SERVER_ONLINE)) stopServer();
        blockThreadUntilOffline();
        status = MC_SERVER_UPDATING;
        if(serverConfig.getCategory().equals("vanilla")) {
            updateVanillaServer(download, version);
        } else if(serverConfig.getCategory().contains("ATM9")) {
            updateATM9Server(download, version);
        }
        serverConfig.setVersion(version);
        saveServerConfig();
    }

    private void updateATM9Server(String download, String version){
        new Thread(() -> {
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
            updateMessage("Installing forge", 0.68);
            startScriptAndBlock("startserver.bat", filePath, 60L); // run script to install new forge
            updateMessage("Messing with some properties", 0.85);
            File runbat = new File(serverDir.getPath() + "\\run.bat");
            try { editRunBat(runbat); } catch (FileNotFoundException ignored) {}
            FileSystemUtils.deleteRecursively(tempDir); // remove temp dir
            FileSystemUtils.deleteRecursively(backDir); // remove back dir
            setServerProperty("motd", "All the Mods 9\\: " + version.split("-")[2]);
            updateMessage("Finished updating server", 1.0);
            if(getServerConfig().isAutoStart()){
                startServer();
            } else {
                status = MC_SERVER_OFFLINE;
            }
        }, "Update").start();
    }

    private void updateVanillaServer(String download, String version){
        new Thread(() -> {
            File serverDir = new File(filePath);
            File serverJar = new File(filePath + "/server.jar");
            updateMessage("Deleting old jar", 0.0);
            serverJar.delete();
            updateMessage("Downloading new server", 0.5);
            downloadServer(serverDir, download);
            setServerProperty("motd", "Vanilla\\: " + version);
            updateMessage("Complete", 1.0);
            if(getServerConfig().isAutoStart()){
                startServer();
            } else {
                status = MC_SERVER_OFFLINE;
            }
        }, "Update").start();
    }

    private void processServerLine(String line){
        if(line.contains("]: Done (")){
            status = MC_SERVER_ONLINE;
            loadServerProperties();
        } else if(line.contains("Stopping server")){
            status = MC_SERVER_STOPPING;
        } else if(line.matches(MC_JOIN_GAME_REGEX)){
            online.add(extractUsername(line));
            playerAction.action(name, new MinecraftServerPlayersPacket(online));
        } else if(line.matches(MC_LEFT_GAME_REGEX)){
            online.remove(extractUsername(line));
            playerAction.action(name, new MinecraftServerPlayersPacket(online));
        }
    }

    private String extractUsername(String line){
        return line
                .replaceAll("\\[.*] \\[Server thread/INFO]: ", "")
                .replaceAll(" .*$", "")
                .replaceAll("[<>]", "");
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
                    messageAction.action(name, line);
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
            String currentStatus = status;
            while(true){
                sleep(1000);
                if(currentStatus.equals(status)) continue;
                currentStatus = status;
                statusAction.action(name, status);
            }
        }, name + " status watch").start();
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
        updateAction.action(name, vals);
    }
}
