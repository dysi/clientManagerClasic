package com.dysi.AngryBoxClientManager.storage;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.lang.ByteArrayReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifReader;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Semaphore;
import javax.imageio.*;
import javax.imageio.stream.*;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Valida contenido real antes de reservar metadatos; nunca conserva imágenes originales. */
@Component
public class ReceiptProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ReceiptProcessor.class);
    private final PortalFileProperties limits;
    private final ReceiptStorage storage;
    // Acota el pico de memoria del decodificador por instancia, además de dimensiones y bytes.
    private final Semaphore processing = new Semaphore(1);

    public ReceiptProcessor(PortalFileProperties limits, ReceiptStorage storage) {
        this.limits = limits; this.storage = storage;
    }

    public List<PreparedReceipt> prepare(List<MultipartFile> files) {
        if (files.size() > limits.getMaxReceipts()) bad("Se superó la cantidad máxima de comprobantes.");
        if (files.isEmpty()) return List.of();
        if (!processing.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Se están procesando otros comprobantes. Esperá unos segundos y volvé a intentar.");
        List<PreparedReceipt> receipts = new ArrayList<>();
        try {
            for (MultipartFile file : files) receipts.add(prepare(file));
            return receipts;
        } catch (RuntimeException error) {
            discard(receipts);
            throw error;
        } finally { processing.release(); }
    }

    private PreparedReceipt prepare(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > limits.getMaxFileBytes()) bad("El comprobante está vacío o supera el tamaño permitido.");
        String name = originalName(file.getOriginalFilename());
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!List.of("pdf", "jpg", "jpeg", "png").contains(extension)) bad("Solo se admiten PDF, JPG y PNG.");
        byte[] input;
        try (InputStream stream = file.getInputStream()) {
            input = stream.readNBytes(Math.toIntExact(limits.getMaxFileBytes() + 1));
        } catch (IOException error) { throw invalidFile(); }
        if (input.length == 0 || input.length > limits.getMaxFileBytes()) bad("El comprobante supera el tamaño permitido.");
        byte[] stored;
        String mime;
        Integer width = null, height = null;
        try {
            if (extension.equals("pdf")) {
                if (input.length < 8 || !new String(input, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) throw invalidFile();
                // El PDF se analiza, no se renderiza ni se vuelve a escribir.
                try (PDDocument pdf = PDDocument.load(new ByteArrayInputStream(input), MemoryUsageSetting.setupMainMemoryOnly(16 * 1024 * 1024))) {
                    if (pdf.getNumberOfPages() < 1) bad("El PDF debe contener al menos una página.");
                }
                stored = input; mime = "application/pdf";
            } else {
                ImageResult image = image(input, extension);
                stored = image.bytes; width = image.width; height = image.height;
                extension = image.extension; mime = extension.equals("png") ? "image/png" : "image/jpeg";
            }
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El PDF requiere contraseña para abrirse. Adjuntá una versión que pueda abrirse sin contraseña.");
        } catch (IOException | IllegalArgumentException error) { throw invalidFile(); }
        try {
            String key = storage.stage(stored, extension);
            return new PreparedReceipt(key, name, mime, stored.length, sha256(stored), width, height);
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo guardar el comprobante. Conservá tus archivos y volvé a intentar.");
        }
    }

    private ImageResult image(byte[] bytes, String extension) throws IOException {
        try (ImageInputStream stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw invalidFile();
            ImageReader reader = readers.next();
            BufferedImage source = null, output = null;
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!(format.equals("png") && extension.equals("png"))
                        && !(format.equals("jpeg") && (extension.equals("jpg") || extension.equals("jpeg")))) throw invalidFile();
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > limits.getMaxDimension() || height > limits.getMaxDimension()
                        || (long) width * height > limits.getMaxPixels()) bad("La imagen supera las dimensiones o cantidad de píxeles permitidas.");
                int orientation = orientation(bytes, format);
                source = reader.read(0);
                boolean swap = orientation >= 5 && orientation <= 8;
                int orientedWidth = swap ? height : width, orientedHeight = swap ? width : height;
                double scale = Math.min(1d, (double) limits.getMaxImageSide() / Math.max(orientedWidth, orientedHeight));
                int targetWidth = Math.max(1, (int) Math.round(orientedWidth * scale));
                int targetHeight = Math.max(1, (int) Math.round(orientedHeight * scale));
                // RGB sobre blanco permite JPEG incluso para capturas con transparencia.
                output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = output.createGraphics();
                try {
                    graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, targetWidth, targetHeight);
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    graphics.scale((double) targetWidth / orientedWidth, (double) targetHeight / orientedHeight);
                    graphics.drawImage(source, transform(orientation, width, height), null);
                } finally { graphics.dispose(); }
                ByteArrayOutputStream result = new ByteArrayOutputStream();
                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                try (ImageOutputStream destination = new MemoryCacheImageOutputStream(result)) {
                    writer.setOutput(destination);
                    ImageWriteParam options = writer.getDefaultWriteParam();
                    options.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    options.setCompressionQuality(limits.getImageQuality());
                    // Un IIOImage nuevo no transporta EXIF, GPS ni metadatos del original.
                    writer.write(null, new IIOImage(output, null, null), options);
                } finally { writer.dispose(); }
                return new ImageResult(result.toByteArray(), targetWidth, targetHeight, "jpg");
            } finally {
                reader.dispose();
                if (source != null) source.flush();
                if (output != null) output.flush();
            }
        }
    }

    private int orientation(byte[] bytes, String format) {
        try {
            Metadata metadata;
            if (format.equals("jpeg")) {
                metadata = JpegMetadataReader.readMetadata(new ByteArrayInputStream(bytes), List.of(new ExifReader()));
            } else {
                // Leer solo eXIf de PNG: no inflar comentarios o perfiles ajenos a la orientación.
                metadata = new Metadata();
                for (int offset = 8; offset <= bytes.length - 12;) {
                    int size = ByteBuffer.wrap(bytes, offset, 4).getInt();
                    if (size < 0 || size > bytes.length - offset - 12) throw invalidFile();
                    if (new String(bytes, offset + 4, 4, StandardCharsets.US_ASCII).equals("eXIf")) {
                        new ExifReader().extract(new ByteArrayReader(Arrays.copyOfRange(bytes, offset + 8, offset + 8 + size)), metadata, 0);
                        break;
                    }
                    offset += size + 12;
                }
            }
            ExifIFD0Directory exif = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            Integer value = exif == null ? null : exif.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
            return value != null && value >= 1 && value <= 8 ? value : 1;
        } catch (com.drew.imaging.jpeg.JpegProcessingException | IOException error) { throw invalidFile(); }
    }

    private AffineTransform transform(int orientation, int width, int height) {
        switch (orientation) {
            case 2: return new AffineTransform(-1, 0, 0, 1, width, 0);
            case 3: return new AffineTransform(-1, 0, 0, -1, width, height);
            case 4: return new AffineTransform(1, 0, 0, -1, 0, height);
            case 5: return new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6: return new AffineTransform(0, 1, -1, 0, height, 0);
            case 7: return new AffineTransform(0, -1, -1, 0, height, width);
            case 8: return new AffineTransform(0, -1, 1, 0, 0, width);
            default: return new AffineTransform();
        }
    }

    public void discard(List<PreparedReceipt> receipts) {
        for (PreparedReceipt receipt : receipts) {
            try { storage.discard(receipt.key); }
            catch (IOException error) { LOG.error("No se pudo limpiar un comprobante temporal; lo revisará la reconciliación."); }
        }
    }
    private String originalName(String value) {
        if (value == null) throw invalidFile();
        String name = value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty() || name.length() > 255 || !name.contains(".")) throw invalidFile();
        return name;
    }
    private String sha256(byte[] bytes) {
        try {
            StringBuilder hash = new StringBuilder(64);
            for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) hash.append(String.format("%02x", value & 255));
            return hash.toString();
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    private void bad(String reason) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason); }
    private ResponseStatusException invalidFile() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no corresponde a un PDF, JPG o PNG válido con su extensión.");
    }
    private static class ImageResult {
        final byte[] bytes;
        final int width, height;
        final String extension;
        ImageResult(byte[] bytes, int width, int height, String extension) {
            this.bytes = bytes; this.width = width; this.height = height; this.extension = extension;
        }
    }
}
