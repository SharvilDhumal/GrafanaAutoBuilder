package com.example.grafanaautobuilder.dto;

import com.example.grafanaautobuilder.entity.FileMetadata;
import lombok.Data;

import java.time.Instant;

@Data
public class FileMetadataDto {
    private Long id;
    private String bucket;
    private String objectPath;
    private String originalName;
    private long size;
    private String checksum;
    private Instant createdAt;
    private String signedUrl; // optional

    public FileMetadataDto() {}

    public FileMetadataDto(FileMetadata e) {
        this.id = e.getId();
        this.bucket = e.getBucket();
        this.objectPath = e.getObjectPath();
        this.originalName = e.getOriginalName();
        this.size = e.getSize();
        this.checksum = e.getChecksum();
        this.createdAt = e.getCreatedAt();
    }
}
