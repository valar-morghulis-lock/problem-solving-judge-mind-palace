package backend.judge.auth.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        @Size(max = 2048, message = "Invalid token")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$",
                message = "Invalid token format"
        )
        String refreshToken
) {}