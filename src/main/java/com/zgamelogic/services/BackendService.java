package com.zgamelogic.services;

import com.zgamelogic.data.minecraft.SimpleLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

@SuppressWarnings("unused")
@Slf4j
public abstract class BackendService {

    /**
     * @param script bat to run
     * @param filePath path to run script in
     */
    public static void startScriptAndBlock(String script, String filePath){
        startScriptAndBlock(script, new File(filePath));
    }

    /**
     * @param script bat to run
     * @param dir dir to run the process in
     */
    public static void startScriptAndBlock(String script, File dir){
        startScriptAndBlock(script, dir, null);
    }

    /**
     * @param script bat to run
     * @param filePath path to run script in
     * @param timeout timeout in seconds. if the process takes longer than this timeout, kill it
     */
    public static void startScriptAndBlock(String script, String filePath, Long timeout){
        startScriptAndBlock(script, new File(filePath), timeout);
    }

    /**
     * @param script bat to run
     * @param dir dir to run the process in
     * @param timeout timeout in seconds. if the process takes longer than this timeout, kill it
     */
    public static void startScriptAndBlock(String script, File dir, Long timeout){
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(dir).command(dir.getAbsolutePath() + "\\" + script);
        log.debug(Strings.join(pb.command(), ' '));
        File loggerFile = new File(dir.getAbsolutePath() + "\\msu_update.log");
        if(loggerFile.exists()) loggerFile.delete();
        SimpleLogger l = new SimpleLogger(loggerFile);
        l.info("Starting update process");
        try {
            Process update = pb.start();
            new Thread(() -> {
                Instant kill = Instant.now().plusSeconds(timeout);
                while(update.isAlive()){
                    Instant now = Instant.now();
                    if(now.isAfter(kill)) {
                        l.error("Killing process, took more than " + timeout + " seconds.");
                        update.destroyForcibly();
                    }
                    sleep(1000);
                }
            }, "cmd watch");
            BufferedReader input = new BufferedReader(new InputStreamReader(update.getInputStream()));
            BufferedReader error = new BufferedReader(new InputStreamReader(update.getErrorStream()));
            while (update.isAlive()) {
                String line = input.readLine();
                if(line != null) {
                    l.info(line);
                    log.debug(line);
                }
            }
            String line;
            while((line = error.readLine()) != null){
                l.error(line);
                log.error(line);
            }
            l.info("Finishing update script");
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
     * Zips a file or directory
     *
     * @param sourcePath The path of the file or directory to zip
     * @param zipFilePath The path of the zip file to create
     */
    public static void zip(String sourcePath, String zipFilePath) {
        File sourceFile = new File(sourcePath);
        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {
            zipFile(sourceFile, sourceFile.getName(), zipOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
            }
            zipOut.closeEntry();
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
                }
            }
            return;
        }
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            zipOut.closeEntry();
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

    /**
     * Edits the run.bat file generated by ATM9 to remove the pause, and add in no GUI
     * @param runbat File location of the run.bat
     */
    public static void editRunBat(File runbat) throws FileNotFoundException {
        StringBuilder newRunBat = new StringBuilder();
        Scanner in = new Scanner(runbat);
        while(in.hasNextLine()){
            String line = in.nextLine();
            if(line.startsWith("java")) {
                newRunBat.append(line.replace("%*", "nogui %*")).append("\n");
                break;
            }
            newRunBat.append(line).append("\n");
        }
        in.close();
        PrintWriter out = new PrintWriter(runbat);
        out.println(newRunBat);
        out.flush();
        out.close();
    }

    /**
     * Sleeps thread for certain amount of milliseconds. Thread safe.
     * @param milliseconds Milliseconds to sleep thread for.
     */
    public static void sleep(long milliseconds){
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ignored) {}
    }
}
