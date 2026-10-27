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
