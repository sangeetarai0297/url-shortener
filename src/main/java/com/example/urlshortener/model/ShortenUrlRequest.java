package com.example.urlshortener.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model to shorten a URL")
public class ShortenUrlRequest {
    @NotBlank(message = "Long URL cannot be blank")
    @Pattern(
            regexp = "^https?://.*",
            message = "URL must start with http:// or https://"
    )
    @Schema(
            description = "The long URL to be shortened",
            example = "https://www.github.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String longUrl;
}

