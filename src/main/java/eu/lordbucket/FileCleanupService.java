package eu.lordbucket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Service
public class FileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);
    private static final String STORAGE_DIR = "/app/downloads";
    private static final long EXPIRATION_TIME_MS = TimeUnit.HOURS.toMillis(24);

    // Runs every hour to clean up old files
    @Scheduled(fixedRate = 3600000)
    public void cleanupOldFiles() {
        File directory = new File(STORAGE_DIR);
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) return;

        long now = System.currentTimeMillis();

        for (File file : files) {
            long diff = now - file.lastModified();
            if (diff > EXPIRATION_TIME_MS) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("Deleted expired file: {}", file.getName());
                } else {
                    log.error("Failed to delete file: {}", file.getName());
                }
            }
        }
    }
}
