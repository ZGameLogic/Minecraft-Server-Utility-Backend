package com.zgamelogic.services;

import com.zgamelogic.data.database.curseforge.CurseforgeProject;
import com.zgamelogic.data.services.curseforge.CurseforgeMod;
import com.zgamelogic.data.services.minecraft.MinecraftServerVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.*;

public abstract class MinecraftService {

    /**
     * This method retrieves different versions of Minecraft servers.
     * @param curseforgeToken  The CurseForge token, provided as a string.
     * @param projects A list of CurseForgeProject instances.
     * @return A map (keyed by category name) that contains server versions for each provided project.
     */
    public static HashMap<String, HashMap<String, MinecraftServerVersion>> getMinecraftServerVersions(String curseforgeToken, List<CurseforgeProject> projects){
        HashMap<String, HashMap<String, MinecraftServerVersion>> versions = new HashMap<>();
        HashMap<String, MinecraftServerVersion> vanillaVersions = new HashMap<>();
        try {
            Document doc = Jsoup.connect("https://mcversions.net").get();
            LinkedList<Thread> threads = new LinkedList<>();
            doc.getElementsByClass("ncItem").forEach(element -> {
                String id = element.id();
                if(id.toLowerCase().contains("w") || id.toLowerCase().contains("pre") || id.toLowerCase().contains("rc")) return;
                String downloadPage = element.select("a").get(0).absUrl("href");
                    threads.add(new Thread(() -> {
                        try {
                            Document downloadDoc = Jsoup.connect(downloadPage).get();
                            String downloadServerLink = downloadDoc.select("a:contains(Download Server Jar)").get(0).select("a").get(0).absUrl("href");
                            synchronized (vanillaVersions){
                                vanillaVersions.put(id, new MinecraftServerVersion(id, downloadServerLink));
                            }
                        } catch (IOException | IndexOutOfBoundsException ignored) {}
                    }));
                    threads.getLast().start();
            });
            while(!threads.isEmpty()) threads.removeIf(thread -> !thread.isAlive());
        } catch (IOException ignored) {}
        versions.put("vanilla", vanillaVersions);

        projects.forEach(project -> {
            HashMap<String, MinecraftServerVersion> projectVersions = new HashMap<>();
            CurseforgeMod mod = CurseforgeService.getCurseforgeMod(curseforgeToken, project.getId());
            if(mod.getServerFileName() == null || mod.getServerFileUrl() == null) return;
            projectVersions.put(mod.getServerFileName(), new MinecraftServerVersion(mod.getServerFileName(), mod.getServerFileUrl()));
            versions.put(project.getName(), projectVersions);
        });

        return versions;
    }

    /**
     * This method downloads a Minecraft server using the provided directory and link.
     * @param dir  The target directory for the server files.
     * @param link The link to the Minecraft server.
     */
    public static void downloadServer(File dir, String link){
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.execute(link, HttpMethod.GET, requestCallback ->
                requestCallback.getHeaders().setContentType(MediaType.APPLICATION_OCTET_STREAM), clientHttpResponse -> {
            FileCopyUtils.copy(clientHttpResponse.getBody(), new FileOutputStream(dir.getPath() + "/server.jar"));
            return null;
        });
    }

    /**
     * This method gets the newest versions of the Minecraft servers from a provided map.
     * @param serverVersions A map that contains server versions for each server.
     * @return A map (keyed by category name) of the newest server versions.
     */
    public static HashMap<String, String> getNewestVersions(HashMap<String, HashMap<String, MinecraftServerVersion>> serverVersions){
        HashMap<String, String> versionMap = new HashMap<>();
        serverVersions.forEach((category, versions) -> {
            LinkedList<MinecraftServerVersion> versionList = new LinkedList<>(versions.values());
            Collections.sort(versionList);
            versionMap.put(category, versionList.getFirst().getVersion());
        });
        return versionMap;
    }
}