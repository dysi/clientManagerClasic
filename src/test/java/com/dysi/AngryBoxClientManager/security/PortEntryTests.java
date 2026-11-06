package com.dysi.AngryBoxClientManager.security;

import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.context-path=/entry-test")
@ActiveProfiles("test")
class PortEntryTests {
    @LocalServerPort int port;

    private HttpURLConnection connection(String path, String method) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + path).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        return connection;
    }

    @Test void barePortRedirectsToConfiguredContextForGetAndHead() throws Exception {
        for (String method : new String[] {"GET", "HEAD"}) {
            HttpURLConnection connection = connection("/", method);
            try {
                assertEquals(302, connection.getResponseCode());
                assertEquals("/entry-test/", new URL(new URL("http://localhost:" + port), connection.getHeaderField("Location")).getPath());
            } finally { connection.disconnect(); }
        }
    }

    @Test void contextEntryLeadsToSpaAndDataStillRequiresLogin() throws Exception {
        HttpURLConnection entry = connection("/entry-test/", "GET");
        try {
            assertEquals(302, entry.getResponseCode());
            assertTrue(entry.getHeaderField("Location").endsWith("/entry-test/ui/"));
        } finally { entry.disconnect(); }
        HttpURLConnection data = connection("/entry-test/web/api/clients", "GET");
        try { assertEquals(401, data.getResponseCode()); }
        finally { data.disconnect(); }
    }
}
