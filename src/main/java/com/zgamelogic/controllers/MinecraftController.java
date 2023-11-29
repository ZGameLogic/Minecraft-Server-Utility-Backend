package com.zgamelogic.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.zgamelogic.data.database.curseforge.CurseforgeProject;
import com.zgamelogic.data.database.curseforge.CurseforgeProjectRepository;
import com.zgamelogic.data.services.curseforge.CurseforgeMod;
import com.zgamelogic.data.services.minecraft.*;
import com.zgamelogic.services.CurseforgeService;
import com.zgamelogic.services.MinecraftService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static com.zgamelogic.data.Constants.*;
import static com.zgamelogic.services.MinecraftService.downloadServer;

@Slf4j
@RestController
@PropertySource("File:msu.properties")
public class MinecraftController {

    @Value("${curseforge.token}")
    private String curseforgeToken;

    private final static File SERVERS_DIR = new File("data/servers");
    private final HashMap<String, MinecraftServer> servers;
    private HashMap<String, HashMap<String, MinecraftServerVersion>> serverVersions;

    private final CurseforgeProjectRepository curseforgeProjectRepository;
    private final WebSocketService webSocketService;

    @Autowired
    private MinecraftController(CurseforgeProjectRepository curseforgeProjectRepository, WebSocketService webSocketService){
        this.webSocketService = webSocketService;
        this.curseforgeProjectRepository = curseforgeProjectRepository;
        if(!SERVERS_DIR.exists()) SERVERS_DIR.mkdirs();
        serverVersions = new HashMap<>();
        servers = new HashMap<>();
        for(File server: SERVERS_DIR.listFiles()){
            servers.put(server.getName(), new MinecraftServer(server, this::serverMessageAction, this::serverStatusAction));
        }
        log.info("Starting minecraft auto-start servers...");
        servers.values().stream().filter(mcServer -> mcServer.getServerConfig().isAutoStart())
                .forEach(MinecraftServer::startServer);
    }

    @PostConstruct
    private void postConstruct(){
        updateServerVersions();
    }

    @ResponseBody
    @GetMapping({"/servers", "/servers/{server}"})
    private Collection<MinecraftServer> getServers(@PathVariable(required = false) String server){
        if(server == null) return servers.values();
        if(!servers.containsKey(server)) return List.of();
        return List.of(servers.get(server));
    }

    @ResponseBody
    @GetMapping({"/server/log/", "/server/log/{server}"})
    private Map<String, MinecraftServerLog> getServerLog(@PathVariable(required = false) String server){
        HashMap<String, MinecraftServerLog> data = new HashMap<>();
        LinkedList<MinecraftServer> servers = new LinkedList<>();
        if(server != null){
            servers.add(this.servers.get(server));
        } else {
            servers.addAll(this.servers.values());
        }
        servers.removeIf(s -> !s.getStatus().equals(MC_SERVER_ONLINE));
        servers.forEach(s -> data.put(s.getName(), s.loadLog()));
        return data;
    }

    @ResponseBody
    @GetMapping("/server/versions")
    public HashMap<String, LinkedList<String>> getMinecraftServerVersions(){
        HashMap<String, LinkedList<String>> data = new HashMap<>();
        serverVersions.forEach((key, value) -> data.put(key, new LinkedList<>(value.keySet())));
        return data;
    }

    @PostMapping("server/command")
    private void sendCommand(@RequestBody MinecraftServerStatusCommand command){
        if(!servers.containsKey(command.server())) return;
        switch (command.command()){
            case "restart":
                servers.get(command.server()).restartServer();
                break;
            case "stop":
                servers.get(command.server()).stopServer();
                break;
            case "start":
                servers.get(command.server()).startServer();
                break;
        }
    }

    @ResponseBody
    @PostMapping("server/create/check")
    private ResponseEntity<CompletionMessage> checkServerCreation(@Valid @RequestBody MinecraftServerCreationData data, BindingResult bindingResult){
        HashMap<String, String> failReasons = new HashMap<>();
        bindingResult.getFieldErrors().forEach(fieldError -> failReasons.put(fieldError.getField(), fieldError.getDefaultMessage()));
        if(data.getPort() != null && !checkOpenServerPort(data.getPort())) failReasons.put("port", MC_SERVER_CREATE_PORT_CONFLICT);
        if(data.getName() != null && !checkOpenServerName(data.getName())) failReasons.put("name", MC_SERVER_CREATE_NAME_CONFLICT);
        if(data.getVersion() != null && !checkValidServerVersion(data.getCategory(), data.getVersion())) failReasons.put("version", MC_SERVER_CREATE_VERSION_DOESNT_EXIST);

        if(failReasons.isEmpty()) return ResponseEntity.ok(CompletionMessage.success("All this info checks out."));
        return ResponseEntity.badRequest().body(CompletionMessage.fail(MC_SERVER_CREATE_CONFLICT, failReasons));
    }

