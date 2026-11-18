package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.WebClientView;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.ClientAccountBalance;
import com.dysi.AngryBoxClientManager.repositories.ClientRepository;
import com.dysi.AngryBoxClientManager.repositories.ClientAccountBalanceRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lectura de saldos sin alterar la fotografía externa ni cargar todo el padrón. */
@Service
@Transactional(readOnly = true)
public class WebClientReadService {
    private final ClientRepository clients;
    private final ClientAccountBalanceRepository balances;
    private final com.dysi.AngryBoxClientManager.repositories.LicenseRepository licenses;
    private final com.dysi.AngryBoxClientManager.repositories.UserRepository users;

    private final ZoneId balanceZone;

    public WebClientReadService(ClientRepository clients, ClientAccountBalanceRepository balances,
            com.dysi.AngryBoxClientManager.repositories.UserRepository users,
            com.dysi.AngryBoxClientManager.repositories.LicenseRepository licenses,
            @Value("${portal.review.time-zone:America/Argentina/Buenos_Aires}") String balanceZone) {
        this.clients = clients;
        this.balances = balances;
        this.users = users;
        this.licenses = licenses;
        this.balanceZone = ZoneId.of(balanceZone);
    }

    public Page<WebClientView> list(String keyword, String balanceState, Integer staleDays, int page, int size) {
        return list(keyword, balanceState, "ALL", staleDays, page, size);
    }

    public Page<WebClientView> list(String keyword, String balanceState, String clientStatus, Integer staleDays, int page, int size) {
        String escaped = keyword.trim().toLowerCase(java.util.Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        Page<Client> result = clients.searchForWeb("%" + escaped + "%", balanceState, clientStatus,
                staleDays == null ? null : LocalDateTime.now(balanceZone).minusDays(staleDays),
                PageRequest.of(page, size, Sort.by("lastname", "firstname", "id")));
        Map<Long, ClientAccountBalance> byClient = new HashMap<>();
        if (!result.isEmpty()) balances.findByClientIdInAndRemoveDateIsNull(result.getContent().stream()
                .map(Client::getId).collect(Collectors.toList()))
                .forEach(balance -> byClient.put(balance.getClient().getId(), balance));
        Map<Long, Long> counts = new HashMap<>();
        if (!result.isEmpty()) users.countForClients(result.getContent().stream().map(Client::getId).collect(Collectors.toList()))
                .forEach(count -> counts.put(count.getClientId(), count.getTotal()));
        Map<Long, WebClientView.LicenseCounts> licenseCounts = new HashMap<>();
        if (!result.isEmpty()) licenses.countForClients(result.getContent().stream().map(Client::getId).collect(Collectors.toList()), tomorrow())
                .forEach(count -> licenseCounts.put(count.getClientId(), new WebClientView.LicenseCounts(count)));
        return result.map(client -> {
            WebClientView view = new WebClientView(client, byClient.get(client.getId()));
            view.userCount = counts.getOrDefault(client.getId(), 0L);
            view.licenseCounts = licenseCounts.getOrDefault(client.getId(), new WebClientView.LicenseCounts());
            return view;
        });
    }

    /** La baja reutiliza el evento y actor de auditoría, sin alterar licencias. */
    @Transactional
    public WebClientView status(long id, boolean removed) {
        Client client = clients.lockPortalClient(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "No se encontró el cliente."));
        if (removed) client.markRemoved(); else client.restore();
        clients.saveAndFlush(client);
        return view(client);
    }

    public WebClientView view(Client client) {
        WebClientView view = new WebClientView(client, balances.findByClientIdAndRemoveDateIsNull(client.getId()).orElse(null));
        view.userCount = users.countForClients(java.util.List.of(client.getId())).stream()
                .mapToLong(com.dysi.AngryBoxClientManager.repositories.UserRepository.ClientUserCount::getTotal).findFirst().orElse(0);
        licenses.countForClients(java.util.List.of(client.getId()), tomorrow()).stream().findFirst()
                .ifPresent(count -> view.licenseCounts = new WebClientView.LicenseCounts(count));
        return view;
    }

    private static java.util.Calendar tomorrow() {
        return java.util.GregorianCalendar.from(java.time.LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()));
    }
}
