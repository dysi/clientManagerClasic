-- Migracion consolidada para la integracion a develop: 11 scripts en orden original.
-- Ejecucion manual UNA SOLA VEZ sobre el esquema previo al 26/10/2026, sin estas
-- migraciones aplicadas ni columnas creadas por ddl-auto=update.
-- Si la base ya esta parcial o totalmente migrada, NO ejecutar el archivo completo:
-- revisar esquema y registro de ejecucion para determinar los cambios pendientes.
-- Precondiciones: MySQL 5.7.24, InnoDB, IDs BIGINT con signo, nombres fisicos
-- minusculos, esquema seleccionado, aplicacion detenida y backup recuperable.
-- Probar primero sobre una copia compatible de MySQL.
-- ATENCION: la seccion 02 elimina TODOS los pagos del modelo anterior.
-- Se conservan las precondiciones y verificaciones particulares de cada seccion.
-- Usar cliente compatible con DELIMITER; detenerse al primer error, sin --force.
-- DDL hace COMMIT implicito: sin rollback global ni reintento seguro desde el inicio.
-- Al finalizar revisar resultados y arrancar con ddl-auto=validate.
-- No se ejecuta automaticamente al iniciar Spring Boot.
-- Los nombres originales citados son referencias a secciones de este mismo archivo.

-- Consolidado para MySQL 5.7.24: ejecutar SOLO en bases originales sin migrar.
-- Sustituye la revision anterior para MySQL 8; no repetir sobre una copia ya migrada.
-- Para la copia ya migrada, revisar el esquema y usar únicamente el archivo 04.
-- Los CHECK se sustituyen por diez triggers al final; mantener la aplicación detenida
-- hasta terminar todo el archivo. Requiere permisos TRIGGER además de DDL/rutinas.
SELECT DATABASE() AS esquema_destino, VERSION() AS version_motor;
DELIMITER $$
CREATE PROCEDURE cm_preflight_mysql57()
BEGIN
    DECLARE legacy_fk_count INT;
    IF DATABASE() IS NULL OR VERSION() NOT LIKE '5.7.%' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Esta variante requiere esquema seleccionado y MySQL 5.7';
    END IF;
    IF @@innodb_large_prefix = 0 OR @@innodb_file_per_table = 0 OR @@innodb_file_format <> 'Barracuda' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Requiere InnoDB large_prefix, file_per_table y formato Barracuda para indices utf8mb4';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
        AND ((table_name IN ('user','client','license') AND column_name='creationdate')
        OR (table_name='payment' AND column_name IN ('client_id','modificationdate')))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Base parcial o totalmente migrada: NO repetir consolidado ni vaciado';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
        AND table_name IN ('user','client','license','payment','licensestatus') AND engine='InnoDB') <> 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Se requieren cinco tablas originales InnoDB';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE()
        AND table_name IN ('bank','bank_account','client_account_balance','payment_status_history',
        'comment','attachment','payment_comment','payment_attachment','payment_license_action',
        'angrybox_sync_state','angrybox_sync_batch','angrybox_payment_delivery','license_extension_certificate')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Existen tablas nuevas: revisar migracion previa antes de cualquier borrado';
    END IF;
    SELECT COUNT(*) INTO legacy_fk_count
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE() AND table_name = 'payment'
        AND column_name = 'license_id' AND referenced_table_schema = DATABASE()
        AND referenced_table_name = 'license' AND referenced_column_name = 'id'
        AND constraint_name IN (SELECT constraint_name FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE() AND table_name = 'payment'
            GROUP BY constraint_name HAVING COUNT(*) = 1);
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='payment' AND column_name IN
        ('license_id','amount','discount','paymentdate','paymentrealdate','expirationdate','observation')) <> 7 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'payment no tiene las siete columnas del modelo antiguo: revisar esquema';
    END IF;
    -- Algunas bases historicas conservan el indice pero no la FK. Se admite su ausencia;
    -- una FK compuesta, duplicada o hacia otro destino sigue siendo incompatible.
    IF legacy_fk_count > 1 OR (SELECT COUNT(*) FROM information_schema.key_column_usage
        WHERE table_schema=DATABASE() AND table_name='payment' AND column_name='license_id'
        AND referenced_table_name IS NOT NULL) <> legacy_fk_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FK de payment.license_id incompatible: solo se admite una simple a license.id o ninguna';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.key_column_usage
        WHERE referenced_table_schema=DATABASE() AND referenced_table_name='payment') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hay dependencias de payment: revisar antes de cualquier cambio';
    END IF;
END$$
DELIMITER ;
CALL cm_preflight_mysql57();
DROP PROCEDURE cm_preflight_mysql57;

