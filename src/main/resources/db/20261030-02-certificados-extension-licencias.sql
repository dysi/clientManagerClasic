-- Certificados temporales por cliente. Ejecución manual, una sola vez, antes del arranque.
-- Precondiciones: MySQL 8, InnoDB, tablas client/payment/user y auditoría instaladas,
-- IDs BIGINT con signo y aplicación detenida. El generador AUTO con
-- spring.jpa.hibernate.use-new-id-generator-mappings=false usa AUTO_INCREMENT en MySQL.
-- Probar primero en una copia compatible con backup verificado. No crea certificados históricos
-- ni cambia pagos, saldos o vencimientos. El DDL hace COMMIT implícito, sin rollback global.
SELECT DATABASE() AS esquema_destino;
SELECT table_name, engine FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN ('client','payment','user');
SELECT table_name, column_name, column_type FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name IN ('client','payment','user') AND column_name = 'id';

CREATE TABLE license_extension_certificate (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    client_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    starts_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    recovered_by_payment_id BIGINT NULL,
    creationdate DATETIME(6) NULL,
    modificationdate DATETIME(6) NULL,
    removedate DATETIME(6) NULL,
    created_by_id BIGINT NULL,
    modified_by_id BIGINT NULL,
    removed_by_id BIGINT NULL,
    UNIQUE KEY uk_extension_payment (payment_id),
    KEY ix_extension_client_latest (client_id, id),
    CONSTRAINT fk_extension_client FOREIGN KEY (client_id) REFERENCES client(id),
    CONSTRAINT fk_extension_payment FOREIGN KEY (payment_id) REFERENCES payment(id),
    CONSTRAINT fk_extension_recovery FOREIGN KEY (recovered_by_payment_id) REFERENCES payment(id),
    CONSTRAINT fk_extension_created FOREIGN KEY (created_by_id) REFERENCES user(id),
    CONSTRAINT fk_extension_modified FOREIGN KEY (modified_by_id) REFERENCES user(id),
    CONSTRAINT fk_extension_removed FOREIGN KEY (removed_by_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Verificación inicial: estructura y cero certificados, sin atribuir eventos históricos.
SHOW CREATE TABLE license_extension_certificate;
SELECT COUNT(*) AS certificados_iniciales FROM license_extension_certificate;
-- Verificación operativa: debe devolver cero filas.
SELECT c.id FROM license_extension_certificate c JOIN payment p ON p.id=c.payment_id
WHERE c.client_id <> p.client_id OR c.expires_at <= c.starts_at;
