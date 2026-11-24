package com.dysi.AngryBoxClientManager.servicesImpl;
import com.dysi.AngryBoxClientManager.dtos.ClientInput;
import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.*;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
@Transactional(readOnly = true)
public class ClientAddressService {
    private final CountryRepository countries;
    private final ProvinceRepository provinces;
    public ClientAddressService(CountryRepository countries, ProvinceRepository provinces) {
        this.countries = countries; this.provinces = provinces;
    }
    public static class Place {
        public final Long id;
        public final String name;
        public final Long countryId;
        public Place(Long id, String name, Long countryId) {
            this.id = id; this.name = name; this.countryId = countryId;
        }
    }
    public List<Place> countries() {
        return countries.findByRemoveDateIsNullOrderByNameAsc().stream()
                .map(c -> new Place(c.getId(), c.getName(), null)).collect(Collectors.toList());
    }
    public List<Place> provinces() {
        return provinces.findByRemoveDateIsNullOrderByNameAsc().stream()
                .filter(p -> p.getCountry().getRemoveDate() == null)
                .map(p -> new Place(p.getId(), p.getName(), p.getCountry().getId())).collect(Collectors.toList());
    }
    /** Participa de la transacción del guardado; omitir un campo conserva su valor. */
    public void fill(Client client, ClientInput input) {
        Country country = client.getCountry();
        Province province = client.getProvince();
        if (input.countryId != null) country = input.countryId == 0 ? null : countries.findById(input.countryId)
                .filter(c -> c.getRemoveDate() == null).orElseThrow(() -> invalid("País inválido."));
        if (input.provinceId != null) province = input.provinceId == 0 ? null : provinces.findById(input.provinceId)
                .filter(p -> p.getRemoveDate() == null).orElseThrow(() -> invalid("Provincia inválida."));
        if (province != null && (country == null || !province.getCountry().getId().equals(country.getId())))
            throw invalid("La provincia no pertenece al país seleccionado.");
        client.setCountry(country); client.setProvince(province);
        if (input.street != null) client.setStreet(input.street);
        if (input.streetNumber != null) client.setStreetNumber(input.streetNumber);
        if (input.addressComplement != null) client.setAddressComplement(input.addressComplement);
        if (input.neighborhood != null) client.setNeighborhood(input.neighborhood);
        if (input.city != null) client.setCity(input.city);
        if (input.postalCode != null) client.setPostalCode(input.postalCode);
        if (input.observations != null) client.setObservations(input.observations);
    }
    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
