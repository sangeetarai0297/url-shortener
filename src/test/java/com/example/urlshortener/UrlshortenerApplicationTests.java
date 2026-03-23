package com.example.urlshortener;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Disabled("Requires Redis and PostgreSQL/H2 setup. Enable when services are configured.")
class UrlshortenerApplicationTests {

	@Test
	void contextLoads() {
	}

}
