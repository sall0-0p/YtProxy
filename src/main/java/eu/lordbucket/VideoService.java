package eu.lordbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);
    private static final String STORAGE_DIR = "/app/downloads";
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public File getOrDownloadVideo(String fullUrl) throws IOException, InterruptedException {
        File directory = new File(STORAGE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileId = String.valueOf(Math.abs(fullUrl.hashCode()));
        String fileName = fileId + ".mp4";
        File file = new File(directory, fileName);

        if (file.exists()) {
            return file;
        }

        ReentrantLock lock = locks.computeIfAbsent(fileId, k -> new ReentrantLock());
        lock.lock();
        try {
            if (file.exists()) {
                return file;
            }
            performDownload(fullUrl, file.getAbsolutePath());
            return file;
        } finally {
            lock.unlock();
        }
    }

    private void performDownload(String url, String outputPath) throws IOException, InterruptedException {
        log.info("Starting download for URL: {}", url);

        ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp",
                "--newline", // Forces progress to print on new lines so Java captures it
                "-f", "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/best[height<=720][ext=mp4]/best[ext=mp4]",
                "-o", outputPath,
                url
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Log only lines containing progress info or warnings to keep it clean but visible
                if (line.contains("[download]") || line.contains("ERROR") || line.contains("WARNING")) {
                    log.info("yt-dlp: {}", line);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp failed with exit code " + exitCode);
        }

        log.info("Download completed successfully: {}", outputPath);
    }
}