-- BEGIN 20261026-01-auditoria-entidades.sql
-- Seccion 01 de 11
-- Auditoria comun: Payment, LicenseStatus, Client, License y User.
-- Ejecutar manualmente ANTES de arrancar la version nueva de la aplicacion.
-- No es un script de inicializacion automatica de Spring Boot.
--
-- Precondiciones: backup verificado, aplicacion detenida, esquema correcto seleccionado,
-- las cinco tablas existentes con nombres fisicos en minuscula, motor InnoDB y user.id
-- BIGINT con signo (como el Long del modelo). Revisar los resultados de estas consultas;
-- si no coinciden, adaptar un nuevo cambio al esquema real antes de continuar.
-- MySQL hace COMMIT implicito de DDL: los ALTER no forman una transaccion global.
-- Aplicar una sola vez, por fecha y numero secuencial. No usar --force para ignorar errores.
-- No se inventan fechas ni usuarios para registros historicos: los campos nuevos son NULL.
-- Se conservan payment/licensestatus.creationdate y .removedate sin sobrescribir datos.
-- Los indices de las FKs son creados por InnoDB. RESTRICT conserva la identidad del actor.

SELECT DATABASE() AS esquema_destino;
SELECT table_name, engine
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('user', 'client', 'license', 'payment', 'licensestatus');
SELECT column_name, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'id';

