package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.config.AngryboxSyncProperties.CodeTarget;
import com.dysi.AngryBoxClientManager.config.AngryboxSyncProperties.MatchingMode;
import com.dysi.AngryBoxClientManager.dtos.AngryboxClientSync.IncomingClient;
import com.dysi.AngryBoxClientManager.dtos.ClientSyncIdentity;
import java.util.*;

/** Índice de una fotografía del padrón. No elige arbitrariamente entre coincidencias. */
public final class AngryboxClientMatcher {
    private final CodeTarget codeTarget;
    private final MatchingMode mode;
    private final Map<String, Map<Long, ClientSyncIdentity>> codes = new HashMap<>();
    private final Map<String, Map<Long, ClientSyncIdentity>> documents = new HashMap<>();

    public AngryboxClientMatcher(List<ClientSyncIdentity> clients, CodeTarget codeTarget, MatchingMode mode) {
        this.codeTarget = codeTarget;
        this.mode = mode;
        for (ClientSyncIdentity client : clients) {
            index(codes, codeTarget == CodeTarget.ID ? client.id.toString() : text(client.externalCode), client);
            Document dni = document(client.dni);
            Document tax = document(client.cuil);
            if (dni != null && !dni.tax) {
                index(documents, "D:" + dni.dni, client);
            }
            if (tax != null) {
                index(documents, (tax.tax ? "C:" : "D:") + tax.value, client);
                if (tax.dni != null) {
                    index(documents, "D:" + tax.dni, client);
                }
            }
            // CUIT entrante personal puede contrastarse con DNI si no hay CUIT remoto.
            if (tax == null && text(client.cuil).isEmpty() || tax != null && !tax.tax) {
                if (dni != null && !dni.tax) {
                    index(documents, "P:" + dni.dni, client);
                }
                if (tax != null && !tax.tax) {
                    index(documents, "P:" + tax.dni, client);
                }
            }
        }
    }

    public static class Resolution {
        public ClientSyncIdentity target;
        public final List<String> errors = new ArrayList<>();
        public final Set<Long> relatedIds = new HashSet<>();
    }

    public Resolution resolve(IncomingClient input) {
        return resolve(input, false);
    }

