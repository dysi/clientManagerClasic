-- Corrección administrativa del destino de pagos pendientes. Ejecución manual antes del arranque.
-- Precondiciones: MySQL 8, InnoDB, tabla payment existente y script
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
