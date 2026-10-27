package com.dysi.AngryBoxClientManager.parameters;

import com.dysi.AngryBoxClientManager.models.Bank;
import com.dysi.AngryBoxClientManager.models.BankAccount;
import com.dysi.AngryBoxClientManager.repositories.BankRepository;
import com.dysi.AngryBoxClientManager.repositories.BankAccountRepository;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.regex.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.use-new-id-generator-mappings=false",
    "spring.datasource.url=jdbc:h2:mem:portal_auto_regression;MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class PortalAutoIncrementTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired BankRepository banks;
    @Autowired BankAccountRepository accounts;

    @Test void autoMappingCreatesBankAndAccountWithoutExplicitIds() {
        Bank bank = new Bank(); bank.setName("Banco AUTO"); bank = banks.saveAndFlush(bank);
        BankAccount account = new BankAccount(); account.setBank(bank); account.setHolder("Titular AUTO");
        account.setCbu("2222222222222222222222"); account = accounts.saveAndFlush(account);
        assertNotNull(bank.getId()); assertNotNull(account.getId());
        assertNotNull(bank.getCreationDate()); assertNotNull(account.getCreationDate());
    }

    @Test void actualMigrationDdlCreatesAutoIncrementColumnsAndPreservesPreviousIds() throws Exception {
        String script;
        try (InputStream input = new ClassPathResource("db/20261026-03-modelo-portal-pagos.sql").getInputStream()) {
            script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        jdbc.execute(probeDdl(script, "bank"));
        jdbc.execute(probeDdl(script, "bank_account"));
        try {
            jdbc.update("INSERT INTO probe_bank(id,name,normalized_name,active) VALUES(9000,'Existente','existente',true)");
            jdbc.update("INSERT INTO probe_bank_account(id,bank_id,holder,cbu,active) VALUES(9001,9000,'Anterior','0000000000000000000000',true)");
            long bankId = insert("INSERT INTO probe_bank(name,normalized_name,active) VALUES('Nuevo','nuevo',true)");
            long accountId = insert("INSERT INTO probe_bank_account(bank_id,holder,cbu,active) VALUES(9000,'Nuevo','1111111111111111111111',true)");
            assertTrue(bankId > 9000); assertTrue(accountId > 9001);
            assertEquals("Existente", jdbc.queryForObject("SELECT name FROM probe_bank WHERE id=9000", String.class));
            assertEquals(9000L, jdbc.queryForObject("SELECT bank_id FROM probe_bank_account WHERE id=9001", Long.class));
        } finally {
            jdbc.execute("DROP TABLE probe_bank_account"); jdbc.execute("DROP TABLE probe_bank");
        }
    }

    private String probeDdl(String script, String table) {
        Matcher matcher = Pattern.compile("CREATE TABLE `" + table + "` \\([\\s\\S]*?\\) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;").matcher(script);
        assertTrue(matcher.find(), "No se encontró el DDL de " + table);
        return matcher.group().replace("`bank_account`", "`probe_bank_account`").replace("`bank`", "`probe_bank`")
                .replaceAll("CONSTRAINT (\\w+)", "CONSTRAINT probe_$1");
    }
    private long insert(String sql) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS), keys);
        Number key = keys.getKey(); assertNotNull(key); return key.longValue();
    }
}
