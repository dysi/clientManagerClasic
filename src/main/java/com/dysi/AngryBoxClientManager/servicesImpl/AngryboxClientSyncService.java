package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.config.AngryboxSyncProperties;
import com.dysi.AngryBoxClientManager.dtos.AngryboxClientSync.*;
import com.dysi.AngryBoxClientManager.dtos.ClientSyncIdentity;
import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.*;
import com.dysi.AngryBoxClientManager.security.JwtUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Reemplaza la fotografía de deuda: no genera pagos, movimientos ni cambios de identidad. */
@Service
public class AngryboxClientSyncService {
    private final AngryboxSyncProperties settings;
    private final ClientRepository clients;
    private final ClientAccountBalanceRepository balances;
    private final AngryboxSyncStateRepository states;
    private final AngryboxSyncBatchRepository batches;
    private final UserRepository users;
    private final ObjectMapper json;
    private final EntityManager entityManager;
    private final ZoneId businessZone;

    public AngryboxClientSyncService(AngryboxSyncProperties settings, ClientRepository clients,
            ClientAccountBalanceRepository balances, AngryboxSyncStateRepository states,
            AngryboxSyncBatchRepository batches, UserRepository users, ObjectMapper json,
            EntityManager entityManager,
            @Value("${portal.review.time-zone:America/Argentina/Buenos_Aires}") String businessZone) {
        this.settings = settings;
        this.clients = clients;
        this.balances = balances;
        this.states = states;
        this.batches = batches;
        this.users = users;
        this.json = json;
        this.entityManager = entityManager;
        this.businessZone = ZoneId.of(businessZone);
    }

    @PreAuthorize("hasAuthority(@angryboxSyncProperties.requiredAuthority.name())")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Response synchronize(String username, Request input) {
        authorize(username, input.companyId);
        validateEnvelope(input);
        String fingerprint = fingerprint(input);
        String uuid = input.sincronizacionId.toLowerCase(Locale.ROOT);
        AngryboxSyncState state = states.lockSource(input.companyId).orElseGet(() -> createState(input.companyId));
        Optional<AngryboxSyncBatch> previous = batches.findByCompanyIdAndSyncUuid(input.companyId, uuid);
        if (previous.isPresent()) {
            if (!previous.get().getRequestHash().equals(fingerprint)) {
                throw conflict("El identificador de sincronización ya se usó con otros datos. "
                        + "Conservá el envío original o generá una nueva fotografía con otro UUID.");
            }
            return readResponse(previous.get().getResponseJson());
        }
        Instant dataAt = input.fechaDatos.toInstant().truncatedTo(ChronoUnit.MICROS);
        if (state.getLastDataAt() != null && !dataAt.isAfter(state.getLastDataAt())) {
            throw conflict("La fotografía enviada no es posterior a la última procesada ("
                    + state.getLastDataAt().atZone(businessZone).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + "). Volvé a consultar los clientes en Angrybox y enviá un nuevo lote.");
        }
        List<ClientSyncIdentity> directory = clients.findSyncIdentities();
        AngryboxClientMatcher matcher = new AngryboxClientMatcher(directory, settings.getCodeTarget(), settings.getMatchingMode());
        Response response = new Response();
        response.sincronizacionId = input.sincronizacionId;
        Set<Long> related = new HashSet<>();
        Map<Long, List<Result>> assignments = new TreeMap<>();
        Map<Long, ClientSyncIdentity> targets = new HashMap<>();
        for (IncomingClient incoming : input.clientes) {
            Result row = new Result(incoming);
            response.clientesIn.add(row);
            validateAmount(row);
            AngryboxClientMatcher.Resolution match = matcher.resolve(incoming,
                    settings.isSyncDeactivations() && Boolean.TRUE.equals(incoming.baja));
            row.errores.addAll(match.errors);
            related.addAll(match.relatedIds);
            if (match.target != null) {
                row.clientManagerId = match.target.id;
                row.criterioCoincidencia = settings.getMatchingMode().name() + "/" + settings.getCodeTarget().name();
                assignments.computeIfAbsent(match.target.id, ignored -> new ArrayList<>()).add(row);
                targets.put(match.target.id, match.target);
            }
        }
        // Todos los conflictos se resuelven antes de escribir: nunca gana la última fila.
        for (Map.Entry<Long, List<Result>> assignment : assignments.entrySet()) {
            List<Result> rows = assignment.getValue();
            if (rows.size() > 1) {
                String ids = rows.stream().map(row -> row.id.toString()).limit(20)
                        .collect(java.util.stream.Collectors.joining(", "));
                for (Result row : rows) {
                    row.errores.add("Los clientes Angrybox [" + ids + "] apuntan al mismo ClientManager: "
                            + AngryboxClientMatcher.describe(targets.get(assignment.getKey()))
                            + ". Corregí los códigos/documentos duplicados; ninguna de estas filas actualiza la deuda.");
                }
            }
        }
        // Orden estable compartido con los flujos que bloquean cliente antes de sus operaciones.
        for (Map.Entry<Long, List<Result>> assignment : assignments.entrySet()) {
            if (assignment.getValue().size() != 1) {
                continue;
            }
            Result row = assignment.getValue().get(0);
            if (!row.errores.isEmpty()) {
                continue;
            }
            applyBalance(row, targets.get(assignment.getKey()), dataAt);
        }
        for (ClientSyncIdentity client : directory) {
            if (!client.removed && !related.contains(client.id)) {
                response.clientesOut.add(external(client));
            }
        }
        AngryboxSyncBatch batch = new AngryboxSyncBatch();
        batch.setCompanyId(input.companyId);
        batch.setSyncUuid(uuid);
        batch.setRequestHash(fingerprint);
        batch.setDataAt(dataAt);
        batch.setMatchingMode(settings.getMatchingMode().name());
        batch.setCodeTarget(settings.getCodeTarget().name());
        batch.setResponseJson(writeResponse(response));
        batches.save(batch);
        state.setLastDataAt(dataAt);
        states.save(state);
        // La respuesta persistida, el cursor y todos los saldos se confirman o revierten juntos.
        entityManager.flush();
        return response;
    }

