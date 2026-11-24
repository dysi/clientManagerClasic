--liquibase formatted sql

--changeset dysi:20261120-01-payment-history-index dbms:mysql
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:2 SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'payment_status_history' AND index_name = 'uk_payment_history_state'
-- Mantener soporte para la FK al quitar la unicidad por estado. No elimina evidencia.
ALTER TABLE payment_status_history ADD INDEX ix_payment_history_payment (payment_id);
--rollback ALTER TABLE payment_status_history DROP INDEX ix_payment_history_payment;

--changeset dysi:20261120-02-payment-history-repeat dbms:mysql
ALTER TABLE payment_status_history DROP INDEX uk_payment_history_state;
-- Reversión solo si no hay estados repetidos; respaldar y ensayar sobre copia MySQL.
-- No borrar evidencia para forzar el rollback: usar un despliegue correctivo si hay repeticiones.
--rollback ALTER TABLE payment_status_history ADD CONSTRAINT uk_payment_history_state UNIQUE (payment_id, new_status);

--changeset dysi:20261120-03-payment-history-bi dbms:mysql splitStatements:true endDelimiter:;; rollbackSplitStatements:true rollbackEndDelimiter:;;
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema = DATABASE() AND trigger_name = 'cm_payment_status_history_bi'
-- Separación explícita con ;; compatible con Liquibase 4.9.1; los ; internos pertenecen al trigger.
-- MySQL confirma DDL implícitamente. Ante fallo entre DROP y CREATE, restaurar el trigger desde respaldo antes de habilitar escrituras.
DROP TRIGGER cm_payment_status_history_bi;;
CREATE TRIGGER cm_payment_status_history_bi BEFORE INSERT ON `payment_status_history` FOR EACH ROW
BEGIN
    IF NOT ((NEW.previous_status IS NULL AND NEW.new_status = 'PENDIENTE') OR (NEW.previous_status IS NOT NULL AND NEW.previous_status IN ('PENDIENTE','APROBADO','RECHAZADO') AND NEW.previous_status <> NEW.new_status AND NEW.new_status IN ('APROBADO','RECHAZADO'))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_transition';
    END IF;
    IF NOT (NEW.new_status <> 'RECHAZADO' OR (NEW.reason IS NOT NULL AND CHAR_LENGTH(TRIM(NEW.reason)) > 0)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_rejection';
    END IF;
END;;
--rollback DROP TRIGGER cm_payment_status_history_bi;;
--rollback CREATE TRIGGER cm_payment_status_history_bi BEFORE INSERT ON `payment_status_history` FOR EACH ROW BEGIN     IF NOT ((NEW.previous_status IS NULL AND NEW.new_status = 'PENDIENTE') OR (NEW.previous_status IS NOT NULL AND NEW.previous_status = 'PENDIENTE' AND NEW.new_status IN ('APROBADO','RECHAZADO'))) THEN         SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_transition';     END IF;     IF NOT (NEW.new_status <> 'RECHAZADO' OR (NEW.reason IS NOT NULL AND CHAR_LENGTH(TRIM(NEW.reason)) > 0)) THEN         SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_rejection';     END IF; END;;

--changeset dysi:20261120-04-payment-history-bu dbms:mysql splitStatements:true endDelimiter:;; rollbackSplitStatements:true rollbackEndDelimiter:;;
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema = DATABASE() AND trigger_name = 'cm_payment_status_history_bu'
-- Separación explícita con ;; compatible con Liquibase 4.9.1; los ; internos pertenecen al trigger.
-- MySQL confirma DDL implícitamente. Ante fallo entre DROP y CREATE, restaurar el trigger desde respaldo antes de habilitar escrituras.
DROP TRIGGER cm_payment_status_history_bu;;
CREATE TRIGGER cm_payment_status_history_bu BEFORE UPDATE ON `payment_status_history` FOR EACH ROW
BEGIN
    IF NOT ((NEW.previous_status IS NULL AND NEW.new_status = 'PENDIENTE') OR (NEW.previous_status IS NOT NULL AND NEW.previous_status IN ('PENDIENTE','APROBADO','RECHAZADO') AND NEW.previous_status <> NEW.new_status AND NEW.new_status IN ('APROBADO','RECHAZADO'))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_transition';
    END IF;
    IF NOT (NEW.new_status <> 'RECHAZADO' OR (NEW.reason IS NOT NULL AND CHAR_LENGTH(TRIM(NEW.reason)) > 0)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_rejection';
    END IF;
END;;
--rollback DROP TRIGGER cm_payment_status_history_bu;;
--rollback CREATE TRIGGER cm_payment_status_history_bu BEFORE UPDATE ON `payment_status_history` FOR EACH ROW BEGIN     IF NOT ((NEW.previous_status IS NULL AND NEW.new_status = 'PENDIENTE') OR (NEW.previous_status IS NOT NULL AND NEW.previous_status = 'PENDIENTE' AND NEW.new_status IN ('APROBADO','RECHAZADO'))) THEN         SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_transition';     END IF;     IF NOT (NEW.new_status <> 'RECHAZADO' OR (NEW.reason IS NOT NULL AND CHAR_LENGTH(TRIM(NEW.reason)) > 0)) THEN         SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_rejection';     END IF; END;;
