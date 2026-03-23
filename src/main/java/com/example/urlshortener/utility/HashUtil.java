package com.example.urlshortener.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class HashUtil {
    private static final Logger logger = LoggerFactory.getLogger(HashUtil.class);
    private static final int SHORT_CODE_LENGTH = 7;
    private static final int MAX_COLLISION_ATTEMPTS = 5;

    /**
     * Generate a short code from a long URL using SHA-1 hashing
     * Implements collision handling by shifting the substring and rehashing with timestamp if needed
     *
     * @param longUrl the original long URL
     * @return a 7-character short code
     */
    public String generateShortCode(String longUrl) {
        try {
            // Generate initial hash
            String hash = hashSha1(longUrl);
            logger.debug("Generated SHA-1 hash for URL: {}", longUrl);
            return hash.substring(0, SHORT_CODE_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-1 algorithm not available, falling back to timestamp hash", e);
            return generateTimestampHash();
        }
    }

    /**
     * Generate an alternative short code by shifting the substring
     * Used for collision handling
     *
     * @param longUrl the original long URL
     * @param attemptNumber the attempt number (1-5)
     * @return a 7-character short code
     */
    public String generateAlternativeShortCode(String longUrl, int attemptNumber) {
        if (attemptNumber > MAX_COLLISION_ATTEMPTS) {
            logger.warn("Max collision attempts reached, generating timestamp-based hash");
            return generateTimestampHash();
        }

        try {
            String hash = hashSha1(longUrl);
            int startIndex = attemptNumber;

            if (startIndex + SHORT_CODE_LENGTH <= hash.length()) {
                String alternativeCode = hash.substring(startIndex, startIndex + SHORT_CODE_LENGTH);
                logger.debug("Generated alternative short code (attempt {}) for URL: {}", attemptNumber, longUrl);
                return alternativeCode;
            } else {
                // If we can't shift enough, rehash with timestamp
                return generateTimestampHash();
            }
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-1 algorithm not available", e);
            return generateTimestampHash();
        }
    }

    /**
     * Generate a hash based on the current timestamp as a fallback
     *
     * @return a 7-character short code
     */
    public String generateTimestampHash() {
        try {
            long timestamp = System.currentTimeMillis();
            String hash = hashSha1(String.valueOf(timestamp));
            logger.debug("Generated timestamp-based hash");
            return hash.substring(0, SHORT_CODE_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to generate timestamp hash", e);
            // Fallback: use base64 encoding of timestamp
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(String.valueOf(System.currentTimeMillis()).getBytes())
                    .substring(0, Math.min(7, 7));
        }
    }

    /**
     * Compute SHA-1 hash of the input string
     *
     * @param input the input string to hash
     * @return the hexadecimal representation of the SHA-1 hash
     * @throws NoSuchAlgorithmException if SHA-1 algorithm is not available
     */
    private String hashSha1(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(input.getBytes());
        StringBuilder hexString = new StringBuilder();

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }
}

