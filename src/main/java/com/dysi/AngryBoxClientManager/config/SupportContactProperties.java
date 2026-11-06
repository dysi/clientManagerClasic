package com.dysi.AngryBoxClientManager.config;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Contacto público de ayuda. El enlace usa solo dígitos del número internacional configurado. */
@Component
@Validated
@ConfigurationProperties(prefix = "portal.support")
public class SupportContactProperties {
    @NotBlank
    @Pattern(regexp = "\\+?[1-9](?:[ ()-]*[0-9]){6,14}", message = "Usá un número internacional de WhatsApp de 7 a 15 dígitos.")
    private String whatsappPhone;

    public String getWhatsappPhone() { return whatsappPhone; }
    public void setWhatsappPhone(String whatsappPhone) { this.whatsappPhone = whatsappPhone; }
    public String getWhatsappUrl() { return "https://wa.me/" + whatsappPhone.replaceAll("[^0-9]", ""); }
}
