-- Corrección administrativa de pagos pendientes. Ejecución manual antes del arranque.
-- Precondiciones: MySQL 8, InnoDB, tabla payment existente; aplicación detenida.
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
