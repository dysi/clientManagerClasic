-- Estructura del portal: ejecutar una vez DESPUES del vaciado 20261026-02.
-- Precondiciones: MySQL 8.0.16+ (CHECK efectivos), InnoDB, nombres fisicos minusculos,
-- IDs BIGINT con signo y auditoria 20261026-01 aplicada.
-- Tablas nuevas: AUTO_INCREMENT, coherente con AUTO y los mapeos heredados configurados.
-- Ejecutar solo sobre el esquema previo a esta migracion; no repetir en bases ya migradas.
-- Aplicacion detenida, backup verificado. Arrancar luego con ddl-auto=validate.
-- No ejecutar con --force. El cliente SQL debe detenerse ante el primer error.
-- DDL produce COMMIT implicito: no hay rollback global. Recuperar desde copia si falla.
-- Conserva client.cuil y phone tal como estan; DNI nuevo queda NULL, sin inferencias.
-- No inserta saldos, usuarios, cuentas, pagos ni atribuciones historicas.
SELECT DATABASE() AS esquema_destino;
DELIMITER $$
CREATE PROCEDURE cm_require_empty_payment()
BEGIN
    DECLARE legacy_license_fk VARCHAR(64);
    DECLARE legacy_license_fk_columns INT;
    IF EXISTS (SELECT 1 FROM payment LIMIT 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ejecutar y verificar primero el vaciado autorizado de pagos';
    END IF;
    SELECT COUNT(*), MAX(constraint_name) INTO legacy_license_fk_columns, legacy_license_fk
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'payment'
        AND column_name = 'license_id' AND referenced_table_name = 'license' AND referenced_column_name = 'id';
    IF legacy_license_fk_columns <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Se esperaba una FK payment.license_id a license.id: revisar esquema';
    END IF;
    -- Reemplaza el nombre generado por Hibernate, sin asumirlo ni conservar un posible CASCADE.
    SET @cm_drop_legacy_fk = CONCAT('ALTER TABLE payment DROP FOREIGN KEY `', REPLACE(legacy_license_fk, '`', '``'), '`');
    PREPARE cm_drop_fk_statement FROM @cm_drop_legacy_fk;
    EXECUTE cm_drop_fk_statement;
    DEALLOCATE PREPARE cm_drop_fk_statement;
END$$
DELIMITER ;
CALL cm_require_empty_payment();
DROP PROCEDURE cm_require_empty_payment;
ALTER TABLE client ADD COLUMN dni VARCHAR(8) NULL,
    ADD CONSTRAINT ck_client_dni CHECK (dni IS NULL OR dni REGEXP '^[0-9]{1,8}$');
-- Permite reforzar la pertenencia del cliente tambien con FK compuesta.
ALTER TABLE license ADD CONSTRAINT uk_license_client UNIQUE (id, client_id);

CREATE TABLE `bank` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    active BIT(1) NOT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT fk_bank_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bank_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bank_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uk_bank_name UNIQUE (normalized_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `bank_account` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    bank_id BIGINT NOT NULL,
    holder VARCHAR(255) NOT NULL,
    cbu VARCHAR(22) NOT NULL,
    alias VARCHAR(100) NULL,
    active BIT(1) NOT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT fk_bank_account_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bank_account_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bank_account_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_bank_account_bank_id FOREIGN KEY (bank_id) REFERENCES `bank`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uk_bank_account_cbu UNIQUE (cbu),
    CONSTRAINT uk_bank_account_alias UNIQUE (alias),
    CONSTRAINT ck_account_cbu CHECK (cbu REGEXP '^[0-9]{22}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `client_account_balance` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    currency VARCHAR(32) NOT NULL,
    synchronized_at DATETIME(6) NOT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT fk_client_account_balance_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_client_account_balance_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_client_account_balance_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_client_account_balance_client_id FOREIGN KEY (client_id) REFERENCES `client`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uk_client_account_balance_client_id UNIQUE (client_id),
    CONSTRAINT ck_balance_currency CHECK (currency = 'ARS')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE payment
    DROP COLUMN discount,
    DROP COLUMN paymentdate,
    DROP COLUMN paymentrealdate,
    DROP COLUMN expirationdate,
    DROP COLUMN observation,
    MODIFY COLUMN amount DECIMAL(14,2) NOT NULL,
    MODIFY COLUMN license_id BIGINT NULL,
    ADD COLUMN client_id BIGINT NOT NULL,
    ADD COLUMN currency VARCHAR(32) NOT NULL,
    ADD COLUMN issued_at DATETIME(6) NOT NULL,
    ADD COLUMN contact_phone VARCHAR(50) NOT NULL,
    ADD COLUMN method VARCHAR(32) NOT NULL,
    ADD COLUMN bank_account_id BIGINT NOT NULL,
    ADD COLUMN destination_bank VARCHAR(255) NOT NULL,
    ADD COLUMN destination_holder VARCHAR(255) NOT NULL,
    ADD COLUMN destination_cbu VARCHAR(22) NOT NULL,
    ADD COLUMN destination_alias VARCHAR(100) NULL,
    ADD COLUMN status VARCHAR(32) NOT NULL,
    ADD COLUMN origin VARCHAR(32) NOT NULL,
    ADD COLUMN submitted_by_id BIGINT NOT NULL,
    ADD COLUMN without_receipt BIT(1) NOT NULL,
    ADD COLUMN no_receipt_reason VARCHAR(1000) NULL,
    ADD COLUMN submission_key VARCHAR(64) NOT NULL,
    ADD COLUMN version BIGINT NOT NULL,
    ADD CONSTRAINT fk_payment_client_id FOREIGN KEY (client_id) REFERENCES `client`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_payment_bank_account_id FOREIGN KEY (bank_account_id) REFERENCES `bank_account`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT fk_payment_submitted_by_id FOREIGN KEY (submitted_by_id) REFERENCES `user`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT uk_payment_submission UNIQUE (client_id, submission_key),
    ADD CONSTRAINT fk_payment_license_client FOREIGN KEY (license_id, client_id) REFERENCES license(id, client_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT ck_payment_amount CHECK (amount > 0),
    ADD CONSTRAINT ck_payment_currency CHECK (currency = 'ARS'),
    ADD CONSTRAINT ck_payment_method CHECK (method = 'TRANSFERENCIA'),
    ADD CONSTRAINT ck_payment_status CHECK (status IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    ADD CONSTRAINT ck_payment_receipt_reason CHECK ((without_receipt = 1 AND no_receipt_reason IS NOT NULL AND CHAR_LENGTH(TRIM(no_receipt_reason)) > 0) OR (without_receipt = 0 AND (no_receipt_reason IS NULL OR CHAR_LENGTH(TRIM(no_receipt_reason)) = 0)));

CREATE TABLE `payment_status_history` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    previous_status VARCHAR(32) NULL,
    new_status VARCHAR(32) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    responsible_user_id BIGINT NOT NULL,
    origin VARCHAR(32) NOT NULL,
    reason VARCHAR(1000) NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT fk_payment_status_history_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_status_history_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_status_history_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_status_history_payment_id FOREIGN KEY (payment_id) REFERENCES `payment`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_status_history_responsible_user_id FOREIGN KEY (responsible_user_id) REFERENCES `user`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uk_payment_history_state UNIQUE (payment_id, new_status),
    CONSTRAINT ck_history_transition CHECK ((previous_status IS NULL AND new_status = 'PENDIENTE') OR (previous_status IS NOT NULL AND previous_status = 'PENDIENTE' AND new_status IN ('APROBADO','RECHAZADO'))),
    CONSTRAINT ck_history_rejection CHECK (new_status <> 'RECHAZADO' OR (reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason)) > 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `comment` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    text VARCHAR(4000) NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    author_id BIGINT NOT NULL,
    origin VARCHAR(32) NOT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT fk_comment_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_comment_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_comment_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_comment_author_id FOREIGN KEY (author_id) REFERENCES `user`(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `attachment` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    storage_key VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    stored_size BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    uploaded_at DATETIME(6) NOT NULL,
    uploaded_by_id BIGINT NOT NULL,
    origin VARCHAR(32) NOT NULL,
    width INT NULL,
    height INT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT fk_attachment_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_attachment_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_attachment_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_attachment_uploaded_by_id FOREIGN KEY (uploaded_by_id) REFERENCES `user`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uk_attachment_storage UNIQUE (storage_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `payment_comment` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    comment_id BIGINT NOT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT fk_payment_comment_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_comment_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_comment_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_comment_payment_id FOREIGN KEY (payment_id) REFERENCES `payment`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_comment_comment_id FOREIGN KEY (comment_id) REFERENCES `comment`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uk_payment_comment_comment_id UNIQUE (comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `payment_attachment` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    attachment_id BIGINT NOT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT fk_payment_attachment_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_attachment_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_attachment_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_attachment_payment_id FOREIGN KEY (payment_id) REFERENCES `payment`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_attachment_attachment_id FOREIGN KEY (attachment_id) REFERENCES `attachment`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT uk_payment_attachment_attachment_id UNIQUE (attachment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `payment_license_action` (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    license_id BIGINT NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    previous_value VARCHAR(100) NULL,
    new_value VARCHAR(100) NOT NULL,
    responsible_user_id BIGINT NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT fk_payment_license_action_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_license_action_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_license_action_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_license_action_payment_id FOREIGN KEY (payment_id) REFERENCES `payment`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_license_action_license_id FOREIGN KEY (license_id) REFERENCES `license`(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_payment_license_action_responsible_user_id FOREIGN KEY (responsible_user_id) REFERENCES `user`(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verificacion del esquema final: tablas, dinero decimal, FKs y ausencia de saldos ficticios.
SELECT table_name, engine FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN ('bank','bank_account','client_account_balance','payment','payment_status_history','comment','attachment','payment_comment','payment_attachment','payment_license_action');
SELECT table_name, column_name, column_type, is_nullable FROM information_schema.columns
WHERE table_schema = DATABASE() AND ((table_name IN ('payment','client_account_balance') AND column_name IN ('amount','client_id','license_id')) OR (table_name = 'client' AND column_name = 'dni'));
SELECT table_name, constraint_name, referenced_table_name, delete_rule
FROM information_schema.referential_constraints WHERE constraint_schema = DATABASE();
SELECT COUNT(*) AS pagos FROM payment;
SELECT COUNT(*) AS saldos_sincronizados FROM client_account_balance;
