package com.zgamelogic.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.zgamelogic.data.database.curseforge.CurseforgeProject;
import com.zgamelogic.data.database.curseforge.CurseforgeProjectRepository;
import com.zgamelogic.data.database.user.User;
import com.zgamelogic.data.database.user.UserRepository;
import com.zgamelogic.data.services.apple.ApplePushNotification;
import com.zgamelogic.data.services.apple.AppleWidgetPacket;
import com.zgamelogic.data.services.auth.NotificationMessage;
import com.zgamelogic.data.services.curseforge.CurseforgeMod;
import com.zgamelogic.data.services.minecraft.*;
import com.zgamelogic.data.services.minecraft.MinecraftSocketMessage;
import com.zgamelogic.services.CurseforgeService;
import com.zgamelogic.services.MinecraftService;
import com.zgamelogic.services.NotificationService;
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
    @Value("${backup.dir}") private String backupDir;

    private final static File SERVERS_DIR = new File("data/servers");
    private final HashMap<String, MinecraftServer> servers;
    private HashMap<String, HashMap<String, MinecraftServerVersion>> serverVersions;

    private final CurseforgeProjectRepository curseforgeProjectRepository;
    private final WebSocketService webSocketService;
    private final UserRepository userRepository;

    private final NotificationService notificationService;

    @Autowired
    private MinecraftController(
            CurseforgeProjectRepository curseforgeProjectRepository,
            WebSocketService webSocketService,
            UserRepository userRepository,
            NotificationService notificationService
    ){
        this.webSocketService = webSocketService;
        this.curseforgeProjectRepository = curseforgeProjectRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        if(!SERVERS_DIR.exists()) SERVERS_DIR.mkdirs();
        serverVersions = new HashMap<>();
        servers = new HashMap<>();
        for(File server: SERVERS_DIR.listFiles()){
            if(server.isDirectory()) {
                servers.put(server.getName(), new MinecraftServer(server, this::serverMessageAction, this::serverStatusAction, this::serverPlayerAction, this::serverUpdateAction, this::serverPlayerNotification, this::serverStatusNotification));
                updateUserNotificationsAndPermissions(server.getName());
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
    @GetMapping("/servers")
    private Collection<MinecraftServer> getServers(){
        return servers.values();
    }

    @ResponseBody
    @GetMapping("/count")
    private int getTotalPlayerCount(){
        int count = 0;
        for(MinecraftServer server: servers.values()){
            count += server.getPlayersOnline();
        }
        return count;
    }

    @ResponseBody
    @GetMapping("/servers/{server}")
    private ResponseEntity<MinecraftServer> getServer(@PathVariable String server){
        if(!servers.containsKey(server)) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(servers.get(server));
    }

    @ResponseBody
    @GetMapping("/server/log/{server}")
    private ResponseEntity<MinecraftServerLog> getServerLog(
            @PathVariable String server,
            @RequestHeader(name = "user", required=false) String id
    ){
        if(!userRepository.userHasPermission(id, server, MC_USE_CONSOLE_SERVER_PERMISSION)) return ResponseEntity.status(401).build();
        if(!servers.containsKey(server)) return ResponseEntity.status(404).build();
        return ResponseEntity.ok(servers.get(server).loadLog());
    }

    @ResponseBody
    @GetMapping("/server/versions")
    public ResponseEntity<HashMap<String, LinkedList<String>>> getMinecraftServerVersions(@RequestHeader(name = "user", required=false) String id){
        if(!userRepository.userHasPermission(id, MC_GENERAL_PERMISSION_CAT, MC_CREATE_SERVER_PERMISSION)) return ResponseEntity.status(401).build();
        HashMap<String, LinkedList<String>> data = new HashMap<>();
        serverVersions.forEach((key, value) -> data.put(key, new LinkedList<>(value.keySet())));
        return ResponseEntity.ok(data);
    }

    @ResponseBody
    @GetMapping("/widget/ios")
    public ResponseEntity<AppleWidgetPacket> getWidgetPacket(){
        AppleWidgetPacket packet = new AppleWidgetPacket(servers.values());
        return ResponseEntity.ok(packet);
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("server/command")
    private ResponseEntity sendCommand(
            @RequestBody MinecraftServerStatusCommand command,
            @RequestHeader(name = "user", required=false) String id
    ){
        if(!userRepository.userHasPermission(id, command.server(), MC_ISSUE_COMMANDS_SERVER_PERMISSION)) return ResponseEntity.status(401).build();
        if(!servers.containsKey(command.server())) ResponseEntity.status(404).build();
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
        return ResponseEntity.status(200).build();
    }

    @ResponseBody
    @PostMapping("server/create/check")
    private ResponseEntity<CompletionMessage> checkServerCreation(
            @Valid @RequestBody MinecraftServerCreationData data,
            BindingResult bindingResult,
            @RequestHeader(name = "user", required=false) String id
    ){
        if(!userRepository.userHasPermission(id, MC_GENERAL_PERMISSION_CAT, MC_CREATE_SERVER_PERMISSION)) return ResponseEntity.status(401).build();
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
            @RequestHeader(name = "user", required=false) String id
    ){
        if(!userRepository.userHasPermission(id, MC_GENERAL_PERMISSION_CAT, MC_CREATE_SERVER_PERMISSION)) return ResponseEntity.status(401).build();
        ResponseEntity<CompletionMessage> validationCheck = checkServerCreation(data, bindingResult, id);
        if(!validationCheck.getStatusCode().equals(HttpStatus.OK)) return validationCheck;
        new Thread(() -> installServer(data), "Install Server").start();
        User user = userRepository.getReferenceById(id);
        user.addPermission(data.getName(), MC_USE_CONSOLE_SERVER_PERMISSION + MC_ISSUE_COMMANDS_SERVER_PERMISSION + MC_EDIT_SERVER_PROPERTIES_PERMISSION);
        userRepository.save(user);
        updateUserNotificationsAndPermissions(data.getName());
        return ResponseEntity.ok(CompletionMessage.success(MC_SERVER_CREATE_SUCCESS, "Starting install process. Listen on the websocket for completion"));
    }

    private void updateUserNotificationsAndPermissions(String server) {
        userRepository.findAll().forEach(user -> {
            user.createNotificationPermission(server);
            userRepository.save(user);
        });
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
        } else if (data.getCategory().contains("BigChadGuys")){
            serverInstallAction(data.getName(), "Extracting server files");
            unzipFile(serverDir + "/server.jar"); // unzip download
            new File(serverDir + "/server.jar").delete(); // delete download
            unfoldDir(findDir(serverDir)); // unfold download
        }
        servers.put(serverDir.getName(), new MinecraftServer(serverDir, this::serverMessageAction, this::serverStatusAction, this::serverPlayerAction, this::serverUpdateAction, this::serverPlayerNotification, this::serverStatusNotification));
        if(config.isAutoStart()) servers.get(data.getName()).startServer();
        serverInstallAction(data.getName(), "Installed");
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("server/update")
    private ResponseEntity updateServer(
            @RequestBody MinecraftServerUpdateCommand updateCommand,
            @RequestHeader(name = "user", required=false) String id
    ){
        if(!userRepository.userHasPermission(id, updateCommand.getServer(), MC_ISSUE_COMMANDS_SERVER_PERMISSION)) return ResponseEntity.status(401).build();
        servers.get(updateCommand.getServer()).updateServerVersion(updateCommand.getVersion(), serverVersions.get(updateCommand.getCategory()).get(updateCommand.getVersion()).getUrl(), new File(backupDir));
        return ResponseEntity.status(200).build();
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("server/backup/{server}")
    private ResponseEntity backupServerWorld(
            @PathVariable String server,
            @RequestHeader(name = "user") String id
    ){
        if(!userRepository.userHasPermission(id, server, MC_EDIT_SERVER_PROPERTIES_PERMISSION)) return ResponseEntity.status(401).build();
        new Thread(() -> servers.get(server).backupWorld(new File(backupDir)), "Backup world").start();
        return ResponseEntity.status(200).build();
    }

    @ResponseBody
    @GetMapping("curseforge/project")
    private ResponseEntity<CurseforgeMod> getCurseforgeProject(
            @RequestBody CurseforgeProjectData data,
            @RequestHeader(name = "user", required=false) String id
    ){
        if(!userRepository.userHasPermission(id, MC_GENERAL_PERMISSION_CAT, MC_CREATE_SERVER_PERMISSION)) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(CurseforgeService.getCurseforgeMod(curseforgeToken, data.getProjectId()));
    }

    @SuppressWarnings("rawtypes")
    @PostMapping("curseforge/project")
    private ResponseEntity addCurseforgeProject(
            @RequestBody CurseforgeProjectData data,
            @RequestHeader(name = "user", required=false) String id
    ){
        if(!userRepository.userHasPermission(id, MC_GENERAL_PERMISSION_CAT, MC_CREATE_SERVER_PERMISSION)) return ResponseEntity.status(401).build();
        CurseforgeMod project = CurseforgeService.getCurseforgeMod(curseforgeToken, data.getProjectId());
        curseforgeProjectRepository.save(new CurseforgeProject(data.getProjectId(), project.getName()));
        updateServerVersions();
        return ResponseEntity.status(200).build();
    }

    @MessageMapping("/server/{server}")
    public void serverWebsocketMessage(
            @DestinationVariable String server,
            MinecraftWebsocketDataRequest message
    ) {
        if(!userRepository.userHasPermission(message.getUserId(), server, MC_ISSUE_COMMANDS_SERVER_PERMISSION)) return;
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

    @Scheduled(cron = "0 */29 * * * *")
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
            server.updateServerVersion(ver, download, new File(backupDir));
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

    private void serverPlayerNotification(String server, String player, boolean joined, LinkedList<String> online){
        String body = online.isEmpty() ? "No players are on " + server : "Players online: " + String.join(", ", online);
        ApplePushNotification notification = new ApplePushNotification(
                player + (joined ? " has joined " + server : " has left " + server),
                body
        );
        userRepository.findAll().forEach(user -> {
            if(!user.hasNotificationEnabled(server, NotificationMessage.Toggle.PLAYER)) return;
            user.getDeviceIds().forEach(device -> notificationService.sendNotification(device, notification));
        });
    }

    private void serverStatusNotification(String server, String status){
        ApplePushNotification notification = new ApplePushNotification(
                "Status for " + server + " has changed",
                "Status: " + status
        );
        userRepository.findAll().forEach(user -> {
            if(!user.hasNotificationEnabled(server, NotificationMessage.Toggle.STATUS)) return;
            user.getDeviceIds().forEach(device -> notificationService.sendNotification(device, notification));
        });
    }
}