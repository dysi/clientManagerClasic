package com.dysi.AngryBoxClientManager.mail;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Configuración y plantillas con codificación explícita, también en archivos externos. */
@Configuration
@PropertySource(value = "${clientmanager.mail.config:classpath:mail.properties}", encoding = "UTF-8")
public class MailConfiguration { }
