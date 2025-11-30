package eu.lordbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);
    private static final String STORAGE_DIR = "/app/downloads";
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    // Downloads the video if it does not exist, then returns the file path
    public File getOrDownloadVideo(String fullUrl) throws IOException, InterruptedException {
        File directory = new File(STORAGE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generates a unique filename based on the URL hash to avoid collision
        String fileId = String.valueOf(Math.abs(fullUrl.hashCode()));
        String fileName = fileId + ".mp4";
        File file = new File(directory, fileName);

        if (file.exists()) {
            return file;
        }

        // Ensures only one thread downloads a specific video at a time
        ReentrantLock lock = locks.computeIfAbsent(fileId, k -> new ReentrantLock());
        lock.lock();
        try {
            // Double-check existence after acquiring lock
            if (file.exists()) {
                return file;
            }
            performDownload(fullUrl, file.getAbsolutePath());
            return file;
        } finally {
            lock.unlock();
        }
    }

    // Executes yt-dlp as a subprocess
    private void performDownload(String url, String outputPath) throws IOException, InterruptedException {
        log.info("Starting download for URL: {}", url);

        // Arguments force mp4 format for compatibility with Minecraft mods
        ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp",
                "-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
                "-o", outputPath,
                url
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("yt-dlp: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp failed with exit code " + exitCode);
        }

        log.info("Download completed: {}", outputPath);
    }
}