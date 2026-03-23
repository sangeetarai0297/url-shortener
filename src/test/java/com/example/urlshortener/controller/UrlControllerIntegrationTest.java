package com.example.urlshortener.controller;

import com.example.urlshortener.model.ShortenUrlRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Disabled("Requires Redis to be running. Enable when Redis is available.")
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("URL Shortener service is running"));
    }

    @Test
    void testShortenUrlSuccess() throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest("https://www.example.com");

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl", notNullValue()))
                .andExpect(jsonPath("$.shortUrl", startsWith("http://localhost:8080/")))
                .andExpect(jsonPath("$.longUrl").value("https://www.example.com"))
                .andExpect(jsonPath("$.shortCode", notNullValue()));
    }

    @Test
    void testShortenUrlInvalid() throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest("not-a-url");

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid URL"));
    }

    @Test
    void testShortenUrlBlank() throws Exception {
        ShortenUrlRequest request = new ShortenUrlRequest("");

        mockMvc.perform(post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testRedirectNotFound() throws Exception {
        mockMvc.perform(get("/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("URL not found"));
    }
}

