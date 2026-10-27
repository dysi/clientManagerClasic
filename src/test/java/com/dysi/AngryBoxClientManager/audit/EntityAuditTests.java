package com.dysi.AngryBoxClientManager.audit;

import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.security.JwtUserFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EntityAuditTests {
    @Autowired EntityManager em;
    @Autowired PasswordEncoder passwords;
    @Autowired ObjectMapper json;
    private User creator;
    private User editor;

    @BeforeEach void actors() {
        SecurityContextHolder.clearContext();
        creator = user("audit-creator"); editor = user("audit-editor");
        em.persist(creator); em.persist(editor); em.flush();
        as(creator);
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    private void as(User actor) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                JwtUserFactory.create(actor), null, Collections.emptyList()));
    }

    private User user(String username) {
        User user = new User(); user.setUsername(username); user.setFirstname("Nombre"); user.setLastname("Apellido");
        user.setEmail(username + "@example.test"); user.setEnabled(true);
        user.setPassword(passwords.encode("AuditTestsOnly123!")); user.setAuthorities(new ArrayList<>());
        return user;
    }

    private List<AuditableEntity> records() {
        Client client = new Client(); client.setFirstname("Audit"); client.setLastname("Client"); em.persist(client);
        License license = new License(); license.setCompanyId(700L); license.setCompanyName("Audit license");
        license.setClient(client);
        LicenseStatus state = new LicenseStatus(); state.setLicense(license); state.setLockDate(Calendar.getInstance());
        em.persist(license);
        Payment payment = com.dysi.AngryBoxClientManager.payments.PaymentFixtures.payment(em, client, license, creator);
        em.persist(payment);
        license.setLicenseStatuses(new ArrayList<>(List.of(state)));
        license.setPaymentList(new ArrayList<>(List.of(payment)));
        em.persist(license);
        User user = user("audit-created"); em.persist(user);
        em.flush();
        return List.of(client, license, state, payment, user);
    }

    private AuditableEntity reload(AuditableEntity record) {
        return em.find(record.getClass(), em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(record));
    }

    private void change(AuditableEntity record, String value) {
        if (record instanceof Client) ((Client) record).setPhone(value);
        else if (record instanceof License) ((License) record).setDescription(value);
        else if (record instanceof LicenseStatus) ((LicenseStatus) record).setObservation(value);
        else if (record instanceof Payment) ((Payment) record).setContactPhone(value);
        else if (record instanceof User) ((User) record).setFirstname(value);
    }

    @Test void allFiveEntitiesPersistCreationAndModificationWithRealActorReferences() {
        List<AuditableEntity> records = records();
        Map<AuditableEntity, Long> creationTimes = new IdentityHashMap<>();
        for (AuditableEntity record : records) {
            assertNotNull(record.getCreationDate()); assertNotNull(record.getModificationDate());
            assertEquals(creator.getId(), record.getCreatedBy().getId());
            assertEquals(creator.getId(), record.getModifiedBy().getId());
            assertNull(record.getRemoveDate()); assertNull(record.getRemovedBy());
            creationTimes.put(record, record.getCreationDate().getTimeInMillis());
        }
        as(editor);
        records.forEach(record -> change(record, "Modified")); em.flush(); em.clear();
        for (AuditableEntity record : records) {
            AuditableEntity saved = reload(record);
            assertEquals(creationTimes.get(record).longValue(), saved.getCreationDate().getTimeInMillis());
            assertEquals(creator.getId(), saved.getCreatedBy().getId());
            assertEquals(editor.getId(), saved.getModifiedBy().getId());
            assertTrue(saved.getModificationDate().getTimeInMillis() >= saved.getCreationDate().getTimeInMillis());
        }
    }

    @Test void removalIsIdempotentAndReactivationClearsDateAndActorForAllEntities() {
        List<AuditableEntity> records = records(); as(editor);
        records.forEach(AuditableEntity::markRemoved); em.flush();
        Map<AuditableEntity, Long> removalTimes = new IdentityHashMap<>();
        for (AuditableEntity record : records) {
            removalTimes.put(record, record.getRemoveDate().getTimeInMillis());
            assertEquals(editor.getId(), record.getRemovedBy().getId());
        }
        as(creator);
        for (AuditableEntity record : records) {
            Calendar replacement = Calendar.getInstance(); replacement.add(Calendar.DATE, 10);
            record.setRemoveDate(replacement); change(record, "While removed");
        }
        em.flush(); em.clear();
        List<AuditableEntity> saved = new ArrayList<>();
        for (AuditableEntity record : records) {
            AuditableEntity loaded = reload(record); saved.add(loaded);
            assertEquals(removalTimes.get(record).longValue(), loaded.getRemoveDate().getTimeInMillis());
            assertEquals(editor.getId(), loaded.getRemovedBy().getId());
            assertEquals(creator.getId(), loaded.getModifiedBy().getId());
        }
        saved.forEach(AuditableEntity::restore); em.flush(); em.clear();
        for (AuditableEntity record : saved) {
            AuditableEntity loaded = reload(record);
            assertNull(loaded.getRemoveDate()); assertNull(loaded.getRemovedBy());
            loaded.markRemoved();
        }
        em.flush(); em.clear();
        for (AuditableEntity record : saved) assertEquals(creator.getId(), reload(record).getRemovedBy().getId());
    }

    @Test void historicalDatesAndUnknownActorsAreNotFabricatedByAnOrdinaryUpdate() {
        Payment payment = (Payment) records().get(3);
        Timestamp created = Timestamp.valueOf("2020-02-01 10:00:00");
        Timestamp removed = Timestamp.valueOf("2020-03-01 11:00:00");
        em.createNativeQuery("UPDATE payment SET creationdate=:created, removedate=:removed, created_by_id=NULL, removed_by_id=NULL WHERE id=:id")
                .setParameter("created", created).setParameter("removed", removed).setParameter("id", payment.getId()).executeUpdate();
        em.clear(); as(editor);
        Payment historical = em.find(Payment.class, payment.getId()); historical.setContactPhone("Historical update"); em.flush(); em.clear();
        historical = em.find(Payment.class, payment.getId());
        assertEquals(created.getTime(), historical.getCreationDate().getTimeInMillis());
        assertEquals(removed.getTime(), historical.getRemoveDate().getTimeInMillis());
        assertNull(historical.getCreatedBy()); assertNull(historical.getRemovedBy());
        assertEquals(editor.getId(), historical.getModifiedBy().getId());
        em.createNativeQuery("UPDATE payment SET creationdate=NULL WHERE id=:id").setParameter("id", payment.getId()).executeUpdate();
        em.clear(); historical = em.find(Payment.class, payment.getId()); historical.setContactPhone("Unknown creation date"); em.flush(); em.clear();
        assertNull(em.find(Payment.class, payment.getId()).getCreationDate());
    }

    @Test void backgroundWritesHaveDatesButDoNotReuseThePreviousAuthenticatedActor() {
        SecurityContextHolder.clearContext();
        Client client = new Client(); client.setFirstname("Background"); em.persist(client); em.flush();
        assertNotNull(client.getCreationDate()); assertNull(client.getCreatedBy()); assertNull(client.getModifiedBy());
        as(editor); client.setLastname("Modified"); em.flush();
        assertEquals(editor.getId(), client.getModifiedBy().getId());
        SecurityContextHolder.clearContext(); client.setLastname("System update"); em.flush();
        assertNull(client.getModifiedBy());
    }

    @Test void auditCannotBeSpoofedByJsonAndActorGraphsAreNotSerialized() throws Exception {
        Payment source = (Payment) records().get(3);
        JsonNode output = json.valueToTree(source);
        assertTrue(output.has("creationDate")); assertTrue(output.has("removeDate"));
        assertFalse(output.has("createdBy")); assertFalse(output.has("modifiedBy")); assertFalse(output.has("removedBy"));
        assertFalse(output.has("modificationDate"));
        Payment input = json.readValue("{\"creationDate\":0,\"modificationDate\":0,\"removeDate\":0,\"createdBy\":{\"id\":1},\"modifiedBy\":{\"id\":1},\"removedBy\":{\"id\":1}}", Payment.class);
        assertNull(input.getCreationDate()); assertNull(input.getModificationDate()); assertNull(input.getRemoveDate());
        assertNull(input.getCreatedBy()); assertNull(input.getModifiedBy()); assertNull(input.getRemovedBy());
    }

    @Test void changingOnlyUserRolesStillUpdatesTheOwnerAudit() {
        User account = user("audit-roles"); em.persist(account);
        Authority role = new Authority(); role.setName(AuthorityName.ROLE_USER); em.persist(role); em.flush();
        as(editor); account.setAuthorities(new ArrayList<>(List.of(role))); em.flush(); em.clear();
        assertEquals(editor.getId(), em.find(User.class, account.getId()).getModifiedBy().getId());
    }
}
