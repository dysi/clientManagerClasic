-- OPERACION DESTRUCTIVA AUTORIZADA: descarta TODOS los pagos del modelo anterior.
-- Ejecutar manualmente con aplicacion detenida, backup comprobado y esquema seleccionado.
-- Precondicion: MySQL 8.0.16+, InnoDB, migracion 20261026-01 aplicada.
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
