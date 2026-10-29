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
