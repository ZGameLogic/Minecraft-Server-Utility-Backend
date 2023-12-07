package com.zgamelogic.data.minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SimpleLogger {
    private final File logFile;

    public SimpleLogger(String path){
        this(new File(path));
    }

    public SimpleLogger(File logFile){
        this.logFile = logFile;
    }

    public void info(String message){
        log("INFO", message);
    }

    public void debug(String message){
        log("DEBUG", message);
    }

    public void warn(String message){
        log("WARN", message);
    }

    public void error(String message){
        log("ERROR", message);
    }

    private void log(String level, String message){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder line = new StringBuilder();
        line.append("[").append(simpleDateFormat.format(new Date())).append("] ");
        line.append("[").append(level).append("] ");
        line.append(message);
        try {
            Files.writeString(
                logFile.toPath(),
                "\n" + line,
                StandardOpenOption.APPEND,
                StandardOpenOption.CREATE
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
