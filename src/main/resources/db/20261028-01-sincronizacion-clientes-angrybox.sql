-- Recepción Angrybox: dos tablas nuevas, sin alterar clientes, saldos, licencias ni pagos históricos.
-- Precondiciones: MySQL 8.0.16+, InnoDB, nombres físicos minúsculos y IDs BIGINT con signo.
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verificaciones: ambas tablas vacías y saldo existente conservado al desplegar.
SHOW CREATE TABLE angrybox_sync_state;
SHOW CREATE TABLE angrybox_sync_batch;
SELECT COUNT(*) AS fuentes FROM angrybox_sync_state;
SELECT COUNT(*) AS lotes FROM angrybox_sync_batch;
-- No insertar saldos cero ni atribuir auditoría histórica al operador de la migración.