    @ResponseBody
    @PostMapping("server/create")
    private ResponseEntity<CompletionMessage> createServer(@Valid @RequestBody MinecraftServerCreationData data, BindingResult bindingResult){
        ResponseEntity<CompletionMessage> validationCheck = checkServerCreation(data, bindingResult);
        if(!validationCheck.getStatusCode().equals(HttpStatus.OK)) return validationCheck;
        MinecraftServerConfig config = new MinecraftServerConfig(data);
        File serverDir = new File(SERVERS_DIR + "/" + data.getName());
        serverDir.mkdirs();
        try {
            File eula = new File(serverDir.getPath() + "/eula.txt");
            PrintWriter eulaPW = new PrintWriter(eula);
            eulaPW.println("eula=true");
            eulaPW.flush();
            eulaPW.close();
            File props = new File(serverDir.getPath() + "/server.properties");
            PrintWriter propsPW = new PrintWriter(props);
            propsPW.println("server-port=" + data.getPort());
            propsPW.flush();
            propsPW.close();
            File configFile = new File(serverDir.getPath() + "/msu_config.json");
            ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
            ow.writeValue(configFile, config);
        } catch (IOException ignored) {}
        String download = serverVersions.get(data.getCategory()).get(data.getVersion()).getUrl();
        downloadServer(serverDir, download);
        servers.put(serverDir.getName(), new MinecraftServer(serverDir, this::serverMessageAction, this::serverStatusAction));
        if(config.isAutoStart()) servers.get(data.getName()).startServer();
        return ResponseEntity.ok(CompletionMessage.success(MC_SERVER_CREATE_SUCCESS, servers.get(serverDir.getName())));
    }

    @PostMapping("server/update")
    private void updateServer(@RequestBody MinecraftServerUpdateCommand updateCommand){
        servers.get(updateCommand.getServer()).updateServerVersion(serverVersions.get(updateCommand.getCategory()).get(updateCommand.getVersion()).getUrl());
    }

    @ResponseBody
    @GetMapping("curseforge/project")
    private CurseforgeMod getCurseforgeProject(@RequestBody CurseforgeProjectData data){
        return CurseforgeService.getCurseforgeMod(curseforgeToken, data.getProjectId());
    }

    @PostMapping("curseforge/project")
    private void addCurseforgeProject(@RequestBody CurseforgeProjectData data){
        CurseforgeMod project = CurseforgeService.getCurseforgeMod(curseforgeToken, data.getProjectId());
        curseforgeProjectRepository.save(new CurseforgeProject(data.getProjectId(), project.getName()));
        updateServerVersions();
    }

    @MessageMapping("/server/{server}")
    public void serverWebsocketMessage(
            @DestinationVariable String server,
            MinecraftWebsocketDataRequest message
    ) {
        if(!servers.containsKey(server)) return;
        MinecraftServer mcServer = servers.get(server);
        switch(message.getAction()){
            case "start":
                mcServer.startServer();
                break;
            case "restart":
                mcServer.restartServer();
                break;
            case "stop":
                mcServer.stopServer();
                break;
            case "update":
                break;
            case "command":
                mcServer.sendServerCommand(message.getData().get("command"));
                break;
        }
    }

    @PreDestroy
    private void preDestroy(){
        servers.values().stream().filter(mcServer -> mcServer.getStatus().equals("Online"))
                .forEach(MinecraftServer::stopServer);
    }

    @Scheduled(cron = "0 0 0 ? * *")
    private void updateServerVersions(){
        serverVersions = MinecraftService.getMinecraftServerVersions(curseforgeToken, curseforgeProjectRepository.findAll());
    }

    private boolean checkOpenServerName(String name){
        return !servers.containsKey(name);
    }

    private boolean checkValidServerVersion(String category, String version){
         if(!serverVersions.containsKey(category)) return false;
         return serverVersions.get(category).containsKey(version);
    }

    private boolean checkOpenServerPort(int port){
        return servers.values().stream().noneMatch(server -> {
            if(!server.getServerProperties().containsKey("server-port")) return false;
            return Integer.parseInt(server.getServerProperties().get("server-port")) == port;
        });
    }

    private void serverMessageAction(String name, String line){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("log", line, name);
        webSocketService.sendMessage("/server/" + name, msm);
    }

    private void serverStatusAction(String name, String status){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("status", status, name);
        webSocketService.sendMessage("/server/" + name, msm);
    }
}