    public Resolution resolve(IncomingClient input, boolean allowRemoved) {
        Resolution result = new Resolution();
        String rawCode = text(input.codigoExterno);
        String code = normalizedCode(rawCode);
        Document doc = document(input.cuitCuil);
        Map<Long, ClientSyncIdentity> byCode = lookup(codes, code);
        Map<Long, ClientSyncIdentity> byDocument = new LinkedHashMap<>();
        Map<Long, ClientSyncIdentity> suggestions = new LinkedHashMap<>();
        if (doc != null) {
            byDocument.putAll(lookup(documents, (doc.tax ? "C:" : "D:") + doc.value));
            if (doc.tax && doc.dni != null) {
                byDocument.putAll(lookup(documents, "P:" + doc.dni));
                suggestions.putAll(lookup(documents, "D:" + doc.dni));
            }
        }
        suggestions.putAll(byCode);
        suggestions.putAll(byDocument);
        result.relatedIds.addAll(suggestions.keySet());

        boolean requireCode = mode == MatchingMode.CODE || mode == MatchingMode.CODE_AND_DOCUMENT;
        boolean requireDocument = mode == MatchingMode.DOCUMENT || mode == MatchingMode.CODE_AND_DOCUMENT;
        if (requireCode && code == null) {
            result.errors.add("El código externo de Angrybox está vacío o no es válido para " + codeLabel()
                    + ". Corregilo en Angrybox antes de reenviar.");
        }
        if (requireDocument && doc == null) {
            result.errors.add("El documento de Angrybox '" + shown(input.cuitCuil)
                    + "' no permite identificar DNI (1 a 8 dígitos) ni CUIT/CUIL (11 dígitos). "
                    + "Revisá el campo y quitá letras u otros caracteres; se admiten espacios, puntos y guiones.");
        }
        if (requireCode && code != null && byCode.size() != 1) {
            result.errors.add(byCode.isEmpty()
                    ? "El código externo de Angrybox '" + shown(rawCode) + "' no coincide con ningún " + codeLabel() + " de ClientManager."
                    : "El código externo de Angrybox '" + shown(rawCode) + "' es ambiguo: coincide con "
                            + byCode.size() + " clientes de ClientManager. Corregí los códigos duplicados.");
        }
        if (requireDocument && doc != null && byDocument.size() != 1) {
            result.errors.add(byDocument.isEmpty()
                    ? "El " + doc.label() + " enviado no coincide con el DNI/CUIT registrado en ClientManager."
                    : "El " + doc.label() + " enviado coincide con " + byDocument.size()
                            + " clientes de ClientManager. Revisá documentos duplicados; no se selecciona el primero.");
        }

        if (result.errors.isEmpty()) {
            if (mode == MatchingMode.CODE) {
                result.target = first(byCode);
            } else if (mode == MatchingMode.DOCUMENT) {
                result.target = first(byDocument);
            } else if (mode == MatchingMode.CODE_AND_DOCUMENT) {
                if (!byCode.keySet().equals(byDocument.keySet())) {
                    result.errors.add("El código externo y el documento de Angrybox apuntan a clientes distintos. "
                            + "Ambos deben identificar al mismo Client; no se actualizó ninguna deuda para esta fila.");
                } else {
                    result.target = first(byCode);
                }
            } else {
                resolveAlternative(result, byCode, byDocument);
            }
        }
        if (result.target != null && result.target.removed && !allowRemoved) {
            result.errors.add("El cliente identificado está anulado en ClientManager. Revisá su estado allí; "
                    + "la sincronización no lo reactiva ni actualiza su saldo.");
        }
        if (result.target != null && mode != MatchingMode.CODE && inconsistent(result.target)) {
            result.errors.add("El cliente de ClientManager tiene DNI y CUIT/CUIL incompatibles entre sí. "
                    + "Corregí su ficha antes de sincronizar por documento.");
        }
        if (!result.errors.isEmpty()) {
            if (suggestions.isEmpty()) {
                result.errors.add("No se encontraron candidatos. Revisá el código/documento en Angrybox o la ficha "
                        + "correspondiente en ClientManager. Esta operación no crea ni fusiona clientes.");
            } else {
                result.errors.add(candidateMessage(suggestions.values()));
            }
        }
        return result;
    }

    private void resolveAlternative(Resolution result, Map<Long, ClientSyncIdentity> byCode,
            Map<Long, ClientSyncIdentity> byDocument) {
        if (byCode.size() > 1 || byDocument.size() > 1) {
            result.errors.add("Hay coincidencias ambiguas por código o documento. Revisá los duplicados en ClientManager; "
                    + "el modo alternativo no permite elegir arbitrariamente un cliente.");
        } else if (!byCode.isEmpty() && !byDocument.isEmpty() && !byCode.keySet().equals(byDocument.keySet())) {
            result.errors.add("El código externo y el documento apuntan a clientes distintos de ClientManager. "
                    + "Corregí la identidad en Angrybox o las fichas del remoto; no se usa uno de los datos para ignorar al otro.");
        } else if (!byCode.isEmpty()) {
            result.target = first(byCode);
        } else if (!byDocument.isEmpty()) {
            result.target = first(byDocument);
        } else {
            result.errors.add("No hay coincidencia por código ni por documento. "
                    + "Revisá que el código corresponda a " + codeLabel() + " y que el DNI/CUIT esté correctamente cargado.");
        }
    }

