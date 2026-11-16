--liquibase formatted sql

--changeset dysi:20261113-01-client-users-index dbms:mysql
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'client_id'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'ix_user_client_accounts'
-- Conservar un índice para la FK antes de quitar la unicidad histórica, si existe.
ALTER TABLE `user` ADD INDEX ix_user_client_accounts (client_id);
--rollback ALTER TABLE `user` DROP INDEX ix_user_client_accounts;

--changeset dysi:20261113-02-client-users-cardinality dbms:mysql
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM client c JOIN `user` u ON u.id = c.user_id WHERE u.client_id IS NULL OR u.client_id <> c.id
-- Los nombres de índices generados por Hibernate varían entre instalaciones.
-- Solo se eliminan índices únicos de una columna: user.client_id.
-- No se modifica client.user_id: referencia histórica del acceso por documento y DTO antiguos.
SET @cm_account_indexes = (SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', REPLACE(index_name, '`', '``'), '`') SEPARATOR ', ') FROM (SELECT index_name FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND non_unique = 0 AND index_name <> 'PRIMARY' GROUP BY index_name HAVING COUNT(*) = 1 AND MAX(column_name) = 'client_id') account_indexes);
SET @cm_account_ddl = IF(@cm_account_indexes IS NULL, 'SELECT 1', CONCAT('ALTER TABLE `user` ', @cm_account_indexes));
PREPARE cm_account_statement FROM @cm_account_ddl;
EXECUTE cm_account_statement;
DEALLOCATE PREPARE cm_account_statement;
-- Requiere respaldo y ensayo sobre una copia MySQL antes del despliegue. DDL confirma implícitamente.
-- Rollback solo después de volver a una cuenta por cliente sin borrar cuentas ni evidencia.
-- Si hay varias cuentas, detener la reversión y recuperar mediante despliegue correctivo.
--rollback ALTER TABLE `user` ADD CONSTRAINT uk_user_client_single_account UNIQUE (client_id);
