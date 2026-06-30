package com.aparaitech.cet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.aparaitech.cet.config.InstituteProperties;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@EnableConfigurationProperties(InstituteProperties.class)
public class CetApplication {

    public static void main(String[] args) {
        ensureRuntimeDirectories();
        SpringApplication.run(CetApplication.class, args);
    }

    /**
     * Pre-creates the directories the app needs at startup, based on the SAME
     * env vars used in application.properties (DB_PATH, UPLOAD_DIR, PAGES_DIR).
     * Without this, SQLite/PDFBox throw "directory does not exist" errors the
     * first time they try to write — especially on platforms like Render where
     * the working directory may not match local dev defaults.
     */
    private static void ensureRuntimeDirectories() {
        // Parent directory of the SQLite DB file, e.g. DB_PATH=/app/data/cetportal.db -> /app/data
        String dbPath = System.getenv().getOrDefault("DB_PATH", "./data/cetportal.db");
        File dbParent = Paths.get(dbPath).toAbsolutePath().getParent().toFile();
        dbParent.mkdirs();

        // Upload + page-image directories
        String uploadDir = System.getenv().getOrDefault(
            "UPLOAD_DIR", "./src/main/resources/static/uploads");
        String pagesDir = System.getenv().getOrDefault(
            "PAGES_DIR", "./src/main/resources/static/uploads/pages");

        new File(uploadDir).mkdirs();
        new File(pagesDir).mkdirs();
    }
}
