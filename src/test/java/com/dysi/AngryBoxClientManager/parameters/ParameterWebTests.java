package com.dysi.AngryBoxClientManager.parameters;

import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.*;
import com.dysi.AngryBoxClientManager.payments.PaymentFixtures;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ParameterWebTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired AuthorityRepository authorities;
    @Autowired BankRepository banks;
    @Autowired BankAccountRepository accounts;
    @Autowired PasswordEncoder passwords;
    @Autowired EntityManager em;

    private Browser login(AuthorityName role) throws Exception {
        Authority authority = new Authority(); authority.setName(role); authorities.saveAndFlush(authority);
        User user = new User(); user.setUsername("parameter-tests"); user.setFirstname("Nombre"); user.setLastname("Apellido");
        user.setEmail("parameters@example.test"); user.setPassword(passwords.encode("ParameterTestOnly123!"));
        user.setEnabled(true); user.setAuthorities(new ArrayList<>(List.of(authority))); users.saveAndFlush(user);
        if (role != AuthorityName.ROLE_ADMIN) {
            Client client = new Client(); client.setFirstname("Cliente"); client.setLastname("Parametros");
            client.setUser(user); em.persist(client); user.setClient(client); em.flush();
        }
        MvcResult result = mvc.perform(get("/web/session/csrf")).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
        JsonNode token = json.readTree(result.getResponse().getContentAsString());
        mvc.perform(post("/web/session/login").session(session).header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"parameter-tests\",\"password\":\"ParameterTestOnly123!\"}"))
                .andExpect(status().isOk());
        token = json.readTree(mvc.perform(get("/web/session/csrf").session(session)).andReturn().getResponse().getContentAsString());
        return new Browser(session, token.get("headerName").asText(), token.get("token").asText(), user);
    }
    private JsonNode write(Browser browser, String method, String path, Object input, int expected) throws Exception {
        return json.readTree(mvc.perform(request(HttpMethod.valueOf(method), path).session(browser.session)
                .header(browser.header, browser.token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(input)))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString());
    }
    private long bank(Browser browser, String name) throws Exception {
        return write(browser, "POST", "/web/api/banks", Map.of("name", name, "active", true), 201).get("id").asLong();
    }
    private Map<String, Object> accountInput(long bankId, String cbu, String alias) {
        return new HashMap<>(Map.of("bankId", bankId, "holder", " Titular de prueba ", "cbu", cbu, "alias", alias, "active", true));
    }

    @Test void banksNormalizeValidateDuplicatesAndRetainAudit() throws Exception {
        Browser browser = login(AuthorityName.ROLE_ADMIN);
        long id = bank(browser, "  Banco   PRUEBA  ");
        assertEquals("Banco PRUEBA", banks.findById(id).orElseThrow().getName());
        assertEquals(browser.actor.getId(), banks.findById(id).orElseThrow().getCreatedBy().getId());
        long created = banks.findById(id).orElseThrow().getCreationDate().getTimeInMillis();
        write(browser, "POST", "/web/api/banks", Map.of("name", "banco prueba", "active", false), 409);
        write(browser, "PUT", "/web/api/banks/" + id, Map.of("name", "Banco renombrado", "active", true), 200);
        for (int index = 0; index < 2; index++) {
            assertFalse(write(browser, "PUT", "/web/api/banks/" + id + "/active", Map.of("active", false), 200).get("active").asBoolean());
        }
        assertEquals(created, banks.findById(id).orElseThrow().getCreationDate().getTimeInMillis());
        assertEquals(browser.actor.getId(), banks.findById(id).orElseThrow().getModifiedBy().getId());
        mvc.perform(get("/web/api/banks").session(browser.session).param("status", "INACTIVE").param("keyword", "renombrado"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50)).andExpect(jsonPath("$.totalElements").value(1));
        JsonNode invalid = write(browser, "POST", "/web/api/banks", Map.of("name", " ", "active", true), 400);
        assertTrue(invalid.get("fields").has("name"));
        write(browser, "PUT", "/web/api/banks/999999", Map.of("name", "Nuevo", "active", true), 404);
        mvc.perform(get("/web/api/banks/999999").session(browser.session)).andExpect(status().isNotFound());
    }

    @Test void accountsNormalizeCbuAndAliasAndCheckRelationsAndUniqueness() throws Exception {
        Browser browser = login(AuthorityName.ROLE_ADMIN); long bank = bank(browser, "Banco");
        var input = accountInput(bank, "00000000000-00000000000", " ALIAS.Prueba ");
        input.put("externalCode", " CTA-07 ");
        JsonNode value = write(browser, "POST", "/web/api/bank-accounts", input, 201); long id = value.get("id").asLong();
        assertEquals("CTA-07", value.get("externalCode").asText());
        assertEquals("0000000000000000000000", value.get("cbu").asText());
        assertEquals("alias.prueba", value.get("alias").asText()); assertEquals("Titular de prueba", value.get("holder").asText());
        assertTrue(value.get("available").asBoolean()); assertFalse(value.has("bank")); assertFalse(value.has("createdBy"));
        assertEquals(browser.actor.getId(), accounts.findById(id).orElseThrow().getCreatedBy().getId());
        mvc.perform(get("/web/api/bank-accounts").session(browser.session).param("keyword", "CTA-07"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].externalCode").value("CTA-07"));
        input.remove("externalCode");
        assertEquals("CTA-07", write(browser, "PUT", "/web/api/bank-accounts/" + id, input, 200).get("externalCode").asText());
        input.put("externalCode", "x".repeat(101));
        assertTrue(write(browser, "PUT", "/web/api/bank-accounts/" + id, input, 400).get("fields").has("externalCode"));
        input.put("externalCode", " ");
        assertTrue(write(browser, "PUT", "/web/api/bank-accounts/" + id, input, 200).get("externalCode").isNull());
        input.remove("externalCode");
        write(browser, "POST", "/web/api/bank-accounts", input, 409);
        input.put("cbu", "1111111111111111111111"); write(browser, "POST", "/web/api/bank-accounts", input, 409);
        input.put("alias", ""); long second = write(browser, "POST", "/web/api/bank-accounts", input, 201).get("id").asLong();
        input.put("cbu", "2222222222222222222222"); write(browser, "POST", "/web/api/bank-accounts", input, 201);
        input.put("alias", "ALIAS.PRUEBA"); write(browser, "PUT", "/web/api/bank-accounts/" + second, input, 409);
        input.put("alias", ""); input.put("bankId", 999999); write(browser, "POST", "/web/api/bank-accounts", input, 404);
        input.put("bankId", bank); input.put("cbu", "abc");
        assertTrue(write(browser, "POST", "/web/api/bank-accounts", input, 400).get("fields").has("cbu"));
        input.put("cbu", "123456789012345678901"); write(browser, "POST", "/web/api/bank-accounts", input, 400);
        input.put("cbu", "12345678901234567890123"); write(browser, "POST", "/web/api/bank-accounts", input, 400);
        input.put("cbu", "3333333333333333333333");
        write(browser, "PUT", "/web/api/bank-accounts/999999", input, 404);
        mvc.perform(get("/web/api/bank-accounts").session(browser.session).param("bankId", String.valueOf(bank)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test void availabilityFollowsBankAndAccountWithoutDestroyingPaymentHistory() throws Exception {
        Browser browser = login(AuthorityName.ROLE_ADMIN); long bankId = bank(browser, "Banco original");
        var input = accountInput(bankId, "0000000000000000000000", "original");
        long id = write(browser, "POST", "/web/api/bank-accounts", input, 201).get("id").asLong();
        Client client = new Client(); em.persist(client);
        Payment payment = PaymentFixtures.payment(em, client, null, browser.actor);
        payment.setBankAccount(accounts.findById(id).orElseThrow()); payment.setDestinationBank("Banco original");
        payment.setDestinationHolder("Titular de prueba"); payment.setDestinationCbu("0000000000000000000000"); payment.setDestinationAlias("original");
        em.persist(payment); em.flush();
        write(browser, "PUT", "/web/api/banks/" + bankId + "/active", Map.of("active", false), 200);
        mvc.perform(get("/web/api/bank-accounts/" + id).session(browser.session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true)).andExpect(jsonPath("$.bankActive").value(false)).andExpect(jsonPath("$.available").value(false));
        write(browser, "PUT", "/web/api/banks/" + bankId + "/active", Map.of("active", true), 200);
        input.put("holder", "Nuevo titular"); input.put("alias", "nuevo");
        write(browser, "PUT", "/web/api/bank-accounts/" + id, input, 200);
        for (int retry = 0; retry < 2; retry++) write(browser, "PUT", "/web/api/bank-accounts/" + id + "/active", Map.of("active", false), 200);
        mvc.perform(delete("/web/api/bank-accounts/" + id).session(browser.session).header(browser.header, browser.token))
                .andExpect(status().isMethodNotAllowed());
        em.clear(); Payment saved = em.find(Payment.class, payment.getId());
        assertEquals("Banco original", saved.getDestinationBank()); assertEquals("Titular de prueba", saved.getDestinationHolder());
        assertEquals("original", saved.getDestinationAlias()); assertNotNull(saved.getBankAccount());
        assertFalse(saved.getBankAccount().getActive());
    }

    @Test void searchEscapesWildcardsAndRejectsInvalidPaging() throws Exception {
        Browser browser = login(AuthorityName.ROLE_ADMIN); bank(browser, "Banco %_ prueba"); bank(browser, "Otro banco");
        mvc.perform(get("/web/api/banks").session(browser.session).param("keyword", "%_"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/web/api/options/banks").session(browser.session).param("keyword", "%_"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        for (String resource : List.of("banks", "bank-accounts")) {
            mvc.perform(get("/web/api/" + resource).session(browser.session).param("size", "101")).andExpect(status().isBadRequest());
            mvc.perform(get("/web/api/" + resource).session(browser.session).param("page", "-1")).andExpect(status().isBadRequest());
            mvc.perform(get("/web/api/" + resource).session(browser.session).param("status", "UNKNOWN")).andExpect(status().isBadRequest());
        }
    }

    @ParameterizedTest @EnumSource(value = AuthorityName.class, names = {"ROLE_USER", "ROLE_SELLER"})
    void nonAdminsCannotReadOrWriteParameters(AuthorityName role) throws Exception {
        Browser browser = login(role);
        for (String path : List.of("banks", "banks/1", "bank-accounts", "bank-accounts/1", "options/banks"))
            mvc.perform(get("/web/api/" + path).session(browser.session)).andExpect(status().isForbidden());
        write(browser, "POST", "/web/api/banks", Map.of("name", "Banco", "active", true), 403);
        write(browser, "PUT", "/web/api/bank-accounts/1/active", Map.of("active", true), 403);
    }

    @Test void anonymousAndMissingCsrfAreRejected() throws Exception {
        mvc.perform(get("/web/api/banks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/web/api/bank-accounts")).andExpect(status().isUnauthorized());
        Browser browser = login(AuthorityName.ROLE_ADMIN);
        mvc.perform(post("/web/api/banks").session(browser.session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Banco\",\"active\":true}")).andExpect(status().isForbidden());
        mvc.perform(put("/web/api/bank-accounts/1/active").session(browser.session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}")).andExpect(status().isForbidden());
    }

    private static class Browser {
        final MockHttpSession session; final String header, token; final User actor;
        Browser(MockHttpSession session, String header, String token, User actor) {
            this.session = session; this.header = header; this.token = token; this.actor = actor;
        }
    }
}
