-- Esquema anterior a la auditoria, reducido a las tablas y columnas mapeadas.
-- Fixture sintetico: exclusivamente para una instancia MySQL desechable y vacia.
CREATE TABLE hibernate_sequence (next_val BIGINT);
INSERT INTO hibernate_sequence VALUES (1000);
CREATE TABLE authority_seq (next_val BIGINT);
INSERT INTO authority_seq VALUES (1000);
CREATE TABLE user (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50), password VARCHAR(100),
 firstname VARCHAR(50), lastname VARCHAR(50), email VARCHAR(50), enabled BIT(1),
 lastpasswordresetdate DATETIME(6), client_id BIGINT
) ENGINE=InnoDB;
CREATE TABLE authority (id BIGINT NOT NULL PRIMARY KEY, name VARCHAR(50)) ENGINE=InnoDB;
CREATE TABLE user_authority (user_id BIGINT NOT NULL, authority_id BIGINT NOT NULL,
 FOREIGN KEY (user_id) REFERENCES user(id), FOREIGN KEY (authority_id) REFERENCES authority(id)) ENGINE=InnoDB;
CREATE TABLE client (id BIGINT NOT NULL PRIMARY KEY, firstname VARCHAR(255), lastname VARCHAR(255),
 email VARCHAR(255), phone VARCHAR(255), cuil VARCHAR(255), user_id BIGINT,
 FOREIGN KEY (user_id) REFERENCES user(id)) ENGINE=InnoDB;
ALTER TABLE user ADD FOREIGN KEY (client_id) REFERENCES client(id);
CREATE TABLE license (id BIGINT NOT NULL PRIMARY KEY, description VARCHAR(255), companyname VARCHAR(255),
 companyid BIGINT, initialactivity DATETIME(6), prepayment BIT(1), client_id BIGINT,
 FOREIGN KEY (client_id) REFERENCES client(id)) ENGINE=InnoDB;
CREATE TABLE licensestatus (id BIGINT NOT NULL PRIMARY KEY, observation VARCHAR(255), limittransaction BIGINT,
 forcedlock BIT(1), lockdate DATETIME(6), license_id BIGINT, creationdate DATETIME(6), removedate DATETIME(6),
 FOREIGN KEY (license_id) REFERENCES license(id)) ENGINE=InnoDB;
CREATE TABLE payment (id BIGINT NOT NULL PRIMARY KEY, discount DOUBLE, paymentdate DATETIME(6),
 paymentrealdate DATETIME(6), expirationdate DATETIME(6), amount DOUBLE, observation VARCHAR(255),
 license_id BIGINT, creationdate DATETIME(6), removedate DATETIME(6),
 FOREIGN KEY (license_id) REFERENCES license(id)) ENGINE=InnoDB;
INSERT INTO user (id,username) VALUES (1,'fixture');
INSERT INTO client(id,firstname,cuil,phone,user_id) VALUES (1,'Prueba','histórico sin clasificar','001234',1),(2,'Otro',NULL,NULL,NULL);
INSERT INTO license(id,companyid,client_id) VALUES (1,123,1);
INSERT INTO licensestatus(id,license_id) VALUES (1,1);
INSERT INTO payment(id,license_id,amount,discount) VALUES (1,1,100.25,10),(2,1,200,0);
