package com.zgamelogic.data.services.minecraft;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.zgamelogic.data.minecraft.MinecraftServerMessageAction;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;

import static com.zgamelogic.data.Constants.*;

@NoArgsConstructor
@Slf4j
@Data
public class MinecraftServer {
    private String filePath;
    private String status;
    private String name;
    private HashMap<String, String> serverProperties;
    private MinecraftServerConfig serverConfig;

    @JsonIgnore
    private Process serverProcess;
    @JsonIgnore
    private BufferedReader processOutput;
    @JsonIgnore
    private BufferedReader processErrorOutput;
    @JsonIgnore
    private PrintStream processInput;
    @JsonIgnore
    private MinecraftServerMessageAction messageAction;

    public MinecraftServer(File serverDir, MinecraftServerMessageAction messageAction){
        filePath = serverDir.getPath();
        this.messageAction = messageAction;
        status = MC_SERVER_OFFLINE;
        serverProperties = new HashMap<>();
        loadServerProperties();
        loadServerConfig();
        if(serverConfig == null){
            serverConfig = new MinecraftServerConfig();
            saveServerConfig();
            loadServerConfig();
        }
    }

    public void startServer(){
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

    private void serverWatch(){
        new Thread(() -> {
            while(serverProcess.isAlive()){
                try {
                    String line = processOutput.readLine();
                    if(line == null) continue;
                    log.debug(line);
                    if(line.contains("]: Done (")){
                        status = MC_SERVER_ONLINE;
                    } else if(line.contains("Stopping server")){
                        status = MC_SERVER_STOPPING;
                    }
                    messageAction.action(name, line);
                } catch (IOException ignored) {}
            }
            if(status.equals(MC_SERVER_STOPPING)){
                status = MC_SERVER_OFFLINE;
            } else {
                status = MC_SERVER_CRASHED;
            }
        }, name + " watch").start();
    }

    private void loadServerProperties(){
        serverProperties = new HashMap<>();
        File properties = new File(filePath + "/server.properties");
        name = properties.getParentFile().getName();
        try {
            Scanner input = new Scanner(properties);
            input.useDelimiter("\n");
            input.forEachRemaining(line -> {
                if(line.split("=").length < 2) return;
                String key = line.split("=")[0];
                String value = line.split("=")[1];
                serverProperties.put(key, value);
            });
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
}
