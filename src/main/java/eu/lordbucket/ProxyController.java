package eu.lordbucket;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
public class ProxyController {

    private final VideoService videoService;

    public ProxyController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/**")
    public ResponseEntity<Resource> proxyVideo(HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String targetUrl = "https://www.youtube.com" + path;
        if (query != null) {
            targetUrl += "?" + query;
        }

        // Check if file is ready
        File videoFile = videoService.getFileIfReady(targetUrl);

        if (videoFile != null) {
            // Updates timestamp for cleanup service
            videoFile.setLastModified(System.currentTimeMillis());

            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("video/mp4"))
                    .body(new FileSystemResource(videoFile));
        } else {
            // File not ready: trigger background download and return 202
            videoService.startDownloadAsync(targetUrl);

            // Returns 202 Accepted.
            // This tells the client: "Request accepted, processing, come back later."
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
    }
}