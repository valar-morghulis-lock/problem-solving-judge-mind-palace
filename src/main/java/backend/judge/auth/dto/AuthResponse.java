package backend.judge.auth.dto;


import java.util.UUID;

public record AuthResponse(
        UUID id,
        String username,
        String email,
        String role,
        String token,
        String refreshToken
) {}