package eu.lordbucket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YtProxy {
    public static void main(String[] args) {
        SpringApplication.run(YtProxy.class, args);
    }
}