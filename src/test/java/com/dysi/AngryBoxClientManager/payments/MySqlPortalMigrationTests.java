package com.dysi.AngryBoxClientManager.payments;

import com.dysi.AngryBoxClientManager.models.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

/** Requiere instancia LOCAL desechable, sin esquema previo. Nunca lee la URL de produccion. */
@EnabledIfEnvironmentVariable(named = "CM_STAGE1_MYSQL", matches = "true")
class MySqlPortalMigrationTests {
    private static final String SERVER = "jdbc:mysql://127.0.0.1:33316/";
    private static final String SCHEMA = "clientmanager_stage1_disposable";
    private static final String OPTIONS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Argentina/Buenos_Aires";

    @Test void migratesLegacyPaymentsAndValidatesHibernateMappings() throws Exception {
        String password = System.getenv().getOrDefault("CM_STAGE1_MYSQL_PASSWORD", "");
        try (Connection connection = DriverManager.getConnection(SERVER + OPTIONS, "root", password)) {
            // CREATE falla si existe: nunca vaciar ni recrear una base preexistente.
            execute(connection, "CREATE DATABASE " + SCHEMA + " CHARACTER SET utf8mb4");
            execute(connection, "USE " + SCHEMA);
            script(connection, "/db/legacy-portal-fixture.sql");
            script(connection, "/db/20261102-01-migracion-consolidada.sql#20261026-01-auditoria-entidades.sql");
            assertThrows(SQLException.class, () -> script(connection, "/db/20261102-01-migracion-consolidada.sql#20261026-03-modelo-portal-pagos.sql"));
            assertEquals(2, count(connection, "payment"));
            execute(connection, "DROP PROCEDURE cm_require_empty_payment");
            execute(connection, "CREATE TABLE unexpected_dependency (id BIGINT PRIMARY KEY, payment_id BIGINT, FOREIGN KEY (payment_id) REFERENCES payment(id)) ENGINE=InnoDB");
            assertThrows(SQLException.class, () -> script(connection, "/db/20261102-01-migracion-consolidada.sql#20261026-02-vaciar-pagos-antiguos.sql"));
            assertEquals(2, count(connection, "payment"));
            execute(connection, "DROP PROCEDURE cm_check_legacy_payment_dependencies");
            execute(connection, "DROP TABLE unexpected_dependency");
            script(connection, "/db/20261102-01-migracion-consolidada.sql#20261026-02-vaciar-pagos-antiguos.sql");
            assertEquals(0, count(connection, "payment"));
            assertEquals(2, count(connection, "client")); assertEquals(1, count(connection, "user"));
            assertEquals(1, count(connection, "license")); assertEquals(1, count(connection, "licensestatus"));
            script(connection, "/db/20261102-01-migracion-consolidada.sql#20261026-03-modelo-portal-pagos.sql");
            script(connection, "/db/20261102-01-migracion-consolidada.sql#20261030-03-correccion-pagos-pendientes.sql");
            script(connection, "/db/20261102-01-migracion-consolidada.sql#20261031-01-correccion-destino-bancario-pagos.sql");
            script(connection, "/db/20261102-04-validaciones-portal-mysql57.sql");
            assertEquals(0, count(connection, "client_account_balance"));
            execute(connection, "INSERT INTO bank(id,name,normalized_name,active) VALUES (100,'Prueba','prueba',1)");
            execute(connection, "INSERT INTO bank_account(id,bank_id,holder,cbu,active) VALUES (101,100,'Titular','0000000000000000000000',1)");
            String payment = "INSERT INTO payment(id,client_id,license_id,amount,currency,issued_at,contact_phone,method,bank_account_id,destination_bank,destination_holder,destination_cbu,status,origin,submitted_by_id,without_receipt,no_receipt_reason,submission_key,version) VALUES (100,1,1,0.10,'ARS','2026-10-26 12:30:00','0000','TRANSFERENCIA',101,'Prueba','Titular','0000000000000000000000','PENDIENTE','ACCESO_AUTENTICADO',1,1,'Motivo','test-key',0)";
            execute(connection, payment);
            // Comprueba los IDs automaticos del script 03 con registros existentes.
            execute(connection, "INSERT INTO bank(name,normalized_name,active) VALUES('Otro','otro',1)");
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id FROM bank WHERE normalized_name='otro'")) {
                assertTrue(rows.next()); assertTrue(rows.getLong(1) > 100);
            }
            execute(connection, "INSERT INTO bank_account(bank_id,holder,cbu,active) VALUES(100,'Nuevo','1111111111111111111111',1)");
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id FROM bank_account WHERE cbu='1111111111111111111111'")) {
                assertTrue(rows.next()); assertTrue(rows.getLong(1) > 101);
            }
            assertThrows(SQLException.class, () -> execute(connection, "UPDATE payment SET client_id=2 WHERE id=100"));
            assertThrows(SQLException.class, () -> execute(connection, "UPDATE payment SET amount=0 WHERE id=100"));
            assertThrows(SQLException.class, () -> execute(connection, "UPDATE payment SET method='QR' WHERE id=100"));
            assertThrows(SQLException.class, () -> execute(connection, "DELETE FROM license WHERE id=1"));
            assertThrows(SQLException.class, () -> execute(connection, "DELETE FROM bank_account WHERE id=101"));
            assertEquals(1, count(connection, "payment"));
        }
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.connection.driver_class", "com.mysql.cj.jdbc.Driver");
        settings.put("hibernate.connection.url", SERVER + SCHEMA + OPTIONS);
        settings.put("hibernate.connection.username", "root"); settings.put("hibernate.connection.password", password);
        settings.put("hibernate.dialect", "org.hibernate.dialect.MySQL57Dialect");
        settings.put("hibernate.physical_naming_strategy", "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");
        var registry = new StandardServiceRegistryBuilder().applySettings(settings).build();
        try {
            MetadataSources sources = new MetadataSources(registry);
            for (Class<?> type : List.of(User.class, Authority.class, Client.class, License.class, LicenseStatus.class,
                    Payment.class, Bank.class, BankAccount.class, ClientAccountBalance.class, Comment.class,
                    Attachment.class, PaymentComment.class, PaymentAttachment.class, PaymentStatusHistory.class,
                    PaymentLicenseAction.class)) sources.addAnnotatedClass(type);
            new SchemaValidator().validate(sources.buildMetadata(), registry);
        } finally { StandardServiceRegistryBuilder.destroy(registry); }
    }

    private static long count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next(); return rows.getLong(1);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) { statement.execute(sql); }
    }

    /** Respeta DELIMITER de los scripts MySQL; detiene la ejecucion al primer error. */
    private static void script(Connection connection, String resource) throws Exception {
        String[] location = resource.split("#", 2);
        boolean inSection = location.length == 1;
        String delimiter = ";"; StringBuilder sql = new StringBuilder();
        try (InputStream input = Objects.requireNonNull(MySqlPortalMigrationTests.class.getResourceAsStream(location[0]));
                BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (location.length == 2) {
                    if (line.equals("-- BEGIN " + location[1])) { inSection = true; continue; }
                    if (line.equals("-- END " + location[1])) break;
                    if (!inSection) continue;
                }
                String trimmed = line.replace("\uFEFF", "").trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
                if (trimmed.startsWith("DELIMITER ")) { delimiter = trimmed.substring(10).trim(); continue; }
                sql.append(line.replace("\uFEFF", "")).append('\n');
                if (trimmed.endsWith(delimiter)) {
                    String statement = sql.toString().trim();
                    execute(connection, statement.substring(0, statement.length() - delimiter.length())); sql.setLength(0);
                }
            }
        }
        if (!inSection) throw new IllegalArgumentException("Seccion SQL inexistente: " + resource);
        if (!sql.toString().trim().isEmpty()) throw new IllegalArgumentException("SQL sin terminador: " + resource);
    }
}
