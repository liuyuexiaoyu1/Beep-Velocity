package com.liuyue.beep_Velocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


public class BeepConfig {
    public boolean enableBigBeep = true;
    public long cooldownMillis = 3000;
    public long bigBeepCooldownMillis = 5000;

    public static BeepConfig load(Path dataDirectory, Gson gson) {
        Path configFile = dataDirectory.resolve("config.json");
        if (!Files.exists(configFile)) {
            try {
                if (!Files.exists(dataDirectory)) Files.createDirectories(dataDirectory);
                BeepConfig defaultConfig = new BeepConfig();
                try (Writer writer = Files.newBufferedWriter(configFile)) {
                    new GsonBuilder().setPrettyPrinting().create().toJson(defaultConfig, writer);
                }
                return defaultConfig;
            } catch (IOException e) {
                return new BeepConfig();
            }
        }

        try (Reader reader = Files.newBufferedReader(configFile)) {
            return gson.fromJson(reader, BeepConfig.class);
        } catch (IOException e) {
            return new BeepConfig();
        }
    }
}