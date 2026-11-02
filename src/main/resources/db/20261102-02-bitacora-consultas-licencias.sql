-- Bitácora de consultas de licencia v1/v2. Ejecución manual antes del arranque.
-- Precondiciones: MySQL 5.7/8, InnoDB, esquema consolidado aplicado, tabla user con id BIGINT con signo.
-- Probar primero sobre una copia compatible con backup verificado; no ejecutado en MySQL en esta entrega.
-- Crea una tabla vacía, sin modificar licencias, usuarios ni datos históricos existentes.
-- No atribuye eventos históricos ni reconstruye consultas anteriores al despliegue.
-- El DDL hace COMMIT implícito: no hay rollback transaccional global.
SELECT DATABASE() AS esquema_destino;
SHOW CREATE TABLE user;

CREATE TABLE license_query_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    received_at DATETIME(6) NOT NULL,
    license_id BIGINT NULL,
    company_id BIGINT NULL,
    company_name VARCHAR(255) NULL,
    authenticated_username VARCHAR(50) NULL,
    requested_username VARCHAR(150) NULL,
    remote_ip VARCHAR(64) NOT NULL,
    user_agent VARCHAR(300) NULL,
    http_method VARCHAR(16) NOT NULL,
    api_version VARCHAR(2) NOT NULL,
    http_status INT NOT NULL,
    duration_ms BIGINT NOT NULL,
    forced_lock BIT NULL,
    lock_date_epoch_ms BIGINT NULL,
    installation_id VARCHAR(100) NULL,
    process_id VARCHAR(100) NULL,
    hostname VARCHAR(255) NULL,
    app_version VARCHAR(100) NULL,
    operating_system VARCHAR(100) NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    KEY ix_license_query_received (received_at, id),
    KEY ix_license_query_company (company_id, received_at),
    KEY ix_license_query_license (license_id, received_at),
    KEY ix_license_query_ip (remote_ip, received_at),
    CONSTRAINT fk_license_query_created FOREIGN KEY (created_by_id) REFERENCES user(id),
    CONSTRAINT fk_license_query_modified FOREIGN KEY (modified_by_id) REFERENCES user(id),
    CONSTRAINT fk_license_query_removed FOREIGN KEY (removed_by_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- license_id y company_id son fotografías escalares sin FK: un intento puede no validar licencia.
-- La purga programada elimina físicamente solo esta tabla, en lotes por received_at.
-- Auditoría sin cascadas sobre user; no almacenar JWT, contraseñas ni cuerpos completos.
SHOW CREATE TABLE license_query_log;
SELECT COUNT(*) AS consultas_iniciales FROM license_query_log;
-- Verificación operativa posterior al despliegue (esperado: cero filas):
SELECT id FROM license_query_log
WHERE received_at IS NULL OR http_status < 100 OR http_status > 599 OR duration_ms < 0;
