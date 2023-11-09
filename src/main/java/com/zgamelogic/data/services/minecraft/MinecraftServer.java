package com.zgamelogic.data.services.minecraft;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

@NoArgsConstructor
@Slf4j
@Data
public class MinecraftServer {
    private String filePath;
    private String status;
    private String name;
    private HashMap<String, String> serverProperties;
    private MinecraftServerConfig serverConfig;

    public MinecraftServer(File serverDir){
        filePath = serverDir.getPath();
        status = "offline";
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

    }

    public void setAutoStart(boolean autoStart){
        serverConfig.setAutoStart(autoStart);
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
