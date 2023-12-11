package com.zgamelogic.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.zgamelogic.data.database.curseforge.CurseforgeProject;
import com.zgamelogic.data.database.curseforge.CurseforgeProjectRepository;
import com.zgamelogic.data.database.user.UserRepository;
import com.zgamelogic.data.services.curseforge.CurseforgeMod;
import com.zgamelogic.data.services.minecraft.*;
import com.zgamelogic.data.services.minecraft.MinecraftSocketMessage;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

import static com.zgamelogic.data.Constants.*;
import static com.zgamelogic.services.BackendService.*;
import static com.zgamelogic.services.MinecraftService.downloadServer;

@Slf4j
@RestController
@PropertySource("File:msu.properties")
public class MinecraftController {

    @Value("${curseforge.token}") private String curseforgeToken;

    private final static File SERVERS_DIR = new File("data/servers");
    private final HashMap<String, MinecraftServer> servers;
    private HashMap<String, HashMap<String, MinecraftServerVersion>> serverVersions;

    private final CurseforgeProjectRepository curseforgeProjectRepository;
    private final WebSocketService webSocketService;
    private final UserRepository userRepository;

    @Autowired
    private MinecraftController(
            CurseforgeProjectRepository curseforgeProjectRepository,
            WebSocketService webSocketService,
            UserRepository userRepository
    ){
        this.webSocketService = webSocketService;
        this.curseforgeProjectRepository = curseforgeProjectRepository;
        this.userRepository = userRepository;
        if(!SERVERS_DIR.exists()) SERVERS_DIR.mkdirs();
        serverVersions = new HashMap<>();
        servers = new HashMap<>();
        for(File server: SERVERS_DIR.listFiles()){
            if(server.isDirectory()) {
                servers.put(server.getName(), new MinecraftServer(server, this::serverMessageAction, this::serverStatusAction, this::serverPlayerAction, this::serverUpdateAction));
            }
        }
    }

    @PostConstruct
    private void postConstruct(){
        updateServerVersions();
        thirtyMinuteTasks();
        log.info("Starting minecraft auto-start servers...");
        servers.values().stream().filter(mcServer -> mcServer.getServerConfig().isAutoStart())
                .forEach(MinecraftServer::startServer);
    }

    @ResponseBody
    @GetMapping({"/servers", "/servers/{server}"})
    private Collection<MinecraftServer> getServers(
            @PathVariable(required = false) String server,
            @CookieValue(name = "user", required = false) String id
    ){
        // TODO dont send configs and stuff if they arent allowed to edit
        if(server == null) return servers.values();
        if(!servers.containsKey(server)) return List.of();
        return List.of(servers.get(server));
    }

