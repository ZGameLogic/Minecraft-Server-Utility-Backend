package com.zgamelogic.data.services.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.zgamelogic.data.minecraft.MinecraftServerSocketAction;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import static com.zgamelogic.data.Constants.*;

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

    public MinecraftServer(File serverDir, MinecraftServerSocketAction messageAction, MinecraftServerSocketAction statusAction, MinecraftServerSocketAction playerAction){
        filePath = serverDir.getPath();
        this.messageAction = messageAction;
        this.statusAction = statusAction;
        this.playerAction = playerAction;
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
        log.info("Starting " + name);
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(filePath));
        pb.command(serverConfig.getStartCommand().split(" "));
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
        log.info("Stopping " + name);
        status = MC_SERVER_STOPPING;
        sendServerCommand("stop");
    }

    public void restartServer() {
        new Thread(() -> {
            stopServer();
            while(!status.equals(MC_SERVER_OFFLINE)) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ignored) {}
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

    public void updateServerVersion(String download){
        if(status.equals(MC_SERVER_ONLINE)) stopServer();
        blockThreadUntilOffline();
        status = MC_SERVER_UPDATING;
        new Thread(() -> {
            // TODO download file
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(filePath));
            pb.command(serverConfig.getUpdateScript());
            try {
                Process update = pb.start();
                while(update.isAlive()){
                    Thread.sleep(250);
                }
            } catch (IOException | InterruptedException ignored) {}
            status = MC_SERVER_OFFLINE;
        }, "Update").start();
        blockThreadUntilOffline();
        if(getServerConfig().isAutoStart()){
            startServer();
        } else {
            status = MC_SERVER_OFFLINE;
        }
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
        new Thread(() -> {
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
        new Thread(() -> {
            String currentStatus = status;
            while(true){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
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
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
        }
    }
}
