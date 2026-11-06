package com.dysi.AngryBoxClientManager.storage;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/** Almacenamiento privado reemplazable; todas las claves las genera el servidor. */
public interface ReceiptStorage {
    String stage(byte[] bytes, String extension) throws IOException;
    void promote(String key) throws IOException;
    void discard(String key) throws IOException;
    InputStream open(String key) throws IOException;
    List<String> staleKeys(boolean pending, Instant before) throws IOException;
    void discardPending(String key) throws IOException;
}
