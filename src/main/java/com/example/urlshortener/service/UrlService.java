package com.example.urlshortener.service;

import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.Url;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.utility.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UrlService {
    private static final Logger logger = LoggerFactory.getLogger(UrlService.class);
    private static final String CACHE_PREFIX = "short_url:";
    private static final long CACHE_EXPIRY_HOURS = 24;

    private final UrlRepository urlRepository;
    private final HashUtil hashUtil;
    // Redis is optional - will be null if not configured
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Value("${app.short-url.base-url:http://localhost:8080}")
    private String baseUrl;

    public UrlService(UrlRepository urlRepository, HashUtil hashUtil) {
        this.urlRepository = urlRepository;
        this.hashUtil = hashUtil;
    }

    /**
     * Create a short URL from a long URL
     * Implements deduplication: returns existing shortCode if longUrl already exists
     * Implements collision handling: tries alternative codes if collision occurs
     *
     * @param longUrl the original long URL
     * @return the generated short code
     * @throws InvalidUrlException if the URL format is invalid
     */
    @Transactional
    public String shortenUrl(String longUrl) {
        logger.info("Processing URL shortening request for: {}", longUrl);

        // Validate URL
        if (!isValidUrl(longUrl)) {
            logger.error("Invalid URL format: {}", longUrl);
            throw new InvalidUrlException("Invalid URL format. URL must start with http:// or https://");
        }

        // Check for deduplication: if this URL already exists, return its shortCode
        Optional<Url> existingUrl = urlRepository.findByLongUrl(longUrl);
        if (existingUrl.isPresent()) {
            logger.info("URL already shortened. Returning existing shortCode: {}", existingUrl.get().getShortCode());
            return existingUrl.get().getShortCode();
        }

        // Generate short code with collision handling
        String shortCode = generateUniqueShortCode(longUrl);
        logger.debug("Generated unique shortCode: {}", shortCode);

        // Save to database
        Url url = new Url();
        url.setLongUrl(longUrl);
        url.setShortCode(shortCode);
        urlRepository.save(url);

        // Cache the mapping
        cacheUrl(shortCode, longUrl);

        logger.info("Successfully shortened URL. ShortCode: {}, LongUrl: {}", shortCode, longUrl);
        return shortCode;
    }

    /**
     * Retrieve the original long URL from a short code
     * Uses cache-first approach: checks Redis before database
     *
     * @param shortCode the short code
     * @return the original long URL
     * @throws UrlNotFoundException if the short code doesn't exist
     */
    public String getOriginalUrl(String shortCode) {
        logger.info("Retrieving original URL for shortCode: {}", shortCode);

        // Check cache first
        String cachedUrl = getCachedUrl(shortCode);
        if (cachedUrl != null) {
            logger.debug("Found URL in cache for shortCode: {}", shortCode);
            return cachedUrl;
        }

        // Check database
        Optional<Url> url = urlRepository.findByShortCode(shortCode);
        if (url.isEmpty()) {
            logger.warn("Short code not found: {}", shortCode);
            throw new UrlNotFoundException("Short code not found: " + shortCode);
        }

        String longUrl = url.get().getLongUrl();
        // Cache the result for future requests
        cacheUrl(shortCode, longUrl);

        logger.info("Retrieved original URL for shortCode: {}", shortCode);
        return longUrl;
    }

    /**
     * Generate a unique short code with collision handling
     * Algorithm:
     * 1. Generate initial short code from SHA-1 hash
     * 2. If collision detected, try alternative codes (shift substring)
     * 3. If max attempts reached, use timestamp-based rehashing
     *
     * @param longUrl the original long URL
     * @return a unique short code
     */
    private String generateUniqueShortCode(String longUrl) {
        String shortCode = hashUtil.generateShortCode(longUrl);

        // Check for collision
        if (urlRepository.findByShortCode(shortCode).isPresent()) {
            logger.debug("Collision detected for shortCode: {}. Attempting alternatives...", shortCode);

            // Try alternative codes
            for (int attempt = 1; attempt <= 5; attempt++) {
                String alternativeCode = hashUtil.generateAlternativeShortCode(longUrl, attempt);
                if (urlRepository.findByShortCode(alternativeCode).isEmpty()) {
                    logger.debug("Found unique alternative shortCode: {} (attempt {})", alternativeCode, attempt);
                    return alternativeCode;
                }
                logger.debug("Collision still detected for alternative code: {}. Trying next...", alternativeCode);
            }

            // If still collisions, use timestamp-based hash
            shortCode = hashUtil.generateTimestampHash();
            if (urlRepository.findByShortCode(shortCode).isEmpty()) {
                logger.debug("Using timestamp-based shortCode: {}", shortCode);
                return shortCode;
            }

            // Fallback: keep trying with incremented timestamp
            while (urlRepository.findByShortCode(shortCode).isPresent()) {
                shortCode = hashUtil.generateTimestampHash();
            }
        }

        return shortCode;
    }

    /**
     * Validate URL format
     *
     * @param url the URL to validate
     * @return true if URL is valid, false otherwise
     */
    private boolean isValidUrl(String url) {
        return url != null && !url.isBlank() && (url.startsWith("http://") || url.startsWith("https://"));
    }

    /**
     * Cache the URL mapping in Redis
     *
     * @param shortCode the short code
     * @param longUrl the original long URL
     */
    private void cacheUrl(String shortCode, String longUrl) {
        if (redisTemplate == null) {
            logger.debug("Redis not available, skipping cache for: {}", shortCode);
            return;
        }
        try {
            String cacheKey = CACHE_PREFIX + shortCode;
            redisTemplate.opsForValue().set(cacheKey, longUrl, CACHE_EXPIRY_HOURS, TimeUnit.HOURS);
            logger.debug("Cached URL mapping: {} -> {}", shortCode, longUrl);
        } catch (Exception e) {
            logger.error("Failed to cache URL mapping", e);
            // Continue without caching - not critical for functionality
        }
    }

    /**
     * Retrieve cached URL from Redis
     *
     * @param shortCode the short code
     * @return the cached long URL, or null if not found
     */
    private String getCachedUrl(String shortCode) {
        if (redisTemplate == null) {
            logger.debug("Redis not available, skipping cache lookup for: {}", shortCode);
            return null;
        }
        try {
            String cacheKey = CACHE_PREFIX + shortCode;
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            logger.error("Failed to retrieve URL from cache", e);
            // Continue to database lookup
            return null;
        }
    }

    /**
     * Build the full short URL with base URL
     *
     * @param shortCode the short code
     * @return the complete short URL
     */
    public String buildShortUrl(String shortCode) {
        return baseUrl + "/" + shortCode;
    }
}
