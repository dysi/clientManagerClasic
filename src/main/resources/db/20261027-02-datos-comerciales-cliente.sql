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
