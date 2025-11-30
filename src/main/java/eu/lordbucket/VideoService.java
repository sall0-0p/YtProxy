package eu.lordbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoService.class);
    private static final String STORAGE_DIR = "/app/downloads";

    // Tracks which files are currently being downloaded to prevent duplicates
    private final ConcurrentHashMap<String, Boolean> activeDownloads = new ConcurrentHashMap<>();

    // Limits concurrent downloads to 4 to save system resources
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * returns the File object if it exists on disk, otherwise returns null.
     */
    public File getFileIfReady(String fullUrl) {
        String fileName = getFileName(fullUrl);
        File file = new File(STORAGE_DIR, fileName);

        if (file.exists() && file.length() > 0) {
            return file;
        }
        return null;
    }

    /**
     * Checks if a download is running. If not, starts it asynchronously.
     */
    public void startDownloadAsync(String fullUrl) {
        String fileName = getFileName(fullUrl);
        String fileId = fileName; // using filename as ID

        // If already downloading, do nothing
        if (activeDownloads.containsKey(fileId)) {
            return;
        }

        // Mark as active
        activeDownloads.put(fileId, true);

        // Submit task to background thread pool
        executor.submit(() -> {
            try {
                performDownload(fullUrl, new File(STORAGE_DIR, fileName).getAbsolutePath());
            } catch (Exception e) {
                log.error("Download failed for {}", fullUrl, e);
            } finally {
                // Remove from active list when done (success or failure)
                activeDownloads.remove(fileId);
            }
        });
    }

    private String getFileName(String fullUrl) {
        // Ensures directory exists
        new File(STORAGE_DIR).mkdirs();
        String fileId = String.valueOf(Math.abs(fullUrl.hashCode()));
        return fileId + ".mp4";
    }

    private void performDownload(String url, String outputPath) throws IOException, InterruptedException {
        log.info("Background download started: {}", url);

        ProcessBuilder builder = new ProcessBuilder(
                "yt-dlp",
                "--newline",
                "--no-playlist", // Prevents downloading the whole list if URL has &list=...
                "-f", "bestvideo[height<=720][ext=mp4]+bestaudio[ext=m4a]/best[height<=720][ext=mp4]/best[ext=mp4]",
                "-o", outputPath,
                url
        );

        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("[download]") || line.contains("ERROR") || line.contains("WARNING")) {
                    log.info("yt-dlp: {}", line);
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            new File(outputPath).delete();
            throw new IOException("yt-dlp failed with exit code " + exitCode);
        }

        log.info("Background download finished: {}", outputPath);
    }
}