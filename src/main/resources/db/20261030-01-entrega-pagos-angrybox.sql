-- MySQL 8, InnoDB; ejecutar una vez con aplicación detenida y backup verificado.
-- Requiere el modelo de pagos y auditoría ya instalado. No modifica Payment ni saldos.
CREATE TABLE angrybox_payment_delivery (
    payment_id BIGINT NOT NULL PRIMARY KEY,
    delivered_at DATETIME(6) NULL,
    company_id BIGINT NULL,
    movement_id BIGINT NULL,
    payload_hash VARCHAR(64) NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    KEY ix_payment_delivery_pending (delivered_at,payment_id),
    CONSTRAINT fk_ab_delivery_payment FOREIGN KEY (payment_id) REFERENCES payment(id),
    CONSTRAINT fk_ab_delivery_created FOREIGN KEY (created_by_id) REFERENCES user(id),
    CONSTRAINT fk_ab_delivery_modified FOREIGN KEY (modified_by_id) REFERENCES user(id),
    CONSTRAINT fk_ab_delivery_removed FOREIGN KEY (removed_by_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Habilita también los aprobados históricos; la fecha mínima se aplica en cada descarga.
-- No atribuir la aprobación ni la creación histórica al operador de esta migración.
INSERT INTO angrybox_payment_delivery (payment_id,creationdate)
SELECT id,CURRENT_TIMESTAMP(6) FROM payment WHERE status='APROBADO' AND removedate IS NULL;
