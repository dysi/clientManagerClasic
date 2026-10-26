package com.dysi.AngryBoxClientManager.security;

import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class WebFrontendTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired AuthorityRepository authorities;
    @Autowired ClientRepository clients;
    @Autowired PasswordEncoder passwords;
    @Autowired PaymentRepository payments;

    private JsonNode write(Browser browser, String method, String path, Object body, int expected) throws Exception {
        return json.readTree(mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path)
                .session(browser.session).header(browser.header, browser.token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString());
    }

    private long newClient(Browser browser, String name) throws Exception {
        return write(browser, "POST", "/web/api/clients", java.util.Map.of("firstname", name,
                "lastname", "Prueba", "email", "test@example.test", "phone", "1234", "cuil", ""), 201).get("id").asLong();
    }

    private java.util.Map<String, Object> licenseInput(long clientId, long companyId) {
        return new java.util.HashMap<>(java.util.Map.of("clientId", clientId, "companyId", companyId,
                "companyName", "Empresa de prueba", "description", "Prueba", "initialActivity", "2026-10-01",
                "lockDate", "2099-12-31", "prePayment", true));
    }

    @Test void licensesValidateOwnershipUniquenessAndExplicitIdempotentLock() throws Exception {
        account(AuthorityName.ROLE_ADMIN); Browser browser = login();
        long client = newClient(browser, "Ana");
        var input = licenseInput(client, 111L);
        long id = write(browser, "POST", "/web/api/licenses", input, 201).get("id").asLong();
        write(browser, "POST", "/web/api/licenses", input, 409);
        long second = write(browser, "POST", "/web/api/licenses", licenseInput(client, 222L), 201).get("id").asLong();
        write(browser, "PUT", "/web/api/licenses/" + second, input, 409);
        input.put("clientId", newClient(browser, "Otro"));
        write(browser, "PUT", "/web/api/licenses/" + id, input, 400);
        for (int retry = 0; retry < 2; retry++) {
            assertTrue(write(browser, "PUT", "/web/api/licenses/" + id + "/lock", java.util.Map.of("locked", true), 200).get("locked").asBoolean());
        }
        mvc.perform(get("/web/api/licenses").session(browser.session).param("status", "BLOCKED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        assertFalse(write(browser, "PUT", "/web/api/licenses/" + id + "/lock", java.util.Map.of("locked", false), 200).get("locked").asBoolean());
        mvc.perform(get("/web/api/licenses").session(browser.session).param("status", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        input.put("clientId", client); input.put("companyName", "Actualizada");
        assertEquals("2099-12-31", write(browser, "PUT", "/web/api/licenses/" + id, input, 200).get("lockDate").asText());
    }

    @Test void paymentsPreserveDatesRejectInvalidAmountsAndKeepRemovalIdempotent() throws Exception {
        account(AuthorityName.ROLE_ADMIN); Browser browser = login();
        long license = write(browser, "POST", "/web/api/licenses", licenseInput(newClient(browser, "Ana"), 111L), 201).get("id").asLong();
        var input = new java.util.HashMap<String, Object>(java.util.Map.of("licenseId", license, "amount", 100.25,
                "discount", 10.10, "paymentDate", "2026-10-01", "expirationDate", "2026-10-31", "observation", "Prueba"));
        JsonNode created = write(browser, "POST", "/web/api/payments", input, 201);
        long id = created.get("id").asLong();
        assertEquals("2026-10-01", created.get("paymentDate").asText());
        assertEquals("2026-10-31", created.get("expirationDate").asText());
        assertTrue(created.get("paymentRealDate").isNull());
        input.put("discount", 200); write(browser, "PUT", "/web/api/payments/" + id, input, 400);
        input.put("discount", 10.10); input.put("paymentRealDate", "2026-10-24");
        assertEquals("2026-10-24", write(browser, "PUT", "/web/api/payments/" + id, input, 200).get("paymentRealDate").asText());
        write(browser, "PUT", "/web/api/payments/999999", input, 404);
        write(browser, "PUT", "/web/api/payments/" + id + "/status", java.util.Map.of("removed", true), 200);
        var removedAt = payments.findById(id).orElseThrow().getRemoveDate().getTimeInMillis();
        write(browser, "PUT", "/web/api/payments/" + id + "/status", java.util.Map.of("removed", true), 200);
        assertEquals(removedAt, payments.findById(id).orElseThrow().getRemoveDate().getTimeInMillis());
        write(browser, "PUT", "/web/api/payments/" + id, input, 409);
        mvc.perform(get("/web/api/payments").session(browser.session).param("status", "REMOVED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        assertFalse(write(browser, "PUT", "/web/api/payments/" + id + "/status", java.util.Map.of("removed", false), 200).get("removed").asBoolean());
        mvc.perform(get("/web/api/payments").session(browser.session).param("status", "PAID"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test void userProfilePreservesPasswordAndMaintainsBothSidesOfClientLink() throws Exception {
        User admin = account(AuthorityName.ROLE_ADMIN); Browser browser = login();
        Authority role = new Authority(); role.setName(AuthorityName.ROLE_USER); authorities.saveAndFlush(role);
        long client = newClient(browser, "Ana");
        var input = new java.util.HashMap<String, Object>(java.util.Map.of("username", "new-user", "firstname", "Nombre",
                "lastname", "Apellido", "email", "user@example.test", "enabled", true,
                "roles", List.of("ROLE_USER"), "clientId", client, "password", "NewPassword123!"));
        JsonNode created = write(browser, "POST", "/web/api/users", input, 201);
        assertFalse(created.has("password")); long id = created.get("id").asLong();
        String hash = users.findById(id).orElseThrow().getPassword();
        assertEquals(id, clients.findById(client).orElseThrow().getUser().getId());
        input.remove("password"); input.put("firstname", "Actualizado");
        write(browser, "PUT", "/web/api/users/" + id, input, 200);
        assertEquals(hash, users.findById(id).orElseThrow().getPassword());
        input.put("username", "another-user"); input.put("password", "NewPassword123!");
        write(browser, "POST", "/web/api/users", input, 409);
        input.put("username", "new-user"); input.remove("password"); input.put("clientId", null);
        write(browser, "PUT", "/web/api/users/" + id, input, 200);
        assertNull(clients.findById(client).orElseThrow().getUser());
        assertNull(users.findById(id).orElseThrow().getClient());
        input.put("username", admin.getUsername()); input.put("enabled", false); input.put("roles", List.of("ROLE_ADMIN"));
        write(browser, "PUT", "/web/api/users/" + admin.getId(), input, 409);
        mvc.perform(put("/web/api/users/" + admin.getId() + "/password").session(browser.session)
                .header(browser.header, browser.token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"ReplacedPassword123!\"}")).andExpect(status().isNoContent());
        assertTrue(passwords.matches("ReplacedPassword123!", users.findById(admin.getId()).orElseThrow().getPassword()));
        mvc.perform(get("/web/session").session(browser.session)).andExpect(status().isUnauthorized());
    }

    private User account(AuthorityName role) {
        Authority authority = new Authority(); authority.setName(role);
        authorities.saveAndFlush(authority);
        User user = new User(); user.setUsername("browser-admin");
        user.setFirstname("Test"); user.setLastname("Admin"); user.setEmail("browser@example.test");
        user.setPassword(passwords.encode("test-password")); user.setEnabled(true);
        user.setAuthorities(new java.util.ArrayList<>(List.of(authority)));
        return users.saveAndFlush(user);
    }

    private Browser login() throws Exception {
        MvcResult result = mvc.perform(get("/web/session/csrf")).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
        String oldId = session.getId();
        JsonNode csrf = json.readTree(result.getResponse().getContentAsString());
        mvc.perform(post("/web/session/login").session(session)
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"browser-admin\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").doesNotExist());
        assertNotEquals(oldId, session.getId(), "Login must rotate the session ID");
        JsonNode rotated = json.readTree(mvc.perform(get("/web/session/csrf").session(session))
                .andReturn().getResponse().getContentAsString());
        assertNotEquals(csrf.get("token").asText(), rotated.get("token").asText());
        return new Browser(session, rotated.get("headerName").asText(), rotated.get("token").asText());
    }

    @Test void anonymousCannotReadDataOrAdministrativeTemplates() throws Exception {
        mvc.perform(get("/web/api/clients")).andExpect(status().isUnauthorized());
        mvc.perform(get("/users/create")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/users/create/1")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/index/home")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/web/session/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test void clientRoundTripPreservesContactAndRejectsPartialEdits() throws Exception {
        account(AuthorityName.ROLE_ADMIN);
        Browser browser = login();
        String body = "{\"firstname\":\"Ana\",\"lastname\":\"Pérez\",\"email\":\"ana@example.test\",\"phone\":\"123456\",\"cuil\":\"20123456789\"}";
        MvcResult created = mvc.perform(post("/web/api/clients").session(browser.session)
                .header(browser.header, browser.token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(put("/web/api/clients/" + id).session(browser.session)
                .header(browser.header, browser.token).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("Pérez", "Gómez")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("123456"))
                .andExpect(jsonPath("$.email").value("ana@example.test")).andExpect(jsonPath("$.cuil").value("20123456789"));
        mvc.perform(get("/web/api/clients").session(browser.session).param("keyword", "Gómez"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(put("/web/api/clients/" + id).session(browser.session)
                .header(browser.header, browser.token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstname\":\"Ana\",\"lastname\":\"Gómez\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.email").exists());
        mvc.perform(get("/api/clients/getClientsLookups").session(browser.session))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].email").value("ana@example.test"));
        mvc.perform(put("/web/api/clients/" + id).session(browser.session)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(get("/web/api/clients").session(browser.session).param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/web/api/clients/999999").session(browser.session)).andExpect(status().isNotFound());
    }

    @Test void userRoleCannotAdministerAndDisabledAccountLosesSession() throws Exception {
        User user = account(AuthorityName.ROLE_USER);
        Browser browser = login();
        mvc.perform(get("/web/api/clients").session(browser.session)).andExpect(status().isForbidden());
        mvc.perform(get("/users/create").session(browser.session)).andExpect(status().isForbidden());
        user.setEnabled(false); users.saveAndFlush(user);
        mvc.perform(get("/web/session").session(browser.session)).andExpect(status().isUnauthorized());
    }

    @Test void logoutInvalidatesSessionAndRequiresCsrf() throws Exception {
        account(AuthorityName.ROLE_ADMIN);
        Browser browser = login();
        mvc.perform(get("/logout").session(browser.session)).andExpect(status().isUnauthorized());
        mvc.perform(get("/web/session").session(browser.session)).andExpect(status().isOk());
        mvc.perform(get("/api/payments/licenciaBloquear").session(browser.session).param("licenciaID", "1"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/payments/bajaPago").session(browser.session).param("pagoID", "1"))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/web/session/logout").session(browser.session)).andExpect(status().isForbidden());
        mvc.perform(post("/web/session/logout").session(browser.session).header(browser.header, browser.token))
                .andExpect(status().isNoContent());
        assertTrue(browser.session.isInvalid());
        mvc.perform(get("/web/session")).andExpect(status().isUnauthorized());
    }

    @Test void oldAdministrativeRoutesRedirectToVue() throws Exception {
        account(AuthorityName.ROLE_ADMIN);
        Browser browser = login();
        mvc.perform(get("/index/home").session(browser.session))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/ui/#/clients"));
        mvc.perform(get("/users/create").session(browser.session)).andExpect(status().isFound()).andExpect(redirectedUrl("/ui/#/users/new"));
    }

    private static class Browser {
        final MockHttpSession session; final String header; final String token;
        Browser(MockHttpSession session, String header, String token) { this.session = session; this.header = header; this.token = token; }
    }
}
