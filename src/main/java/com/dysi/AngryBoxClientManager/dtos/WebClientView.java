package com.dysi.AngryBoxClientManager.dtos;

import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.ClientAccountBalance;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fotografía externa de solo lectura para la administración web. */
public class WebClientView extends ClientDTO {
    public boolean removed;
    public Balance accountBalance;
    public WebClientView(Client client, ClientAccountBalance balance) {
        super(client.getId(), client.getFirstname(), client.getLastname(), client.getEmail(),
                client.getPhone(), client.getCuil(), client.getUser() == null ? null : client.getUser().getUsername(),
                client.getLegalName(), client.getTradeName(), client.getExternalCode());
        setDni(client.getDni());
        removed = client.getRemoveDate() != null;
        if (balance != null) accountBalance = new Balance(balance);
    }
    public static class Balance {
        public final Long id;
        public final Long clientId;
        public final BigDecimal amount;
        public final String currency;
        public final LocalDateTime synchronizedAt;
        public Balance(ClientAccountBalance balance) {
            id = balance.getId(); clientId = balance.getClient().getId(); amount = balance.getAmount();
            currency = balance.getCurrency().name(); synchronizedAt = balance.getSynchronizedAt();
        }
    }
}
