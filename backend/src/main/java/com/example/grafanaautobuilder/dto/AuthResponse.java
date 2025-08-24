package com.example.grafanaautobuilder.dto;

import java.time.OffsetDateTime;

public record AuthResponse(
        String token,
        OffsetDateTime expiresAt
) {
}