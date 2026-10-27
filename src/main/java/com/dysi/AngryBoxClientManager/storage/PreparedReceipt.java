package com.dysi.AngryBoxClientManager.storage;

/** Solo describe bytes ya validados y procesados en el almacenamiento privado. */
public final class PreparedReceipt {
    public final String key, originalName, mimeType, sha256;
    public final long storedSize;
    public final Integer width, height;

    public PreparedReceipt(String key, String originalName, String mimeType, long storedSize,
            String sha256, Integer width, Integer height) {
        this.key = key; this.originalName = originalName; this.mimeType = mimeType;
        this.storedSize = storedSize; this.sha256 = sha256; this.width = width; this.height = height;
    }
}