    @ResponseBody
    @GetMapping({"/server/log/", "/server/log/{server}"})
    private Map<String, MinecraftServerLog> getServerLog(
            @PathVariable(required = false) String server,
            @CookieValue(name = "user", required = false) String id
    ){
        // TODO dont send the configs if they cant send console commands
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
    public HashMap<String, LinkedList<String>> getMinecraftServerVersions(@CookieValue(name = "user", required = false) String id){
        // TODO dont send the versions if they arent authorized to create servers
        HashMap<String, LinkedList<String>> data = new HashMap<>();
        serverVersions.forEach((key, value) -> data.put(key, new LinkedList<>(value.keySet())));
        return data;
    }

    @PostMapping("server/command")
    private void sendCommand(
            @RequestBody MinecraftServerStatusCommand command,
            @CookieValue(name = "user", required = false) String id
    ){
        // TODO dont let them send commands if they arent able to send commands
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
    private ResponseEntity<CompletionMessage> checkServerCreation(
            @Valid @RequestBody MinecraftServerCreationData data,
            BindingResult bindingResult,
            @CookieValue(name = "user", required = false) String id
    ){
        // TODO dont let them send commands if they arent able to send commands
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
    private ResponseEntity<CompletionMessage> createServer(
            @Valid @RequestBody MinecraftServerCreationData data,
            BindingResult bindingResult,
            @CookieValue(name = "user", required = false) String id
    ){
        // TODO dont let them create if they cant create
        ResponseEntity<CompletionMessage> validationCheck = checkServerCreation(data, bindingResult, id);
        if(!validationCheck.getStatusCode().equals(HttpStatus.OK)) return validationCheck;
        new Thread(() -> installServer(data), "Install Server").start();
        return ResponseEntity.ok(CompletionMessage.success(MC_SERVER_CREATE_SUCCESS, "Starting install process. Listen on the websocket for completion"));
    }

    private void installServer(MinecraftServerCreationData data) {
        serverInstallAction(data.getName(), "Starting");
        MinecraftServerConfig config = new MinecraftServerConfig(data);
        File serverDir = new File(SERVERS_DIR + "/" + data.getName());
        serverDir.mkdirs();
        try {
            serverInstallAction(data.getName(), "Spoofing Eula");
            File eula = new File(serverDir.getPath() + "/eula.txt");
            PrintWriter eulaPW = new PrintWriter(eula);
            eulaPW.println("eula=true");
            eulaPW.flush();
            eulaPW.close();
            serverInstallAction(data.getName(), "Spoofing properties");
            File props = new File(serverDir.getPath() + "/server.properties");
            PrintWriter propsPW = new PrintWriter(props);
            propsPW.println("server-port=" + data.getPort());
            propsPW.flush();
            propsPW.close();
            serverInstallAction(data.getName(), "Saving config");
            File configFile = new File(serverDir.getPath() + "/msu_config.json");
            ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
            ow.writeValue(configFile, config);
        } catch (IOException ignored) {}
        serverInstallAction(data.getName(), "Downloading server");
        String download = serverVersions.get(data.getCategory()).get(data.getVersion()).getUrl();
        downloadServer(serverDir, download);
        if(data.getCategory().contains("ATM9")){
            serverInstallAction(data.getName(), "Extracting server files");
            unzipFile(serverDir + "/server.jar"); // unzip download
            new File(serverDir + "/server.jar").delete(); // delete download
            unfoldDir(findDir(serverDir)); // unfold download
            serverInstallAction(data.getName(), "Installing forge");
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
            startScriptAndBlock("startserver.bat", serverDir, 60L); // run script to install new forge
            serverInstallAction(data.getName(), "Fixing run.bat");
            File runbat = new File(serverDir.getPath() + "\\run.bat");
            try {
                editRunBat(runbat);
            } catch (FileNotFoundException ignored) {}
        }
        servers.put(serverDir.getName(), new MinecraftServer(serverDir, this::serverMessageAction, this::serverStatusAction, this::serverPlayerAction, this::serverUpdateAction));
        if(config.isAutoStart()) servers.get(data.getName()).startServer();
        serverInstallAction(data.getName(), "Installed");
    }

    @PostMapping("server/update")
    private void updateServer(
            @RequestBody MinecraftServerUpdateCommand updateCommand,
            @CookieValue(name = "user", required = false) String id
    ){
        // TODO dont let them update if they dont have edit configs
        servers.get(updateCommand.getServer()).updateServerVersion(updateCommand.getVersion(), serverVersions.get(updateCommand.getCategory()).get(updateCommand.getVersion()).getUrl());
    }

    @ResponseBody
    @GetMapping("curseforge/project")
    private CurseforgeMod getCurseforgeProject(
            @RequestBody CurseforgeProjectData data,
            @CookieValue(name = "user", required = false) String id
    ){
        // TODO dont let them send if they dont have create perms
        return CurseforgeService.getCurseforgeMod(curseforgeToken, data.getProjectId());
    }

    @PostMapping("curseforge/project")
    private void addCurseforgeProject(
            @RequestBody CurseforgeProjectData data,
            @CookieValue(name = "user", required = false) String id
    ){
        // TODO dont let them send if they dont have create perms
        CurseforgeMod project = CurseforgeService.getCurseforgeMod(curseforgeToken, data.getProjectId());
        curseforgeProjectRepository.save(new CurseforgeProject(data.getProjectId(), project.getName()));
        updateServerVersions();
    }

    @MessageMapping("/server/{server}")
    public void serverWebsocketMessage(
            @DestinationVariable String server,
            MinecraftWebsocketDataRequest message
    ) {
        // TODO check user id if they are able to send it
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

    @Scheduled(cron = "0 */30 * * * *")
    private void thirtyMinuteTasks(){
        log.debug("Doing some upgrade stuff");
        HashMap<String, String> newestVersions = MinecraftService.getNewestVersions(serverVersions);
        servers.values().stream().filter(minecraftServer -> {
            if(!minecraftServer.getServerConfig().isAutoUpdate()) return false;
            if(newestVersions.get(minecraftServer.getServerConfig().getCategory()).isEmpty()) return false;
            if(minecraftServer.getServerConfig().getVersion().equals(newestVersions.get(minecraftServer.getServerConfig().getCategory()))) return false;
            return minecraftServer.getPlayersOnline() == 0;
        }
        ).forEach(server -> {
            log.debug("Updating " + server.getName());
            String cat = server.getServerConfig().getCategory();
            String ver = newestVersions.get(cat);
            String download = serverVersions.get(cat).get(ver).getUrl();
            server.updateServerVersion(ver, download);
        });
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

    private void serverMessageAction(String name, Object line){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("log", line, name);
        webSocketService.sendMessage("/server/" + name, msm);
    }

    private void serverStatusAction(String name, Object status){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("status", status, name);
        webSocketService.sendMessage("/server/" + name, msm);
    }

    private void serverUpdateAction(String name, Object status){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("update", status, name);
        webSocketService.sendMessage("/server/" + name, msm);
    }

    private void serverInstallAction(String name, Object status){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("install", status, name);
        webSocketService.sendMessage("/server/" + name, msm);
    }

    private void serverPlayerAction(String name, Object packet){
        MinecraftSocketMessage msm = new MinecraftSocketMessage("player", packet, name);
        webSocketService.sendMessage("/server/" + name, msm);
    }
}