package com.dysi.AngryBoxClientManager.storage;

import javax.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "portal.files")
public class PortalFileProperties {
    @NotBlank private String directory = java.nio.file.Paths.get(System.getProperty("user.home"), ".clientmanager", "receipts").toString();
    @Min(1) @Max(20) private int maxReceipts = 5;
    @Min(1024) @Max(52428800) private long maxFileBytes = 10485760;
    @Min(1) @Max(80000000) private long maxPixels = 40000000;
    @Min(1) @Max(20000) private int maxDimension = 12000;
    @Min(100) @Max(4000) private int maxImageSide = 2000;
    @DecimalMin("0.1") @DecimalMax("1.0") private float imageQuality = 0.8f;
    @Min(3600) private long orphanGraceSeconds = 86400;

    public String getDirectory() { return directory; }
    public void setDirectory(String directory) { this.directory = directory; }
    public int getMaxReceipts() { return maxReceipts; }
    public void setMaxReceipts(int maxReceipts) { this.maxReceipts = maxReceipts; }
    public long getMaxFileBytes() { return maxFileBytes; }
    public void setMaxFileBytes(long maxFileBytes) { this.maxFileBytes = maxFileBytes; }
    public long getMaxPixels() { return maxPixels; }
    public void setMaxPixels(long maxPixels) { this.maxPixels = maxPixels; }
    public int getMaxDimension() { return maxDimension; }
    public void setMaxDimension(int maxDimension) { this.maxDimension = maxDimension; }
    public int getMaxImageSide() { return maxImageSide; }
    public void setMaxImageSide(int maxImageSide) { this.maxImageSide = maxImageSide; }
    public float getImageQuality() { return imageQuality; }
    public void setImageQuality(float imageQuality) { this.imageQuality = imageQuality; }
    public long getOrphanGraceSeconds() { return orphanGraceSeconds; }
    public void setOrphanGraceSeconds(long orphanGraceSeconds) { this.orphanGraceSeconds = orphanGraceSeconds; }
}
