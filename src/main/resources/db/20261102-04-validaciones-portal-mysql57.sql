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
