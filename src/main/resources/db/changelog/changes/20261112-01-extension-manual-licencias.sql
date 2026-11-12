--liquibase formatted sql

--changeset dysi:20261112-01-extension-origin dbms:mysql
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'license_extension_certificate' AND column_name = 'payment_id' AND is_nullable = 'NO'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'license_extension_certificate' AND column_name = 'origin'
-- El DEFAULT clasifica los certificados existentes como originados por pago.
ALTER TABLE license_extension_certificate
    MODIFY COLUMN payment_id bigint NULL,
    ADD COLUMN origin varchar(24) NOT NULL DEFAULT 'PAYMENT',
    ADD INDEX ix_extension_client_start (client_id, starts_at, id);
-- Rollback solo sin certificados manuales. Antes de revertir, respaldar la tabla y
-- verificar SELECT COUNT(*) FROM license_extension_certificate WHERE payment_id IS NULL = 0.
-- Si existen solicitudes manuales, conservarlas y recuperar mediante despliegue correctivo;
-- no eliminarlas para forzar el rollback. MySQL confirma este DDL implícitamente.
--rollback ALTER TABLE license_extension_certificate DROP INDEX ix_extension_client_start, DROP COLUMN origin, MODIFY COLUMN payment_id bigint NOT NULL;
