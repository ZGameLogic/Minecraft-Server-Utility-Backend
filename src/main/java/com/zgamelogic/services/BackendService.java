package com.zgamelogic.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@SuppressWarnings("unused")
@Slf4j
public abstract class BackendService {

    /**
     * @param script cmd line or bat to run
     * @param filePath path to run script in
     */
    public static void startScriptAndBlock(String script, String filePath){
        startScriptAndBlock(script, new File(filePath));
    }

    /**
     * @param script cmd line or bat to run
     * @param dir dir to run the process in
     */
    public static void startScriptAndBlock(String script, File dir){
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(dir).command(dir.getAbsolutePath() + "\\" + script);
        log.info(Strings.join(pb.command(), ' '));
        try {
            Process update = pb.start();
            BufferedReader input = new BufferedReader(new InputStreamReader(update.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(update.getErrorStream()));
            while (update.isAlive()) {
                log.info(input.readLine());
            }
            String line;
            while((line = error.readLine()) != null){
                log.error(line);
            }
        } catch (IOException e) {
            log.error("Error running script", e);
        }
    }

    /**
     * Unzips a file
     * @param filePath File path of the file to unzip
     */
    public static void unzipFile(String filePath){
        unzipFile(new File(filePath));
    }

    /**
     * Unzips a file
     * @param file File to unzip
     */
    public static void unzipFile(File file){
        File destDir = file.getParentFile();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(file.toPath()))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                String filePath = destDir + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
                        StreamUtils.copy(zipInputStream, fileOutputStream);
                    }
                } else {
                    Files.createDirectories(Path.of(filePath));
                }
                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Takes all the child dirs/file and brings them one level up. Removes original parent dir
     * This method will OVERWRITE
     * @param filePath File path to be unfolded
     */
    public static void unfoldDir(String filePath){
        unfoldDir(new File(filePath));
    }

    /**
     * Takes all the child dirs/file and brings them one level up. Removes original parent dir.
     * This method will OVERWRITE
     * @param dir Dir to be unfolded
     */
    public static void unfoldDir(File dir){
        File target = dir.getParentFile();
        for(File f: dir.listFiles()){
            try {
                Files.move(f.toPath(), new File(target + "/" + f.getName()).toPath(), REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("Error unfolding dir", e);
            }
        }
        dir.delete();
    }

    /**
     * @param filePath File path to find the dir in
     * @return File representing the child dir
     */
    public static File findDir(String filePath){
        return findDir(new File(filePath));
    }

    /**
     * @param dir Dir to find the dir in
     * @return File representing the child dir
     */
    public static File findDir(File dir){
        for(File f: dir.listFiles()){
            if(f.isDirectory()) return f;
        }
        return null;
    }
}
