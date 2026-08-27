package com.vedaai.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class VedaAiApplication {

    static {
        // Automatically load .env file into System properties if present
        try {
            Path envPath = Paths.get(".env");
            if (Files.exists(envPath)) {
                Files.lines(envPath).forEach(line -> {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                        int idx = trimmed.indexOf('=');
                        String key = trimmed.substring(0, idx).trim();
                        String val = trimmed.substring(idx + 1).trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                        }
                    }
                });
            }
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(VedaAiApplication.class, args);
    }
}