ALTER TABLE `user`
    ADD COLUMN `creationdate` DATETIME(6) NULL,
    ADD COLUMN `modificationdate` DATETIME(6) NULL,
    ADD COLUMN `removedate` DATETIME(6) NULL,
    ADD COLUMN `created_by_id` BIGINT NULL,
    ADD COLUMN `modified_by_id` BIGINT NULL,
    ADD COLUMN `removed_by_id` BIGINT NULL,
    ADD CONSTRAINT `fk_user_created_by` FOREIGN KEY (`created_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_user_modified_by` FOREIGN KEY (`modified_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_user_removed_by` FOREIGN KEY (`removed_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `client`
    ADD COLUMN `creationdate` DATETIME(6) NULL,
    ADD COLUMN `modificationdate` DATETIME(6) NULL,
    ADD COLUMN `removedate` DATETIME(6) NULL,
    ADD COLUMN `created_by_id` BIGINT NULL,
    ADD COLUMN `modified_by_id` BIGINT NULL,
    ADD COLUMN `removed_by_id` BIGINT NULL,
    ADD CONSTRAINT `fk_client_created_by` FOREIGN KEY (`created_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_client_modified_by` FOREIGN KEY (`modified_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_client_removed_by` FOREIGN KEY (`removed_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `license`
    ADD COLUMN `creationdate` DATETIME(6) NULL,
    ADD COLUMN `modificationdate` DATETIME(6) NULL,
    ADD COLUMN `removedate` DATETIME(6) NULL,
    ADD COLUMN `created_by_id` BIGINT NULL,
    ADD COLUMN `modified_by_id` BIGINT NULL,
    ADD COLUMN `removed_by_id` BIGINT NULL,
    ADD CONSTRAINT `fk_license_created_by` FOREIGN KEY (`created_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_license_modified_by` FOREIGN KEY (`modified_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_license_removed_by` FOREIGN KEY (`removed_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `payment`
    ADD COLUMN `modificationdate` DATETIME(6) NULL,
    ADD COLUMN `created_by_id` BIGINT NULL,
    ADD COLUMN `modified_by_id` BIGINT NULL,
    ADD COLUMN `removed_by_id` BIGINT NULL,
    ADD CONSTRAINT `fk_payment_created_by` FOREIGN KEY (`created_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_payment_modified_by` FOREIGN KEY (`modified_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_payment_removed_by` FOREIGN KEY (`removed_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `licensestatus`
    ADD COLUMN `modificationdate` DATETIME(6) NULL,
    ADD COLUMN `created_by_id` BIGINT NULL,
    ADD COLUMN `modified_by_id` BIGINT NULL,
    ADD COLUMN `removed_by_id` BIGINT NULL,
    ADD CONSTRAINT `fk_licensestatus_created_by` FOREIGN KEY (`created_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_licensestatus_modified_by` FOREIGN KEY (`modified_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT `fk_licensestatus_removed_by` FOREIGN KEY (`removed_by_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

-- Verificacion: seis columnas por tabla (30 filas), y tres FKs de auditoria por tabla (15 filas).
SELECT table_name, column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('user', 'client', 'license', 'payment', 'licensestatus')
  AND column_name IN ('creationdate', 'modificationdate', 'removedate', 'created_by_id', 'modified_by_id', 'removed_by_id')
ORDER BY table_name, ordinal_position;
SELECT table_name, constraint_name, referenced_table_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND constraint_name IN ('fk_user_created_by', 'fk_user_modified_by', 'fk_user_removed_by',
      'fk_client_created_by', 'fk_client_modified_by', 'fk_client_removed_by',
      'fk_license_created_by', 'fk_license_modified_by', 'fk_license_removed_by',
      'fk_payment_created_by', 'fk_payment_modified_by', 'fk_payment_removed_by',
      'fk_licensestatus_created_by', 'fk_licensestatus_modified_by', 'fk_licensestatus_removed_by')
ORDER BY table_name, constraint_name;
-- END 20261026-01-auditoria-entidades.sql

-- BEGIN 20261026-02-vaciar-pagos-antiguos.sql
-- Seccion 02 de 11
-- OPERACION DESTRUCTIVA AUTORIZADA: descarta TODOS los pagos del modelo anterior.
-- Ejecutar manualmente con aplicacion detenida, backup comprobado y esquema seleccionado.
-- Precondicion: MySQL 5.7.24, InnoDB, migracion 20261026-01 aplicada.
-- El esquema versionado no tiene tablas dependientes de payment. No existen comprobantes
-- ni comentarios antiguos. Si aparecen FKs entrantes, ABORTAR y revisar esas dependencias:
-- no se eliminan registros ajenos ni se deshabilitan claves foraneas.
-- Orden: 01 auditoria (si corresponde), 02 vaciado, 03 estructura. No usar --force.
-- DELETE es transaccional en InnoDB; los CREATE/DROP PROCEDURE hacen COMMIT implicito.
-- No ejecutar sobre una base compartida como parte de las pruebas.
SELECT DATABASE() AS esquema_destino;
SELECT COUNT(*) AS pagos_a_descartar FROM payment;
DELIMITER $$
CREATE PROCEDURE cm_check_legacy_payment_dependencies()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.key_column_usage
        WHERE referenced_table_schema = DATABASE() AND referenced_table_name = 'payment') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hay dependencias entrantes a payment: revisar antes del vaciado';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE()
        AND table_name = 'payment' AND engine <> 'InnoDB') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'payment debe usar InnoDB';
    END IF;
END$$
DELIMITER ;
CALL cm_check_legacy_payment_dependencies();
DROP PROCEDURE cm_check_legacy_payment_dependencies;
START TRANSACTION;
SET @cm_clients_before = (SELECT COUNT(*) FROM client);
SET @cm_users_before = (SELECT COUNT(*) FROM user);
SET @cm_licenses_before = (SELECT COUNT(*) FROM license);
SET @cm_states_before = (SELECT COUNT(*) FROM licensestatus);
DELETE FROM payment;
SELECT COUNT(*) AS pagos_restantes FROM payment;
SELECT (SELECT COUNT(*) FROM client) = @cm_clients_before AS clientes_conservados,
       (SELECT COUNT(*) FROM user) = @cm_users_before AS usuarios_conservados,
       (SELECT COUNT(*) FROM license) = @cm_licenses_before AS licencias_conservadas,
       (SELECT COUNT(*) FROM licensestatus) = @cm_states_before AS estados_conservados;
COMMIT;
-- END 20261026-02-vaciar-pagos-antiguos.sql

-- BEGIN 20261026-03-modelo-portal-pagos.sql
-- Seccion 03 de 11
-- Estructura del portal: ejecutar una vez DESPUES del vaciado 20261026-02.
-- Precondiciones: MySQL 5.7.24 (validaciones mediante triggers al final), InnoDB, nombres fisicos minusculos,
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
        AND column_name = 'license_id' AND referenced_table_schema = DATABASE()
        AND referenced_table_name = 'license' AND referenced_column_name = 'id'
        AND constraint_name IN (SELECT constraint_name FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE() AND table_name = 'payment'
            GROUP BY constraint_name HAVING COUNT(*) = 1);
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='payment' AND column_name IN
        ('license_id','amount','discount','paymentdate','paymentrealdate','expirationdate','observation')) <> 7
        OR EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE()
            AND table_name='payment' AND column_name='client_id') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'payment no corresponde al modelo antiguo: no repetir migracion';
    END IF;
    IF legacy_license_fk_columns > 1 OR (SELECT COUNT(*) FROM information_schema.key_column_usage
        WHERE table_schema=DATABASE() AND table_name='payment' AND column_name='license_id'
        AND referenced_table_name IS NOT NULL) <> legacy_license_fk_columns THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FK de payment.license_id incompatible: solo se admite una simple a license.id o ninguna';
    END IF;
    -- Si existe, reemplaza la FK sin asumir el nombre ni conservar un posible CASCADE.
    -- Si falta, la FK compuesta se crea igualmente en el ALTER posterior, con payment vacia.
    IF legacy_license_fk_columns = 1 THEN
        SET @cm_drop_legacy_fk = CONCAT('ALTER TABLE payment DROP FOREIGN KEY `', REPLACE(legacy_license_fk, '`', '``'), '`');
        PREPARE cm_drop_fk_statement FROM @cm_drop_legacy_fk;
        EXECUTE cm_drop_fk_statement;
        DEALLOCATE PREPARE cm_drop_fk_statement;
    END IF;
END$$
DELIMITER ;
CALL cm_require_empty_payment();
DROP PROCEDURE cm_require_empty_payment;
ALTER TABLE client ADD COLUMN dni VARCHAR(8) NULL;
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
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

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
    CONSTRAINT uk_bank_account_alias UNIQUE (alias)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

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
    CONSTRAINT uk_client_account_balance_client_id UNIQUE (client_id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

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
    ADD CONSTRAINT fk_payment_license_client FOREIGN KEY (license_id, client_id) REFERENCES license(id, client_id) ON DELETE RESTRICT ON UPDATE RESTRICT;

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
    CONSTRAINT uk_payment_history_state UNIQUE (payment_id, new_status)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

-- Verificacion del esquema final: tablas, dinero decimal, FKs y ausencia de saldos ficticios.
SELECT table_name, engine FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN ('bank','bank_account','client_account_balance','payment','payment_status_history','comment','attachment','payment_comment','payment_attachment','payment_license_action');
SELECT table_name, column_name, column_type, is_nullable FROM information_schema.columns
WHERE table_schema = DATABASE() AND ((table_name IN ('payment','client_account_balance') AND column_name IN ('amount','client_id','license_id')) OR (table_name = 'client' AND column_name = 'dni'));
SELECT table_name, constraint_name, referenced_table_name, delete_rule
FROM information_schema.referential_constraints WHERE constraint_schema = DATABASE();
SELECT COUNT(*) AS pagos FROM payment;
SELECT COUNT(*) AS saldos_sincronizados FROM client_account_balance;
-- END 20261026-03-modelo-portal-pagos.sql

-- BEGIN 20261027-01-descripcion-y-agregado-adjuntos.sql
-- Seccion 04 de 11
-- Alcance: descripciones opcionales e identificación de envíos adicionales de comprobantes.
-- Precondiciones: copia de MySQL/InnoDB con 20261026-03-modelo-portal-pagos.sql aplicado,
-- tablas attachment y payment_attachment existentes; columnas nuevas aún ausentes.
-- Ejecutar manualmente primero en una copia y respaldar antes de aplicar en el destino.
-- Conserva filas, archivos, relaciones y auditoría. NULL en upload_key identifica los adjuntos
-- originales; los agregados posteriores llevan la clave del envío para reintentos idempotentes.
-- Las descripciones históricas desconocidas permanecen NULL.
-- DDL realiza COMMIT implícito: no hay rollback transaccional global.
SELECT DATABASE();
ALTER TABLE attachment ADD COLUMN description VARCHAR(1000) NULL;
ALTER TABLE payment_attachment ADD COLUMN upload_key VARCHAR(64) NULL,
    ADD INDEX ix_payment_attachment_upload (payment_id, upload_key);
-- Verificación: ambas columnas deben existir con los tipos y nulabilidad indicados.
SELECT table_name, column_name, column_type, is_nullable
FROM information_schema.columns WHERE table_schema = DATABASE()
AND ((table_name = 'attachment' AND column_name = 'description')
  OR (table_name = 'payment_attachment' AND column_name = 'upload_key'));
SHOW INDEX FROM payment_attachment WHERE Key_name = 'ix_payment_attachment_upload';
-- END 20261027-01-descripcion-y-agregado-adjuntos.sql

-- BEGIN 20261027-02-datos-comerciales-cliente.sql
-- Seccion 05 de 11
-- Alcance: campos opcionales de identificación comercial de Client.
-- Precondiciones: MySQL/InnoDB, tabla client existente, columnas nuevas aún ausentes.
-- Tipos: legal_name/trade_name VARCHAR(255), external_code VARCHAR(100), todos NULL.
-- Ejecutar manualmente primero en una copia compatible y respaldar el destino.
-- Conserva filas, relaciones y auditoría; los datos históricos desconocidos quedan NULL.
-- external_code no es único: no se impone una regla de integración aún no definida.
-- DDL hace COMMIT implícito; no existe rollback transaccional global.
-- Aplicación única: no repetir si ddl-auto=update ya agregó estas columnas.
SELECT DATABASE();
SELECT ENGINE FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'client';
SELECT column_name, column_type, is_nullable FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'client'
AND column_name IN ('legal_name', 'trade_name', 'external_code');
SET @client_rows_before = (SELECT COUNT(*) FROM client);

ALTER TABLE client
    ADD COLUMN legal_name VARCHAR(255) NULL,
    ADD COLUMN trade_name VARCHAR(255) NULL,
    ADD COLUMN external_code VARCHAR(100) NULL;

-- Verificación: tres columnas con esos tipos, IS_NULLABLE = YES y mismo total de filas.
SELECT column_name, column_type, is_nullable FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'client'
AND column_name IN ('legal_name', 'trade_name', 'external_code');
SELECT @client_rows_before AS rows_before, COUNT(*) AS rows_after,
       COUNT(*) = @client_rows_before AS rows_preserved FROM client;
-- END 20261027-02-datos-comerciales-cliente.sql

-- BEGIN 20261028-01-sincronizacion-clientes-angrybox.sql
-- Seccion 06 de 11
-- Recepción Angrybox: dos tablas nuevas, sin alterar clientes, saldos, licencias ni pagos históricos.
-- Precondiciones: MySQL 5.7.24, InnoDB, nombres físicos minúsculos y IDs BIGINT con signo.
-- Requiere las migraciones de auditoría y modelo de portal anteriores.
-- Aplicación detenida, copia/backup verificado. Ejecutar una vez; sin --force.
-- DDL hace COMMIT implícito: no promete rollback global. No se ejecuta automáticamente.
-- Tras migrar, validar esquema antes de activar angrybox.sync.enabled.
SELECT DATABASE() AS esquema_destino;

CREATE TABLE angrybox_sync_state (
    company_id BIGINT NOT NULL PRIMARY KEY,
    last_data_at DATETIME(6) NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT fk_ab_state_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ab_state_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ab_state_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

CREATE TABLE angrybox_sync_batch (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    sync_uuid VARCHAR(36) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    data_at DATETIME(6) NOT NULL,
    matching_mode VARCHAR(32) NOT NULL,
    code_target VARCHAR(32) NOT NULL,
    response_json LONGTEXT NOT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    CONSTRAINT uk_angrybox_sync_company_uuid UNIQUE (company_id, sync_uuid),
    CONSTRAINT fk_ab_batch_source FOREIGN KEY (company_id) REFERENCES angrybox_sync_state(company_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ab_batch_created FOREIGN KEY (created_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ab_batch_modified FOREIGN KEY (modified_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_ab_batch_removed FOREIGN KEY (removed_by_id) REFERENCES user(id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

-- Verificaciones: ambas tablas vacías y saldo existente conservado al desplegar.
SHOW CREATE TABLE angrybox_sync_state;
SHOW CREATE TABLE angrybox_sync_batch;
SELECT COUNT(*) AS fuentes FROM angrybox_sync_state;
SELECT COUNT(*) AS lotes FROM angrybox_sync_batch;
-- No insertar saldos cero ni atribuir auditoría histórica al operador de la migración.
-- END 20261028-01-sincronizacion-clientes-angrybox.sql

-- BEGIN 20261029-01-visibilidad-licencias.sql
-- Seccion 07 de 11
-- Alcance: visibilidad de licencias, independiente del bloqueo y de la baja del cliente.
-- Precondiciones: MySQL/InnoDB, tabla license existente; hidden todavía no existe.
-- Ejecutar manualmente primero en una copia compatible, con respaldo del destino.
-- No repetir si ddl-auto=update ya creó hidden; comprobar el esquema antes de aplicar.
-- Conserva todas las filas, estados, relaciones y auditoría. Las licencias existentes son visibles.
-- La baja de Client usa removedate/removed_by_id existentes: no necesita columnas nuevas.
-- MySQL realiza COMMIT implícito de DDL; no hay rollback transaccional global.
SELECT DATABASE();
SELECT ENGINE FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'license';
SELECT column_name, column_type, is_nullable, column_default FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'license' AND column_name = 'hidden';
SET @license_rows_before = (SELECT COUNT(*) FROM license);

ALTER TABLE license ADD COLUMN hidden BIT(1) NOT NULL DEFAULT b'0';

-- Verificación: hidden BIT(1), NOT NULL, default 0 y mismo total de filas.
SELECT column_name, column_type, is_nullable, column_default FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'license' AND column_name = 'hidden';
SELECT @license_rows_before AS rows_before, COUNT(*) AS rows_after,
       COUNT(*) = @license_rows_before AS rows_preserved FROM license;
SELECT COUNT(*) AS hidden_licenses FROM license WHERE hidden = b'1';
-- END 20261029-01-visibilidad-licencias.sql

-- BEGIN 20261030-01-entrega-pagos-angrybox.sql
-- Seccion 08 de 11
-- MySQL 5.7.24, InnoDB; ejecutar una vez con aplicación detenida y backup verificado.
-- Requiere el modelo de pagos y auditoría ya instalado. No modifica Payment ni saldos.
CREATE TABLE angrybox_payment_delivery (
    payment_id BIGINT NOT NULL PRIMARY KEY,
    delivered_at DATETIME(6) NULL,
    company_id BIGINT NULL,
    movement_id BIGINT NULL,
    payload_hash VARCHAR(64) NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    KEY ix_payment_delivery_pending (delivered_at,payment_id),
    CONSTRAINT fk_ab_delivery_payment FOREIGN KEY (payment_id) REFERENCES payment(id),
    CONSTRAINT fk_ab_delivery_created FOREIGN KEY (created_by_id) REFERENCES user(id),
    CONSTRAINT fk_ab_delivery_modified FOREIGN KEY (modified_by_id) REFERENCES user(id),
    CONSTRAINT fk_ab_delivery_removed FOREIGN KEY (removed_by_id) REFERENCES user(id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

-- Habilita también los aprobados históricos; la fecha mínima se aplica en cada descarga.
-- No atribuir la aprobación ni la creación histórica al operador de esta migración.
INSERT INTO angrybox_payment_delivery (payment_id,creationdate)
SELECT id,CURRENT_TIMESTAMP(6) FROM payment WHERE status='APROBADO' AND removedate IS NULL;
-- END 20261030-01-entrega-pagos-angrybox.sql

-- BEGIN 20261030-02-certificados-extension-licencias.sql
-- Seccion 09 de 11
-- Certificados temporales por cliente. Ejecución manual, una sola vez, antes del arranque.
-- Precondiciones: MySQL 5.7.24, InnoDB, tablas client/payment/user y auditoría instaladas,
-- IDs BIGINT con signo y aplicación detenida. El generador AUTO con
-- spring.jpa.hibernate.use-new-id-generator-mappings=false usa AUTO_INCREMENT en MySQL.
-- Probar primero en una copia compatible con backup verificado. No crea certificados históricos
-- ni cambia pagos, saldos o vencimientos. El DDL hace COMMIT implícito, sin rollback global.
SELECT DATABASE() AS esquema_destino;
SELECT table_name, engine FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN ('client','payment','user');
SELECT table_name, column_name, column_type FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name IN ('client','payment','user') AND column_name = 'id';

CREATE TABLE license_extension_certificate (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    recovered_by_payment_id BIGINT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    UNIQUE KEY uk_extension_payment (payment_id),
    KEY ix_extension_client_latest (client_id, id),
    CONSTRAINT fk_extension_client FOREIGN KEY (client_id) REFERENCES client(id),
    CONSTRAINT fk_extension_payment FOREIGN KEY (payment_id) REFERENCES payment(id),
    CONSTRAINT fk_extension_recovery FOREIGN KEY (recovered_by_payment_id) REFERENCES payment(id),
    CONSTRAINT fk_extension_created FOREIGN KEY (created_by_id) REFERENCES user(id),
    CONSTRAINT fk_extension_modified FOREIGN KEY (modified_by_id) REFERENCES user(id),
    CONSTRAINT fk_extension_removed FOREIGN KEY (removed_by_id) REFERENCES user(id)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC DEFAULT CHARSET=utf8mb4;

-- Verificación inicial: estructura y cero certificados, sin atribuir eventos históricos.
SHOW CREATE TABLE license_extension_certificate;
SELECT COUNT(*) AS certificados_iniciales FROM license_extension_certificate;
-- Verificación operativa: debe devolver cero filas.
SELECT c.id FROM license_extension_certificate c JOIN payment p ON p.id=c.payment_id
WHERE c.client_id <> p.client_id OR c.expires_at <= c.starts_at;
-- END 20261030-02-certificados-extension-licencias.sql

-- BEGIN 20261030-03-correccion-pagos-pendientes.sql
-- Seccion 10 de 11
-- Corrección administrativa de pagos pendientes. Ejecución manual antes del arranque.
-- Precondiciones: MySQL 5.7.24, InnoDB, tabla payment existente; aplicación detenida.
-- Probar primero sobre una copia compatible con backup verificado.
-- Conserva los datos históricos: NULL utiliza los valores actuales para idempotencia.
-- La primera corrección guarda la huella original, sin alterar la clave de presentación.
-- El DDL hace COMMIT implícito; no existe rollback transaccional global.
SELECT DATABASE() AS esquema_destino;
SHOW CREATE TABLE payment;
ALTER TABLE payment ADD COLUMN original_editable_hash VARCHAR(64) NULL;
-- Verificación: columna nullable y todos los registros históricos con NULL inicialmente.
SHOW COLUMNS FROM payment LIKE 'original_editable_hash';
SELECT COUNT(*) AS huellas_iniciales FROM payment WHERE original_editable_hash IS NOT NULL;
-- END 20261030-03-correccion-pagos-pendientes.sql

-- BEGIN 20261031-01-correccion-destino-bancario-pagos.sql
-- Seccion 11 de 11
-- Corrección administrativa del destino de pagos pendientes. Ejecución manual antes del arranque.
-- Precondiciones: MySQL 5.7.24, InnoDB, tabla payment existente y script
-- 20261030-03-correccion-pagos-pendientes.sql aplicado; aplicación detenida.
-- Probar primero en una copia compatible con backup verificado. Conserva los datos históricos.
-- NULL compara la fotografía actual; la primera corrección del destino conserva su huella original.
-- No atribuye auditoría histórica ni cambia importes, cuentas o comprobantes existentes.
-- MySQL hace COMMIT implícito en DDL: no existe rollback transaccional global.
SELECT DATABASE() AS esquema_destino;
SHOW CREATE TABLE payment;
ALTER TABLE payment ADD COLUMN original_destination_hash VARCHAR(64) NULL;
-- Verificación inicial: columna nullable y cero huellas anteriores a las correcciones.
SHOW COLUMNS FROM payment LIKE 'original_destination_hash';
SELECT COUNT(*) AS huellas_iniciales FROM payment WHERE original_destination_hash IS NOT NULL;
-- END 20261031-01-correccion-destino-bancario-pagos.sql


-- Compatibilidad MySQL 5.7.24: reemplazo efectivo de los CHECK ignorados por 5.7.
-- Aplicar UNA SOLA VEZ sobre el modelo de portal ya creado; NO borra pagos ni altera columnas.
-- También se incluye al final de 20261102-01-migracion-consolidada.sql:
-- si se ejecutó ese consolidado nuevo, NO ejecutar este archivo por separado.
-- Precondiciones: aplicación detenida, backup recuperable, InnoDB, IDs BIGINT con signo,
-- esquema seleccionado y secciones 01 a 11 del consolidado anterior terminadas.
-- No completa una migración parcial: revisar primero tablas/columnas y registro de errores.
-- Requiere CREATE ROUTINE, EXECUTE y TRIGGER; con binlog pueden requerirse permisos adicionales.
-- Probar primero en copia MySQL 5.7.24; no aplicar a una base compartida por esta entrega.
-- Conserva filas y auditoría. Si hay datos inválidos, aborta SIN corregirlos automáticamente.
-- El cliente DEBE detenerse al primer error, sin --force. DDL hace COMMIT implícito.
-- No hay rollback global. No ejecutar si ya existen estos triggers.
SELECT DATABASE() AS esquema_destino, VERSION() AS version_motor;
DELIMITER $$
CREATE PROCEDURE cm_validate_mysql57_rules()
BEGIN
    IF DATABASE() IS NULL OR VERSION() NOT LIKE '5.7.%' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Seleccionar esquema MySQL 5.7 antes de continuar';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
        AND table_name='payment' AND column_name IN ('original_editable_hash','original_destination_hash')) <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Completar y revisar primero las secciones 01 a 11; no repetir el consolidado';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()
        AND table_name IN ('client','bank_account','client_account_balance','payment','payment_status_history')
        AND engine='InnoDB') <> 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Se requieren las cinco tablas de validacion InnoDB';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.key_column_usage WHERE table_schema=DATABASE()
        AND table_name='payment' AND constraint_name='fk_payment_license_client'
        AND referenced_table_schema=DATABASE() AND referenced_table_name='license'
        AND ((ordinal_position=1 AND column_name='license_id' AND referenced_column_name='id')
        OR (ordinal_position=2 AND column_name='client_id' AND referenced_column_name='client_id'))) <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Falta FK compuesta payment-license: revisar migracion parcial';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.triggers WHERE trigger_schema=DATABASE()
        AND trigger_name IN ('cm_client_bi','cm_client_bu','cm_bank_account_bi','cm_bank_account_bu',
        'cm_client_account_balance_bi','cm_client_account_balance_bu','cm_payment_bi','cm_payment_bu',
        'cm_payment_status_history_bi','cm_payment_status_history_bu')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existen triggers cm_: revisar ejecucion previa, no reemplazarlos';
    END IF;
    IF EXISTS (SELECT 1 FROM `client` WHERE NOT (dni IS NULL OR dni REGEXP '^[0-9]{1,8}$')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Datos existentes invalidos: ck_client_dni';
    END IF;
    IF EXISTS (SELECT 1 FROM `bank_account` WHERE NOT (cbu REGEXP '^[0-9]{22}$')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Datos existentes invalidos: ck_account_cbu';
    END IF;
    IF EXISTS (SELECT 1 FROM `client_account_balance` WHERE NOT (currency = 'ARS')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Datos existentes invalidos: ck_balance_currency';
    END IF;
    IF EXISTS (SELECT 1 FROM `payment` WHERE NOT (amount > 0)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Datos existentes invalidos: ck_payment_amount';
    END IF;
    IF EXISTS (SELECT 1 FROM `payment` WHERE NOT (currency = 'ARS')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Datos existentes invalidos: ck_payment_currency';
    END IF;
    IF EXISTS (SELECT 1 FROM `payment` WHERE NOT (method = 'TRANSFERENCIA')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Datos existentes invalidos: ck_payment_method';
    END IF;
    IF EXISTS (SELECT 1 FROM `payment` WHERE NOT (status IN ('PENDIENTE', 'APROBADO', 'RECHAZADO'))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Datos existentes invalidos: ck_payment_status';
    END IF;
    IF EXISTS (SELECT 1 FROM `payment` WHERE NOT ((without_receipt = 1 AND no_receipt_reason IS NOT NULL AND CHAR_LENGTH(TRIM(no_receipt_reason)) > 0) OR (without_receipt = 0 AND (no_receipt_reason IS NULL OR CHAR_LENGTH(TRIM(no_receipt_reason)) = 0)))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Datos existentes invalidos: ck_payment_receipt_reason';
    END IF;
    IF EXISTS (SELECT 1 FROM `payment_status_history` WHERE NOT ((previous_status IS NULL AND new_status = 'PENDIENTE') OR (previous_status IS NOT NULL AND previous_status = 'PENDIENTE' AND new_status IN ('APROBADO','RECHAZADO')))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Datos existentes invalidos: ck_history_transition';
    END IF;
    IF EXISTS (SELECT 1 FROM `payment_status_history` WHERE NOT (new_status <> 'RECHAZADO' OR (reason IS NOT NULL AND CHAR_LENGTH(TRIM(reason)) > 0))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Datos existentes invalidos: ck_history_rejection';
    END IF;
END$$
DELIMITER ;
CALL cm_validate_mysql57_rules();
DROP PROCEDURE cm_validate_mysql57_rules;

DELIMITER $$
CREATE TRIGGER cm_client_bi BEFORE INSERT ON `client` FOR EACH ROW
BEGIN
    IF NOT (NEW.dni IS NULL OR NEW.dni REGEXP '^[0-9]{1,8}$') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_client_dni';
    END IF;
END$$
CREATE TRIGGER cm_client_bu BEFORE UPDATE ON `client` FOR EACH ROW
BEGIN
    IF NOT (NEW.dni IS NULL OR NEW.dni REGEXP '^[0-9]{1,8}$') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_client_dni';
    END IF;
END$$
CREATE TRIGGER cm_bank_account_bi BEFORE INSERT ON `bank_account` FOR EACH ROW
BEGIN
    IF NOT (NEW.cbu REGEXP '^[0-9]{22}$') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_account_cbu';
    END IF;
END$$
CREATE TRIGGER cm_bank_account_bu BEFORE UPDATE ON `bank_account` FOR EACH ROW
BEGIN
    IF NOT (NEW.cbu REGEXP '^[0-9]{22}$') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_account_cbu';
    END IF;
END$$
CREATE TRIGGER cm_client_account_balance_bi BEFORE INSERT ON `client_account_balance` FOR EACH ROW
BEGIN
    IF NOT (NEW.currency = 'ARS') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_balance_currency';
    END IF;
END$$
CREATE TRIGGER cm_client_account_balance_bu BEFORE UPDATE ON `client_account_balance` FOR EACH ROW
BEGIN
    IF NOT (NEW.currency = 'ARS') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_balance_currency';
    END IF;
END$$
CREATE TRIGGER cm_payment_bi BEFORE INSERT ON `payment` FOR EACH ROW
BEGIN
    IF NOT (NEW.amount > 0) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_payment_amount';
    END IF;
    IF NOT (NEW.currency = 'ARS') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_payment_currency';
    END IF;
    IF NOT (NEW.method = 'TRANSFERENCIA') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_payment_method';
    END IF;
    IF NOT (NEW.status IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_payment_status';
    END IF;
    IF NOT ((NEW.without_receipt = 1 AND NEW.no_receipt_reason IS NOT NULL AND CHAR_LENGTH(TRIM(NEW.no_receipt_reason)) > 0) OR (NEW.without_receipt = 0 AND (NEW.no_receipt_reason IS NULL OR CHAR_LENGTH(TRIM(NEW.no_receipt_reason)) = 0))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_payment_receipt_reason';
    END IF;
END$$
CREATE TRIGGER cm_payment_bu BEFORE UPDATE ON `payment` FOR EACH ROW
BEGIN
    IF NOT (NEW.amount > 0) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_payment_amount';
    END IF;
    IF NOT (NEW.currency = 'ARS') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_payment_currency';
    END IF;
    IF NOT (NEW.method = 'TRANSFERENCIA') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_payment_method';
    END IF;
    IF NOT (NEW.status IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_payment_status';
    END IF;
    IF NOT ((NEW.without_receipt = 1 AND NEW.no_receipt_reason IS NOT NULL AND CHAR_LENGTH(TRIM(NEW.no_receipt_reason)) > 0) OR (NEW.without_receipt = 0 AND (NEW.no_receipt_reason IS NULL OR CHAR_LENGTH(TRIM(NEW.no_receipt_reason)) = 0))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_payment_receipt_reason';
    END IF;
END$$
CREATE TRIGGER cm_payment_status_history_bi BEFORE INSERT ON `payment_status_history` FOR EACH ROW
BEGIN
    IF NOT ((NEW.previous_status IS NULL AND NEW.new_status = 'PENDIENTE') OR (NEW.previous_status IS NOT NULL AND NEW.previous_status = 'PENDIENTE' AND NEW.new_status IN ('APROBADO','RECHAZADO'))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_transition';
    END IF;
    IF NOT (NEW.new_status <> 'RECHAZADO' OR (NEW.reason IS NOT NULL AND CHAR_LENGTH(TRIM(NEW.reason)) > 0)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_rejection';
    END IF;
END$$
CREATE TRIGGER cm_payment_status_history_bu BEFORE UPDATE ON `payment_status_history` FOR EACH ROW
BEGIN
    IF NOT ((NEW.previous_status IS NULL AND NEW.new_status = 'PENDIENTE') OR (NEW.previous_status IS NOT NULL AND NEW.previous_status = 'PENDIENTE' AND NEW.new_status IN ('APROBADO','RECHAZADO'))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_transition';
    END IF;
    IF NOT (NEW.new_status <> 'RECHAZADO' OR (NEW.reason IS NOT NULL AND CHAR_LENGTH(TRIM(NEW.reason)) > 0)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ck_history_rejection';
    END IF;
END$$
DELIMITER ;
-- Verificacion: diez triggers (INSERT y UPDATE en cada una de las cinco tablas).
SELECT event_object_table, trigger_name, action_timing, event_manipulation
FROM information_schema.triggers WHERE trigger_schema=DATABASE() AND trigger_name IN
('cm_client_bi','cm_client_bu','cm_bank_account_bi','cm_bank_account_bu',
'cm_client_account_balance_bi','cm_client_account_balance_bu','cm_payment_bi','cm_payment_bu',
'cm_payment_status_history_bi','cm_payment_status_history_bu') ORDER BY event_object_table, trigger_name;
