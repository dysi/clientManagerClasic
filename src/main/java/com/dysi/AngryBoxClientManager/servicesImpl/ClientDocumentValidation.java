package com.dysi.AngryBoxClientManager.servicesImpl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Reglas del flujo web; no reescribe documentos históricos. */
@Service
public class ClientDocumentValidation {
    public static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[ .-]", "");
    }

    public static boolean validCuit(String value) {
        String digits = normalize(value);
        if (value == null || !value.matches("[0-9 .-]+") || !digits.matches("[0-9]{11}")) return false;
        if (!java.util.Arrays.asList("20", "23", "24", "27", "30", "33", "34").contains(digits.substring(0, 2))) return false;
        int[] weights = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < weights.length; i++) sum += (digits.charAt(i) - '0') * weights[i];
        int check = 11 - sum % 11;
        if (check == 11) check = 0;
        return check < 10 && check == digits.charAt(10) - '0';
    }

    /** Usa el DNI conservado cuando un consumidor anterior lo omite. */
    public void validate(String cuit, String dni) {
        if (cuit == null || cuit.trim().isEmpty()) return;
        if (!validCuit(cuit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresá un CUIL / CUIT válido, de once dígitos y con dígito verificador correcto.");
        }
        if (dni != null && !dni.isEmpty()
                && !normalize(cuit).substring(2, 10).equals(String.format("%8s", dni).replace(' ', '0'))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El CUIL / CUIT debe contener el DNI del cliente en sus ocho dígitos centrales.");
        }
    }
}