    private void authorize(String username, Long companyId) {
        if (!settings.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La recepción de clientes desde Angrybox está desactivada.");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUser)
                || !authentication.getName().equals(username)
                || !Objects.equals(settings.getSourceCompanyId(), companyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario o la empresa no están autorizados como origen de la sincronización.");
        }
        JwtUser principal = (JwtUser) authentication.getPrincipal();
        User actor = users.findById(principal.getId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario de integración no está disponible."));
        if (actor.getRemoveDate() != null || !Boolean.TRUE.equals(actor.getEnabled())
                || actor.getAuthorities().stream().noneMatch(role -> role.getName() == settings.getRequiredAuthority())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La integración requiere un usuario activo con el permiso " + settings.getRequiredAuthority().name() + ".");
        }
    }

    private void validateEnvelope(Request input) {
        Set<Long> ids = new HashSet<>();
        for (IncomingClient client : input.clientes) {
            if (client == null || client.id == null || client.id <= 0 || !ids.add(client.id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada cliente enviado debe tener un ID Angrybox positivo y no repetido. No se procesó el lote.");
            }
        }
        if (input.fechaDatos.toInstant().isAfter(Instant.now().plusSeconds(settings.getMaxFutureSeconds()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de los datos está en el futuro. Revisá el reloj y la zona horaria del servidor Angrybox.");
        }
    }

    private AngryboxSyncState createState(Long companyId) {
        AngryboxSyncState state = new AngryboxSyncState();
        state.setCompanyId(companyId);
        states.saveAndFlush(state);
        // Dos primeras solicitudes simultáneas pueden competir por el PK; una debe reintentarse.
        return states.lockSource(companyId).orElseThrow(() -> conflict("No se pudo reservar el origen de sincronización."));
    }

    private void validateAmount(Result row) {
        if (row.deudaCuentaCorriente == null) {
            row.errores.add("Angrybox no envió deudaCuentaCorriente. Revisá su cuenta corriente; no se supone una deuda cero.");
            return;
        }
        try {
            row.deudaCuentaCorriente = row.deudaCuentaCorriente.setScale(2, RoundingMode.UNNECESSARY);
            if (row.deudaCuentaCorriente.abs().compareTo(new BigDecimal("1000000000000")) >= 0) {
                row.errores.add("La deuda excede los 12 dígitos enteros admitidos. Revisá el importe en Angrybox.");
            }
        } catch (ArithmeticException precision) {
            row.errores.add("La deuda tiene más de dos decimales significativos. Corregí el importe enviado por Angrybox.");
        }
    }

    private void applyBalance(Result row, ClientSyncIdentity identity, Instant dataAt) {
        Optional<Client> locked = clients.lockPortalClient(identity.id);
        if (locked.isEmpty()) {
            row.errores.add("El cliente de ClientManager fue anulado o eliminado durante la sincronización. Revisá su ficha.");
            return;
        }
        Client client = locked.get();
        entityManager.refresh(client, LockModeType.PESSIMISTIC_WRITE);
        if (!Objects.equals(identity.dni, client.getDni()) || !Objects.equals(identity.cuil, client.getCuil())
                || !Objects.equals(identity.externalCode, client.getExternalCode())
                || identity.removed != (client.getRemoveDate() != null)) {
            row.errores.add("Los identificadores del cliente cambiaron durante la sincronización. Actualizá los datos y reenviá un nuevo lote.");
            return;
        }
        ClientAccountBalance balance = balances.findByClientId(identity.id).orElseGet(ClientAccountBalance::new);
        if (balance.getRemoveDate() != null) {
            row.errores.add("La fotografía de saldo está anulada en ClientManager. Pedí a Soporte revisar ese registro; "
                    + "no se reactiva ni se crea un saldo duplicado.");
            return;
        }
        balance.setClient(client);
        balance.setAmount(row.deudaCuentaCorriente);
        balance.setCurrency(com.dysi.AngryBoxClientManager.models.Currency.ARS);
        balance.setSynchronizedAt(LocalDateTime.ofInstant(dataAt, businessZone));
        balances.save(balance);
        // La baja explícita conserva deuda, relaciones y el primer evento de auditoría.
        // Nunca se reactiva un cliente por recibir baja=false o un campo ausente.
        if (settings.isSyncDeactivations() && Boolean.TRUE.equals(row.baja)) {
            client.markRemoved();
            clients.save(client);
        }
        row.sincronizo = true;
    }

    private ExternalClient external(ClientSyncIdentity client) {
        ExternalClient output = new ExternalClient();
        output.id = client.id;
        output.nombre = client.name();
        output.mail = client.email;
        output.celular = client.phone;
        AngryboxClientMatcher.Document tax = AngryboxClientMatcher.document(client.cuil);
        output.dni = client.dni;
        if ((output.dni == null || output.dni.isBlank()) && tax != null && !tax.tax) {
            output.dni = tax.value;
        }
        output.cuit = tax != null && tax.tax ? client.cuil : null;
        output.codigoExterno = client.externalCode;
        return output;
    }

    private String fingerprint(Request input) {
        try {
            ObjectNode tree = json.valueToTree(input);
            tree.put("fechaDatos", input.fechaDatos.toInstant().toString());
            for (JsonNode row : tree.withArray("clientes")) {
                JsonNode amount = row.get("deudaCuentaCorriente");
                if (amount != null && amount.isNumber()) {
                    ((ObjectNode) row).put("deudaCuentaCorriente", amount.decimalValue().stripTrailingZeros().toPlainString());
                }
            }
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json.writeValueAsString(tree).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("No se pudo identificar el lote de sincronización.", failure);
        }
    }

    private String writeResponse(Response response) {
        try {
            return json.writeValueAsString(response);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("No se pudo conservar el resultado de sincronización.", failure);
        }
    }

    private Response readResponse(String content) {
        try {
            return json.readValue(content, Response.class);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("No se pudo recuperar el resultado original de sincronización.", failure);
        }
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
