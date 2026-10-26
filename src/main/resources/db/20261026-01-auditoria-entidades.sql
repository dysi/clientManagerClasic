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
