--liquibase formatted sql

--changeset dysi:20261109-01-country dbms:mysql
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'country'
CREATE TABLE country (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name varchar(100) NOT NULL,
    creationdate datetime NULL, modificationdate datetime NULL, removedate datetime NULL,
    created_by_id bigint NULL, modified_by_id bigint NULL, removed_by_id bigint NULL,
    CONSTRAINT uk_country_name UNIQUE(name),
    CONSTRAINT fk_country_created FOREIGN KEY (created_by_id) REFERENCES user(id),
    CONSTRAINT fk_country_modified FOREIGN KEY (modified_by_id) REFERENCES user(id),
    CONSTRAINT fk_country_removed FOREIGN KEY (removed_by_id) REFERENCES user(id)
) ENGINE=InnoDB;
--rollback DROP TABLE country;

--changeset dysi:20261109-02-province dbms:mysql
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'province'
CREATE TABLE province (
    id bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name varchar(100) NOT NULL,
    creationdate datetime NULL, modificationdate datetime NULL, removedate datetime NULL,
    created_by_id bigint NULL, modified_by_id bigint NULL, removed_by_id bigint NULL,
    country_id bigint NOT NULL,
    CONSTRAINT fk_province_country FOREIGN KEY (country_id) REFERENCES country(id),
    CONSTRAINT uk_province_name UNIQUE(country_id, name),
    CONSTRAINT fk_province_created FOREIGN KEY (created_by_id) REFERENCES user(id),
    CONSTRAINT fk_province_modified FOREIGN KEY (modified_by_id) REFERENCES user(id),
    CONSTRAINT fk_province_removed FOREIGN KEY (removed_by_id) REFERENCES user(id)
) ENGINE=InnoDB;
--rollback DROP TABLE province;

--changeset dysi:20261109-03-argentina dbms:mysql
INSERT INTO country (name, creationdate, modificationdate) VALUES ('Argentina', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO province (name, country_id, creationdate, modificationdate)
SELECT 'Buenos Aires', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Ciudad Autónoma de Buenos Aires', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Catamarca', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Chaco', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Chubut', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Córdoba', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Corrientes', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Entre Ríos', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Formosa', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Jujuy', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'La Pampa', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'La Rioja', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Mendoza', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Misiones', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Neuquén', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Río Negro', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Salta', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'San Juan', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'San Luis', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Santa Cruz', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Santa Fe', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Santiago del Estero', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Tierra del Fuego, Antártida e Islas del Atlántico Sur', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina'
UNION ALL
SELECT 'Tucumán', id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM country WHERE name = 'Argentina';
--rollback DELETE FROM province WHERE country_id IN (SELECT id FROM country WHERE name = 'Argentina');
--rollback DELETE FROM country WHERE name = 'Argentina';

--changeset dysi:20261109-04-client-address dbms:mysql
--preconditions onFail:HALT onError:HALT
--precondition-sql-check expectedResult:1 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'client'
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'client' AND column_name IN ('street', 'street_number', 'address_complement', 'neighborhood', 'city', 'postal_code', 'observations', 'country_id', 'province_id')
ALTER TABLE client
    ADD COLUMN street varchar(255) NULL,
    ADD COLUMN street_number varchar(30) NULL,
    ADD COLUMN address_complement varchar(255) NULL,
    ADD COLUMN neighborhood varchar(255) NULL,
    ADD COLUMN city varchar(255) NULL,
    ADD COLUMN postal_code varchar(30) NULL,
    ADD COLUMN observations varchar(4000) NULL,
    ADD COLUMN country_id bigint NULL,
    ADD COLUMN province_id bigint NULL,
    ADD CONSTRAINT fk_client_country FOREIGN KEY (country_id) REFERENCES country(id),
    ADD CONSTRAINT fk_client_province FOREIGN KEY (province_id) REFERENCES province(id);
--rollback ALTER TABLE client DROP FOREIGN KEY fk_client_province, DROP FOREIGN KEY fk_client_country, DROP COLUMN province_id, DROP COLUMN country_id, DROP COLUMN street, DROP COLUMN street_number, DROP COLUMN address_complement, DROP COLUMN neighborhood, DROP COLUMN city, DROP COLUMN postal_code, DROP COLUMN observations;