    private String candidateMessage(Collection<ClientSyncIdentity> candidates) {
        StringBuilder message = new StringBuilder("Candidatos en ClientManager: ");
        int shown = 0;
        for (ClientSyncIdentity client : candidates) {
            if (shown++ == 5) {
                message.append(" Hay más candidatos; revisá los duplicados en ClientManager.");
                break;
            }
            if (shown > 1) {
                message.append("; ");
            }
            message.append(describe(client));
            if (codeTarget == CodeTarget.EXTERNAL_CODE && text(client.externalCode).isEmpty()) {
                message.append(". Cargá su código externo en ClientManager y el mismo valor en Angrybox");
            } else {
                message.append(". Para vincularlo por código, Angrybox debe enviar '")
                        .append(codeTarget == CodeTarget.ID ? client.id : shown(client.externalCode)).append("'");
            }
        }
        return message.append(" y un documento coherente cuando el modo lo requiera. "
                + "Si la ficha remota es incorrecta, editá sus datos en ClientManager.").toString();
    }

    public static String describe(ClientSyncIdentity client) {
        return "ID " + client.id + " '" + shown(client.name()) + "', código externo '" + shown(client.externalCode)
                + "', DNI '" + shown(client.dni) + "', CUIT/CUIL '" + shown(client.cuil) + "'"
                + (client.removed ? " (anulado)" : "");
    }

    private String codeLabel() {
        return codeTarget == CodeTarget.ID ? "ID numérico de Client" : "código externo (externalCode)";
    }

    private String normalizedCode(String code) {
        if (code.isEmpty()) {
            return null;
        }
        if (codeTarget == CodeTarget.EXTERNAL_CODE) {
            return code;
        }
        if (!code.matches("[0-9]+")) {
            return null;
        }
        try {
            long id = Long.parseLong(code);
            return id > 0 ? Long.toString(id) : null;
        } catch (NumberFormatException invalid) {
            return null;
        }
    }

    private static boolean inconsistent(ClientSyncIdentity client) {
        Document dni = document(client.dni);
        Document tax = document(client.cuil);
        return dni != null && !dni.tax && tax != null && tax.dni != null && !dni.dni.equals(tax.dni);
    }

    private static void index(Map<String, Map<Long, ClientSyncIdentity>> map, String key, ClientSyncIdentity client) {
        if (key != null && !key.isEmpty()) {
            map.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).put(client.id, client);
        }
    }

    private static Map<Long, ClientSyncIdentity> lookup(Map<String, Map<Long, ClientSyncIdentity>> map, String key) {
        return map.getOrDefault(key, Collections.emptyMap());
    }

    private static ClientSyncIdentity first(Map<Long, ClientSyncIdentity> values) {
        return values.values().iterator().next();
    }

    public static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String shown(String value) {
        String text = text(value);
        return text.isEmpty() ? "sin cargar" : text.length() > 180 ? text.substring(0, 180) + "…" : text;
    }

    public static class Document {
        public final boolean tax;
        public final String value;
        public final String dni;

        private Document(boolean tax, String value, String dni) {
            this.tax = tax;
            this.value = value;
            this.dni = dni;
        }

        public String label() {
            return (tax ? "CUIT/CUIL " : "DNI ") + value;
        }
    }

    /** No infiere DNI desde CUIT de personas jurídicas ni elimina letras silenciosamente. */
    public static Document document(String raw) {
        String value = text(raw);
        if (value.isEmpty() || !value.matches("[0-9 .-]+")) {
            return null;
        }
        value = value.replaceAll("[ .-]", "");
        if (value.matches("[0-9]{1,8}") && !value.matches("0+")) {
            String dni = String.format("%8s", value).replace(' ', '0');
            return new Document(false, dni, dni);
        }
        if (value.matches("[0-9]{11}") && !value.matches("0+")) {
            String prefix = value.substring(0, 2);
            boolean personal = Arrays.asList("20", "23", "24", "27").contains(prefix);
            return new Document(true, value, personal ? value.substring(2, 10) : null);
        }
        return null;
    }
}
