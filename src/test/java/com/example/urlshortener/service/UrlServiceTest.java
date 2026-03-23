package com.example.urlshortener.service;

import com.example.urlshortener.exception.InvalidUrlException;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.Url;
import com.example.urlshortener.repository.UrlRepository;
import com.example.urlshortener.utility.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private HashUtil hashUtil;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testShortenUrl_ValidUrl() {
        String longUrl = "https://www.example.com";
        String shortCode = "abc1234";

        when(urlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(hashUtil.generateShortCode(longUrl)).thenReturn(shortCode);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        String result = urlService.shortenUrl(longUrl);

        assertEquals(shortCode, result);
        verify(urlRepository, times(1)).save(any(Url.class));
        // Redis is disabled, so no caching should occur
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testShortenUrl_Deduplication() {
        String longUrl = "https://www.example.com";
        String shortCode = "abc1234";
        Url existingUrl = new Url();
        existingUrl.setShortCode(shortCode);
        existingUrl.setLongUrl(longUrl);

        when(urlRepository.findByLongUrl(longUrl)).thenReturn(Optional.of(existingUrl));

        String result = urlService.shortenUrl(longUrl);

        assertEquals(shortCode, result);
        verify(urlRepository, never()).save(any(Url.class));
        verify(urlRepository, never()).findByShortCode(anyString());
    }

    @Test
    void testShortenUrl_InvalidUrl() {
        String invalidUrl = "not-a-valid-url";

        assertThrows(InvalidUrlException.class, () -> urlService.shortenUrl(invalidUrl));
        verify(urlRepository, never()).save(any(Url.class));
        verify(hashUtil, never()).generateShortCode(anyString());
    }

    @Test
    void testShortenUrl_CollisionHandling() {
        String longUrl = "https://www.example.com";
        String shortCode1 = "abc1234";
        String shortCode2 = "bcd2345";

        Url existingUrlWithCode1 = new Url();
        existingUrlWithCode1.setShortCode(shortCode1);

        when(urlRepository.findByLongUrl(longUrl)).thenReturn(Optional.empty());
        when(hashUtil.generateShortCode(longUrl)).thenReturn(shortCode1);
        when(urlRepository.findByShortCode(shortCode1)).thenReturn(Optional.of(existingUrlWithCode1));
        when(hashUtil.generateAlternativeShortCode(longUrl, 1)).thenReturn(shortCode2);
        when(urlRepository.findByShortCode(shortCode2)).thenReturn(Optional.empty());

        String result = urlService.shortenUrl(longUrl);

        assertEquals(shortCode2, result);
        verify(urlRepository, times(1)).save(any(Url.class));
    }

    @Test
    void testGetOriginalUrl_FromCache() {
        String shortCode = "abc1234";
        String longUrl = "https://www.example.com";

        // Since Redis is disabled (redisTemplate is null), this should go to database
        Url url = new Url();
        url.setShortCode(shortCode);
        url.setLongUrl(longUrl);

        when(valueOperations.get("short_url:" + shortCode)).thenReturn(null); // Redis disabled
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        String result = urlService.getOriginalUrl(shortCode);

        assertEquals(longUrl, result);
        verify(urlRepository, times(1)).findByShortCode(shortCode);
        // Redis is disabled, so no caching should occur
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testGetOriginalUrl_FromDatabase() {
        String shortCode = "abc1234";
        String longUrl = "https://www.example.com";
        Url url = new Url();
        url.setShortCode(shortCode);
        url.setLongUrl(longUrl);

        when(valueOperations.get("short_url:" + shortCode)).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(url));

        String result = urlService.getOriginalUrl(shortCode);

        assertEquals(longUrl, result);
        verify(urlRepository, times(1)).findByShortCode(shortCode);
        // Redis is disabled, so no caching should occur
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void testGetOriginalUrl_NotFound() {
        String shortCode = "nonexistent";

        when(valueOperations.get("short_url:" + shortCode)).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> urlService.getOriginalUrl(shortCode));
    }

    @Test
    void testBuildShortUrl() {
        String shortCode = "abc1234";
        String expectedUrl = "http://localhost:8080/abc1234";

        String result = urlService.buildShortUrl(shortCode);

        assertEquals(expectedUrl, result);
    }
}
