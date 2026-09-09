package com.hotel.hotel.modules.files.service;

import com.hotel.hotel.config.exceptions.MyCustomStorageException;
import com.hotel.hotel.config.exceptions.ResourceNotFoundException;
import com.hotel.hotel.modules.audit.Auditable;
import com.hotel.hotel.modules.files.dto.FileResponse;
import com.hotel.hotel.modules.files.model.File;
import com.hotel.hotel.modules.files.repository.FileRepository;
import com.hotel.hotel.modules.room.model.Room;
import com.hotel.hotel.modules.user.model.User;
import io.minio.*;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileService {

    @Autowired
    private FileRepository repository;

    @Autowired
    private MinioClient minioClient;
    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    @Auditable(action = "FILE_UPDATE", resourceType = "FILE")
    public String uploadFile(MultipartFile fileData, Room room, User user) {
        try {
            log.info("Start process to upload file: {}", fileData.getOriginalFilename());
            String objectName = UUID.randomUUID().toString();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(fileData.getInputStream(), fileData.getSize(), -1)
                            .contentType(fileData.getContentType())
                            .build()
            );

            File file = new File(objectName, fileData, null, room, user);
            repository.save(file);
            log.info("File {} successfully created", fileData.getOriginalFilename());
            return objectName;
        } catch (ErrorResponseException e) {
            throw new MyCustomStorageException(e.getMessage());
        } catch (IOException e) {
            throw new MyCustomStorageException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during upload", e);
        }
    }

    @Transactional
    public void syncRoomImages(Long roomId, List<String> remainingImages, List<MultipartFile> newImages, Room room) {
        List<File> currentFiles = repository.findByRoomId(roomId);
        Set<String> keysToKeep = (remainingImages != null)
                ? remainingImages.stream().collect(Collectors.toSet())
                : Set.of();

        currentFiles.stream()
                .filter(file -> !keysToKeep.contains(file.getMinioKey()))
                .forEach(file -> {
                    this.deleteFromMinio(file.getMinioKey());
                    repository.delete(file);
                });

        if (newImages != null && !newImages.isEmpty()) {
            this.uploadMultipleFilesForRoom(newImages, room);
        }
    }

    @Transactional
    public void uploadMultipleFilesForRoom(List<MultipartFile> files, Room room) {
        if (files == null) return;
        for (MultipartFile file : files) {
            this.uploadFile(file, room, null);
        }
    }

    public List<String> listImagesByRoom(Long id) {
        log.info("Listing image keys for room ID: {}", id);
        return repository.findByRoomId(id).stream()
                .map(File::getMinioKey)
                .collect(Collectors.toList());
    }

   public void deleteFromMinio(String objectName) {
        try {
            log.info("Deleting object from MinIO: {}", objectName);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to delete object from MinIO: {}", objectName, e);
            throw new MyCustomStorageException("Failed to remove file from storage: " + e.getMessage());
        }
    }

    @Auditable(action = "FILE_DELETE", resourceType = "FILE")
    public void deleteById(Long id) {
        log.info("Start process to delete file with id: {}", id);
        File file = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("File does not exists"));
        repository.delete(file);
        log.info("File with ID: {} successfully deleted", id);
    }

    public FileResponse findById(Long id) {
        File file = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File does not exist"));
        return new FileResponse(file, buildPublicUrl(file.getMinioKey()));
    }

    public FileResponse findByMinioKey(String minioKey) {
        return repository.findByMinioKey(minioKey)
                .map(file -> new FileResponse(file, buildPublicUrl(file.getMinioKey())))
                .orElse(null);
    }

    private String buildPublicUrl(String objectName) {
        return String.format("%s/%s/%s", minioUrl, bucketName, objectName);
    }

}
