package vn.edu.demo.userservice.storage;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MinioBucketInitializer {
    private final MinioStorageService minioStorage;

    public MinioBucketInitializer(MinioStorageService minioStorage) {
        this.minioStorage = minioStorage;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        minioStorage.initializeBucket();
    }
}
