package com.example.urlshortener.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response model containing the shortened URL details")
public class ShortenUrlResponse {
    @Schema(
            description = "The complete short URL that can be used for redirection",
            example = "http://localhost:8080/abc1234"
    )
    private String shortUrl;

    @Schema(
            description = "The original long URL that was shortened",
            example = "https://www.github.com"
    )
    private String longUrl;

    @Schema(
            description = "The unique short code used for redirection",
            example = "abc1234"
    )
    private String shortCode;
}

