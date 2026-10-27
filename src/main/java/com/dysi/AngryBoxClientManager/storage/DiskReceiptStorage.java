package com.dysi.AngryBoxClientManager.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

/** Las rutas nunca derivan del nombre original ni se publican como recursos estáticos. */
@Component
public class DiskReceiptStorage implements ReceiptStorage {
    private final Path root, pending, stored;

    public DiskReceiptStorage(PortalFileProperties properties, DataSourceProperties database) throws IOException {
        Path configured = Paths.get(properties.getDirectory()).toAbsolutePath().normalize();
        Files.createDirectories(configured);
        root = configured.toRealPath();
        if (root.getParent() == null || root.equals(Paths.get(System.getProperty("user.home")).toRealPath())) {
            throw new IllegalArgumentException("Elegí una carpeta exclusiva para los comprobantes.");
        }
        // Evitar ubicaciones públicas habituales del proyecto y sus recursos empaquetados.
        String path = root.toString().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
        if (path.contains("/static/") || path.endsWith("/static") || path.contains("/webapp/") || path.endsWith("/webapp")) {
            throw new IllegalArgumentException("Los comprobantes deben guardarse fuera de directorios públicos.");
        }
        bindDatabase(database.determineUrl());
        privateDirectory(root);
        pending = privateDirectory(root.resolve("pending"));
        stored = privateDirectory(root.resolve("stored"));
    }

    private void bindDatabase(String url) throws IOException {
        // No guardar URL ni credenciales: solo la huella del destino, sin parámetros de conexión.
        String destination = url.split("[?;]", 2)[0].replaceFirst("(?<=//)[^/@]*@", "");
        StringBuilder identity = new StringBuilder(64);
        try {
            for (byte value : MessageDigest.getInstance("SHA-256").digest(destination.getBytes(StandardCharsets.UTF_8))) {
                identity.append(String.format("%02x", value & 255));
            }
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
        Path marker = root.resolve(".clientmanager-database");
        if (!Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<Path> entries = Files.list(root)) {
                if (entries.anyMatch(entry -> !List.of("pending", "stored").contains(entry.getFileName().toString()))) {
                    throw new IOException("Elegí una carpeta vacía y exclusiva para los comprobantes.");
                }
            }
            for (String name : List.of("pending", "stored")) {
                Path directory = root.resolve(name);
                if (Files.exists(directory)) {
                    if (Files.isSymbolicLink(directory)) throw new IOException("El almacenamiento no admite enlaces.");
                    try (Stream<Path> files = Files.list(directory)) {
                        if (files.findAny().isPresent()) throw new IOException("El directorio contiene archivos sin identificación de base. Revisalo antes de reutilizarlo.");
                    }
                }
            }
            try { Files.writeString(marker, identity, StandardOpenOption.CREATE_NEW); }
            catch (FileAlreadyExistsException ignored) { /* Otra instancia pudo identificar la misma carpeta. */ }
        }
        if (Files.isSymbolicLink(marker) || !Files.readString(marker).trim().equals(identity.toString())) {
            throw new IOException("El directorio de comprobantes pertenece a otra base. Configurá una carpeta exclusiva para este destino.");
        }
    }

    private Path privateDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
        if (Files.isSymbolicLink(directory)) throw new IOException("No se admiten enlaces en el almacenamiento privado.");
        try { Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------")); }
        catch (UnsupportedOperationException ignored) { /* En Windows se usan los permisos del servicio. */ }
        return directory;
    }

    @Override
    public String stage(byte[] bytes, String extension) throws IOException {
        if (!List.of("pdf", "jpg", "png").contains(extension)) throw new IOException("Extensión interna inválida.");
        String key = UUID.randomUUID() + "." + extension;
        Path file = resolve(pending, key);
        try {
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try { Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------")); }
            catch (UnsupportedOperationException ignored) { }
            return key;
        } catch (IOException error) {
            Files.deleteIfExists(file);
            throw error;
        }
    }

    @Override
    public void promote(String key) throws IOException {
        // Ambas carpetas están en el mismo volumen: un rename atómico precede al commit JPA.
        Files.move(resolve(pending, key), resolve(stored, key), StandardCopyOption.ATOMIC_MOVE);
    }

    @Override public void discardPending(String key) throws IOException { Files.deleteIfExists(resolve(pending, key)); }
    @Override public void discard(String key) throws IOException {
        discardPending(key); Files.deleteIfExists(resolve(stored, key));
    }
    @Override public InputStream open(String key) throws IOException {
        return Files.newInputStream(resolve(stored, key), LinkOption.NOFOLLOW_LINKS);
    }
    @Override public List<String> staleKeys(boolean staging, Instant before) throws IOException {
        try (Stream<Path> files = Files.list(staging ? pending : stored)) {
            return files.filter(file -> Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
                    .filter(file -> file.getFileName().toString().matches("[a-f0-9-]{36}\\.(pdf|jpg|png)"))
                    .filter(file -> olderThan(file, before)).map(file -> file.getFileName().toString()).collect(Collectors.toList());
        }
    }
    private boolean olderThan(Path file, Instant before) {
        try { return Files.getLastModifiedTime(file, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(before); }
        catch (IOException error) { return false; }
    }
    private Path resolve(Path directory, String key) throws IOException {
        if (key == null || !key.matches("[a-f0-9-]{36}\\.(pdf|jpg|png)")) throw new IOException("Clave interna inválida.");
        Path file = directory.resolve(key).normalize();
        if (!file.startsWith(root) || Files.isSymbolicLink(file)) throw new IOException("Ruta de almacenamiento inválida.");
        return file;
    }
}
