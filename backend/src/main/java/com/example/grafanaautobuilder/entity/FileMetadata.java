package com.example.grafanaautobuilder.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // optional: null if unauthenticated

    @Column(nullable = false)
    private String bucket;

    @Column(name = "object_path", nullable = false, length = 512)
    private String objectPath;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(nullable = false)
    private long size;

    @Column(length = 128)
    private String checksum;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
