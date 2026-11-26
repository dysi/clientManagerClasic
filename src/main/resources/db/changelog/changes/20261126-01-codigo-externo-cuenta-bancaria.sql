--liquibase formatted sql

--changeset dysi:20261126-01-codigo-externo-cuenta-bancaria dbms:mysql
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'bank_account'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'bank_account' AND column_name = 'external_code'
-- Las cuentas existentes conservan sus datos y quedan sin código externo.
ALTER TABLE bank_account ADD COLUMN external_code varchar(100) NULL;
-- MySQL confirma DDL implícitamente. Respaldar los códigos antes de revertir:
-- el rollback elimina únicamente esta columna y sus valores.
--rollback ALTER TABLE bank_account DROP COLUMN external_code;
