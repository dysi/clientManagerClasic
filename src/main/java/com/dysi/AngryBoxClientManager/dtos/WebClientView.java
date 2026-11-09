package com.dysi.AngryBoxClientManager.dtos;

import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.ClientAccountBalance;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fotografía externa de solo lectura para la administración web. */
public class WebClientView extends ClientDTO {
    public String street;
    public String streetNumber;
    public String addressComplement;
    public String neighborhood;
    public String city;
    public String postalCode;
    public String observations;
    public Long countryId;
    public String countryName;
    public Long provinceId;
    public String provinceName;
    public boolean removed;
    public Balance accountBalance;
    public WebClientView(Client client, ClientAccountBalance balance) {
        super(client.getId(), client.getFirstname(), client.getLastname(), client.getEmail(),
                client.getPhone(), client.getCuil(), client.getUser() == null ? null : client.getUser().getUsername(),
                client.getLegalName(), client.getTradeName(), client.getExternalCode());
        setDni(client.getDni());
        street = client.getStreet();
        streetNumber = client.getStreetNumber();
        addressComplement = client.getAddressComplement();
        neighborhood = client.getNeighborhood();
        city = client.getCity();
        postalCode = client.getPostalCode();
        observations = client.getObservations();
        countryId = client.getCountry() == null ? null : client.getCountry().getId();
        countryName = client.getCountry() == null ? null : client.getCountry().getName();
        provinceId = client.getProvince() == null ? null : client.getProvince().getId();
        provinceName = client.getProvince() == null ? null : client.getProvince().getName();

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
