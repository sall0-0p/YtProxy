package eu.lordbucket;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

@RestController
public class ProxyController {

    private final VideoService videoService;

    public ProxyController(VideoService videoService) {
        this.videoService = videoService;
    }

    // Captures all paths (/**) to handle /watch?v=... or /shorts/...
    @GetMapping("/**")
    public ResponseEntity<Resource> proxyVideo(HttpServletRequest request) {
        try {
            // Reconstructs the target YouTube URL
            String path = request.getRequestURI();
            String query = request.getQueryString();
            String targetUrl = "https://www.youtube.com" + path;
            if (query != null) {
                targetUrl += "?" + query;
            }

            // Retrieves the file, blocking until download completes
            File videoFile = videoService.getOrDownloadVideo(targetUrl);

            // Updates last modified time to reset the 24h deletion timer
            videoFile.setLastModified(System.currentTimeMillis());

            Resource fileResource = new FileSystemResource(videoFile);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // Use octet-stream or video/mp4
                    .body(fileResource);

        } catch (IOException | InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
