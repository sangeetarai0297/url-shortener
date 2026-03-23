package com.example.urlshortener.controller;

import com.example.urlshortener.exception.ErrorResponse;
import com.example.urlshortener.model.ShortenUrlRequest;
import com.example.urlshortener.model.ShortenUrlResponse;
import com.example.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "APIs for creating and redirecting short URLs")
public class UrlController {
    private static final Logger logger = LoggerFactory.getLogger(UrlController.class);

    private final UrlService urlService;

    /**
     * Endpoint to create a short URL
     * POST /shorten
     * Request: { "longUrl": "https://example.com" }
     * Response: { "shortUrl": "http://localhost:8080/abc123", "longUrl": "...", "shortCode": "abc123" }
     *
     * @param request the ShortenUrlRequest containing the long URL
     * @return ResponseEntity with the short URL details
     */
    @PostMapping("/shorten")
    @Operation(
            summary = "Create a short URL",
            description = "Creates a short URL from a long URL. If the same URL is submitted again, " +
                    "the same short code is returned (deduplication). The short code is generated using SHA-1 hashing."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Short URL created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ShortenUrlResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid URL format or missing required field"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public ResponseEntity<ShortenUrlResponse> shortenUrl(@Valid @RequestBody ShortenUrlRequest request) {
        logger.info("Received request to shorten URL: {}", request.getLongUrl());

        // Generate short code
        String shortCode = urlService.shortenUrl(request.getLongUrl());

        // Build response
        String shortUrl = urlService.buildShortUrl(shortCode);
        ShortenUrlResponse response = new ShortenUrlResponse(
                shortUrl,
                request.getLongUrl(),
                shortCode
        );

        logger.info("Successfully created short URL: {}", shortUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to redirect to the original URL
     * GET /{shortCode}
     * Returns HTTP 302 redirect to the original long URL
     *
     * @param shortCode the short code
     * @return RedirectView to the original URL
     */
    @GetMapping("/{shortCode}")
    @Operation(
            summary = "Redirect to original URL",
            description = "Retrieves the original long URL associated with a short code and performs a redirect (HTTP 302). " +
                    "This endpoint is used when users click on shortened URLs. " +
                    "**Important:** This endpoint requires a valid short code that exists in the database. " +
                    "To test this endpoint:" +
                    "1. First use POST /shorten to create a short URL" +
                    "2. Copy the 'shortCode' from the response (e.g., 'abc1234')" +
                    "3. Replace {shortCode} in the URL with the actual short code" +
                    "4. The endpoint will redirect to the original URL with HTTP 302 status" +
                    "Example: GET /abc1234 → redirects to https://www.example.com",
            parameters = {
                    @Parameter(
                            name = "shortCode",
                            description = "The 7-character short code obtained from POST /shorten response",
                            required = true,
                            example = "abc1234",
                            schema = @Schema(type = "string", minLength = 7, maxLength = 7, pattern = "^[a-zA-Z0-9]{7}$")
                    )
            }
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "302",
                    description = "Found - Redirect to the original URL",
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = "Location",
                            description = "The original long URL to redirect to",
                            schema = @Schema(type = "string", example = "https://www.example.com")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Short code not found - The provided short code does not exist in the database",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = "{\"timestamp\":\"2026-03-23T12:00:00\",\"status\":404,\"error\":\"URL not found\",\"message\":\"Short code not found: invalid123\",\"path\":\"uri=/invalid123\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public RedirectView redirectToOriginalUrl(@PathVariable String shortCode) {
        logger.info("Received redirect request for shortCode: {}", shortCode);

        // Get the original URL
        String originalUrl = urlService.getOriginalUrl(shortCode);

        logger.info("Redirecting shortCode: {} to original URL", shortCode);
        // Return a 302 redirect
        RedirectView redirectView = new RedirectView(originalUrl);
        redirectView.setStatusCode(HttpStatus.FOUND); // HTTP 302
        return redirectView;
    }

    /**
     * Health check endpoint
     * GET /health
     *
     * @return a simple health status
     */
    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Simple health check endpoint to verify the application is running. " +
                    "Can be used by load balancers and monitoring systems."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Service is running"
    )
    public ResponseEntity<String> health() {
        logger.debug("Health check endpoint called");
        return ResponseEntity.ok("URL Shortener service is running");
    }
}
