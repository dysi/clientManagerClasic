package com.dysi.AngryBoxClientManager.security;

import com.dysi.AngryBoxClientManager.models.User;
import com.dysi.AngryBoxClientManager.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class Java11AuthenticationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtTokenUtil tokens;
    @Autowired private com.dysi.AngryBoxClientManager.repositories.AuthorityRepository authorities;
    @Autowired private com.dysi.AngryBoxClientManager.repositories.ClientRepository clients;

    @Test
    void bearerWritesUseThePersistentAuthenticatedAuditActor() throws Exception {
        var role = new com.dysi.AngryBoxClientManager.models.Authority();
        role.setName(com.dysi.AngryBoxClientManager.models.AuthorityName.ROLE_ADMIN); authorities.saveAndFlush(role);
        User user = new User(); user.setUsername("audit-bearer"); user.setFirstname("Audit"); user.setLastname("Bearer");
        user.setEmail("audit-bearer@example.test"); user.setEnabled(true);
        user.setPassword(passwordEncoder.encode("BearerAuditOnly123!")); user.setAuthorities(new java.util.ArrayList<>(java.util.List.of(role)));
        users.saveAndFlush(user);
        String login = mockMvc.perform(post("/auth").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"audit-bearer\",\"password\":\"BearerAuditOnly123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(login).get("token").asText();
        mockMvc.perform(post("/api/clients/edit").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstname\":\"Bearer client\",\"lastname\":\"Audit\",\"email\":\"bearer-client@example.test\",\"phone\":\"1234\",\"cuil\":\"\"}"))
                .andExpect(status().isOk());
        var created = clients.findAll().stream().filter(client -> "Bearer client".equals(client.getFirstname())).findFirst().orElseThrow();
        assertEquals(user.getId(), created.getCreatedBy().getId());
        assertEquals(user.getId(), created.getModifiedBy().getId());
    }

    @Test
    void loginGeneratesTokenUsableOnProtectedEndpoint() throws Exception {
        User user = new User();
        user.setUsername("java11user");
        user.setPassword(passwordEncoder.encode("test-password"));
        user.setFirstname("Test");
        user.setLastname("User");
        user.setEmail("java11@example.test");
        user.setEnabled(true);
        user.setAuthorities(Collections.emptyList());
        users.saveAndFlush(user);

        String response = mockMvc.perform(post("/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"java11user\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        String token = body.get("token").asText();
        assertFalse(token.isEmpty());
        assertEquals("java11user", tokens.getUsernameFromToken(token));
        assertEquals("web", tokens.getAudienceFromToken(token));

        mockMvc.perform(get("/web/api/clients").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/user").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("java11user"));
    }
}
