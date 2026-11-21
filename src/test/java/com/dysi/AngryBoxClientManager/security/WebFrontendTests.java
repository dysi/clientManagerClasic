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
    @Autowired javax.persistence.EntityManager em;

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

    @Test void webListsDefaultToFiftyAndLicenseFiltersCombine() throws Exception {
        account(AuthorityName.ROLE_ADMIN); Browser browser = login();
        long first = newClient(browser, "Primero");
        long second = newClient(browser, "Segundo");
        long license = write(browser, "POST", "/web/api/licenses", licenseInput(first, 111L), 201).get("id").asLong();
        write(browser, "POST", "/web/api/licenses", licenseInput(second, 222L), 201);
        mvc.perform(get("/web/api/clients").session(browser.session).param("keyword", "Primero"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].licenseCounts.total").value(1))
                .andExpect(jsonPath("$.content[0].licenseCounts.active").value(1))
                .andExpect(jsonPath("$.content[0].licenseCounts.expired").value(0))
                .andExpect(jsonPath("$.content[0].licenseCounts.blocked").value(0))
                .andExpect(jsonPath("$.content[0].licenseCounts.unknown").value(0));
        for (String resource : List.of("clients", "licenses", "payments", "users")) {
            mvc.perform(get("/web/api/" + resource).session(browser.session))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
        }
        mvc.perform(get("/web/api/licenses").session(browser.session).param("clientId", String.valueOf(first))
                .param("keyword", "Empresa de prueba").param("status", "ACTIVE").param("size", "25"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(license));
        mvc.perform(get("/web/api/licenses").session(browser.session).param("clientId", String.valueOf(first))
                .param("keyword", "No coincide").param("status", "ACTIVE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/web/api/licenses").session(browser.session).param("clientId", String.valueOf(first))
                .param("keyword", "Empresa de prueba").param("status", "BLOCKED").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test void licenseCountdownAndStatusUseTheSameCalendarDayBoundaries() throws Exception {
        account(AuthorityName.ROLE_ADMIN); Browser browser = login();
        var input = licenseInput(newClient(browser, "Fecha"), 333L);
        long id = write(browser, "POST", "/web/api/licenses", input, 201).get("id").asLong();
        int[] days = {-1, 0, 1, 15, 16, 35};
        String[] states = {"EXPIRED", "EXPIRED", "EXPIRING", "EXPIRING", "ACTIVE", "ACTIVE"};
        for (int index = 0; index < days.length; index++) {
            input.put("lockDate", java.time.LocalDate.now().plusDays(days[index]).toString());
            JsonNode result = write(browser, "PUT", "/web/api/licenses/" + id, input, 200);
            assertEquals(days[index], result.get("daysUntilLock").asInt());
            assertEquals(states[index], result.get("status").asText());
            mvc.perform(get("/web/api/clients/" + input.get("clientId")).session(browser.session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.licenseCounts.total").value(1))
                    .andExpect(jsonPath("$.licenseCounts.active").value(days[index] > 0 ? 1 : 0))
                    .andExpect(jsonPath("$.licenseCounts.expired").value(days[index] <= 0 ? 1 : 0));
        }
        assertEquals("BLOCKED", write(browser, "PUT", "/web/api/licenses/" + id + "/lock",
                java.util.Map.of("locked", true), 200).get("status").asText());
        input.put("lockDate", java.time.LocalDate.now().minusDays(1).toString());
        assertEquals("BLOCKED", write(browser, "PUT", "/web/api/licenses/" + id, input, 200).get("status").asText());
        mvc.perform(get("/web/api/clients/" + input.get("clientId")).session(browser.session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licenseCounts.blocked").value(1))
                .andExpect(jsonPath("$.licenseCounts.expired").value(0));
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
        String batchPath = "/web/api/licenses/batch";
        write(browser, "PUT", batchPath, java.util.Map.of("ids", List.of(id, id), "action", "BLOCK"), 400);
        write(browser, "PUT", batchPath, java.util.Map.of("ids", List.of(id), "action", "LOCK_DATE"), 400);
        write(browser, "PUT", batchPath, java.util.Map.of("ids", List.of(id, 999999L), "action", "BLOCK"), 404);
        mvc.perform(get("/web/api/licenses/" + id).session(browser.session))
                .andExpect(jsonPath("$.locked").value(false));
        write(browser, "PUT", "/web/api/licenses/" + id + "/lock", java.util.Map.of("locked", true), 200);
        String date = java.time.LocalDate.now().toString();
        JsonNode changed = write(browser, "PUT", batchPath,
                java.util.Map.of("ids", List.of(second, id), "action", "LOCK_DATE", "lockDate", date), 200);
        assertEquals(2, changed.size());
        for (JsonNode license : changed) {
            assertEquals(date, license.get("lockDate").asText());
            assertEquals(license.get("id").asLong() == id, license.get("locked").asBoolean());
        }
        for (int retry = 0; retry < 2; retry++) {
            JsonNode blocked = write(browser, "PUT", batchPath,
                    java.util.Map.of("ids", List.of(id, second), "action", "BLOCK"), 200);
            for (JsonNode license : blocked) {
                assertTrue(license.get("locked").asBoolean());
                assertEquals(date, license.get("lockDate").asText());
            }
        }
        for (int retry = 0; retry < 2; retry++) {
            JsonNode unblocked = write(browser, "PUT", batchPath,
                    java.util.Map.of("ids", List.of(id, second), "action", "UNBLOCK"), 200);
            for (JsonNode license : unblocked) {
                assertFalse(license.get("locked").asBoolean());
                assertEquals(date, license.get("lockDate").asText());
                assertEquals("EXPIRED", license.get("status").asText());
            }
        }
        mvc.perform(put(batchPath).session(browser.session).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("ids", List.of(id), "action", "BLOCK"))))
                .andExpect(status().isForbidden());
    }

    @Test void obsoletePaymentWritesAreClosedAndUnknownIdsRemainNotFound() throws Exception {
        account(AuthorityName.ROLE_ADMIN); Browser browser = login();
        write(browser, "POST", "/web/api/payments", java.util.Map.of(), 409);
        write(browser, "PUT", "/web/api/payments/999999", java.util.Map.of(), 404);
        write(browser, "PUT", "/web/api/payments/999999/status", java.util.Map.of("removed", true), 404);
        assertEquals(0, payments.count());
    }

    @Test void paymentListIncludesClientPaymentsWithoutLicenseAndHasNoLegacyOrPrivateFields() throws Exception {
        User actor = account(AuthorityName.ROLE_ADMIN); Browser browser = login();
        Client client = new Client(); client.setFirstname("Cliente"); client.setLastname("Sin licencia");
        clients.saveAndFlush(client);
        Payment payment = com.dysi.AngryBoxClientManager.payments.PaymentFixtures.payment(em, client, null, actor);
        em.persist(payment); em.flush();
        mvc.perform(get("/web/api/payments").session(browser.session).param("keyword", "Sin licencia").param("status", "PENDIENTE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licenseId").isEmpty())
                .andExpect(jsonPath("$.content[0].amount").value(100.25))
                .andExpect(jsonPath("$.content[0].bankAccountId").value(payment.getBankAccount().getId()))
                .andExpect(jsonPath("$.content[0].destinationBank").value(payment.getDestinationBank()))
                .andExpect(jsonPath("$.content[0].decidedBy").isEmpty())
                .andExpect(jsonPath("$.content[0].discount").doesNotExist())
                .andExpect(jsonPath("$.content[0].paymentRealDate").doesNotExist())
                .andExpect(jsonPath("$.content[0].bankAccount").doesNotExist())
                .andExpect(jsonPath("$.content[0].submittedBy").doesNotExist());
        mvc.perform(get("/web/api/payments").session(browser.session).param("status", "APROBADO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/web/api/payments").session(browser.session).param("bankAccountId", "999999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/web/api/payments/counts").session(browser.session)
                .param("bankAccountId", String.valueOf(payment.getBankAccount().getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(1));
        mvc.perform(get("/web/api/options/payment-bank-accounts").session(browser.session)
                .param("keyword", payment.getDestinationBank()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(payment.getBankAccount().getId()));
        write(browser, "PUT", "/web/api/payments/" + payment.getId(), java.util.Map.of(), 409);
        write(browser, "PUT", "/web/api/payments/" + payment.getId() + "/status", java.util.Map.of("removed", true), 409);
        assertEquals(PaymentStatus.PENDIENTE, payments.findById(payment.getId()).orElseThrow().getStatus());
        payment.decide(PaymentStatus.APROBADO);
        PaymentStatusHistory decision = new PaymentStatusHistory(); decision.setPayment(payment);
        decision.setPreviousStatus(PaymentStatus.PENDIENTE); decision.setNewStatus(PaymentStatus.APROBADO);
        decision.setOccurredAt(java.time.LocalDateTime.of(2026, 11, 21, 12, 0));
        decision.setResponsibleUser(actor); decision.setOrigin(OperationOrigin.ACCESO_AUTENTICADO);
        em.persist(decision); em.flush();
        mvc.perform(get("/web/api/payments").session(browser.session).param("status", "APROBADO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].decidedBy").value(actor.getUsername()));

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
        JsonNode second = write(browser, "POST", "/web/api/users", input, 201);
        long secondId = second.get("id").asLong();
        Browser firstLogin = login("new-user", "NewPassword123!");
        Browser secondLogin = login("another-user", "NewPassword123!");
        mvc.perform(get("/web/portal/overview").session(firstLogin.session)).andExpect(status().isOk());
        mvc.perform(get("/web/portal/overview").session(secondLogin.session)).andExpect(status().isOk());
        write(browser, "PUT", "/web/api/users/" + secondId + "/password",
                java.util.Map.of("password", "IndependentPassword123!"), 204);
        assertEquals(hash, users.findById(id).orElseThrow().getPassword());
        mvc.perform(get("/web/session").session(firstLogin.session)).andExpect(status().isOk());
        mvc.perform(get("/web/session").session(secondLogin.session)).andExpect(status().isUnauthorized());
        assertEquals(client, users.findById(secondId).orElseThrow().getClient().getId());
        assertEquals(id, clients.findById(client).orElseThrow().getUser().getId());
        mvc.perform(get("/web/api/clients/" + client).session(browser.session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userCount").value(2));
        mvc.perform(get("/web/api/clients").param("keyword", "another-user").session(browser.session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        input.put("username", "new-user"); input.remove("password"); input.put("clientId", null);
        write(browser, "PUT", "/web/api/users/" + id, input, 200);
        assertEquals(secondId, clients.findById(client).orElseThrow().getUser().getId());
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
        return login("browser-admin", "test-password");
    }

    private Browser login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(get("/web/session/csrf")).andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
        String oldId = session.getId();
        JsonNode csrf = json.readTree(result.getResponse().getContentAsString());
        mvc.perform(post("/web/session/login").session(session)
                .header(csrf.get("headerName").asText(), csrf.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("username", username, "password", password))))
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
        String body = "{\"firstname\":\"Ana\",\"lastname\":\"Pérez\",\"email\":\"ana@example.test\",\"phone\":\"123456\",\"cuil\":\"20123456786\"}";
        MvcResult created = mvc.perform(post("/web/api/clients").session(browser.session)
                .header(browser.header, browser.token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        assertEquals("browser-admin", clients.findById(id).orElseThrow().getCreatedBy().getUsername());
        mvc.perform(put("/web/api/clients/" + id).session(browser.session)
                .header(browser.header, browser.token).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("Pérez", "Gómez")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("123456"))
                .andExpect(jsonPath("$.email").value("ana@example.test")).andExpect(jsonPath("$.cuil").value("20123456786"));
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
        Client client = new Client(); client.setFirstname("Portal"); client.setLastname("Prueba");
        client.setDni("1234567"); client.setCuil("20-01234567-5"); client.setUser(user);
        clients.saveAndFlush(client); user.setClient(client); users.saveAndFlush(user);
        Browser browser = login();
        mvc.perform(get("/web/session").session(browser.session)).andExpect(jsonPath("$.access").value("PORTAL"));
        mvc.perform(get("/web/portal/context").session(browser.session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Portal"))
                .andExpect(jsonPath("$.origin").value("ACCESO_AUTENTICADO"));
        mvc.perform(get("/web/api/clients").session(browser.session)).andExpect(status().isForbidden());
        write(browser, "PUT", "/web/api/licenses/batch", java.util.Map.of("ids", List.of(1L), "action", "BLOCK"), 403);
        mvc.perform(get("/users/create").session(browser.session)).andExpect(status().isForbidden());
        // Incluso un usuario ADMIN asociado solo recibe la capacidad del portal por documento.
        Authority admin = new Authority(); admin.setName(AuthorityName.ROLE_ADMIN); authorities.saveAndFlush(admin);
        user.setAuthorities(new java.util.ArrayList<>(List.of(admin))); users.saveAndFlush(user);
        mvc.perform(post("/web/session/document").session(browser.session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"document\":\"1.234.567\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/web/session/document").session(browser.session).header(browser.header, browser.token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"document\":\"20-01234567-5\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("PORTAL_VIEW"))
                .andExpect(jsonPath("$.origin").value("PORTAL_DOCUMENTO"));
        mvc.perform(get("/web/api/banks").session(browser.session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/clients/getClientsLookups").session(browser.session)).andExpect(status().isForbidden());
        mvc.perform(get("/web/portal/clients/999999").session(browser.session)).andExpect(status().isForbidden());
        mvc.perform(get("/web/portal/context").session(browser.session).param("clientId", "999999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Portal"))
                .andExpect(jsonPath("$.cuil").doesNotExist()).andExpect(jsonPath("$.username").doesNotExist());
        // La coincidencia desde los dígitos centrales funciona también sin DNI explícito.
        client.setDni(null); clients.saveAndFlush(client);
        JsonNode token = json.readTree(mvc.perform(get("/web/session/csrf").session(browser.session))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(post("/web/session/document").session(browser.session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"document\":\"01234567\"}"))
                .andExpect(status().isOk());
        Client ambiguous = new Client(); ambiguous.setDni("01234567"); clients.saveAndFlush(ambiguous);
        token = json.readTree(mvc.perform(get("/web/session/csrf").session(browser.session))
                .andReturn().getResponse().getContentAsString());
        mvc.perform(post("/web/session/document").session(browser.session)
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"document\":\"01234567\"}"))
                .andExpect(status().isUnauthorized());
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
                .andExpect(status().isFound()).andExpect(redirectedUrl("/ui/#/licenses"));
        mvc.perform(get("/users/create").session(browser.session)).andExpect(status().isFound()).andExpect(redirectedUrl("/ui/#/users/new"));
    }

    private static class Browser {
        final MockHttpSession session; final String header; final String token;
        Browser(MockHttpSession session, String header, String token) { this.session = session; this.header = header; this.token = token; }
    }
}
