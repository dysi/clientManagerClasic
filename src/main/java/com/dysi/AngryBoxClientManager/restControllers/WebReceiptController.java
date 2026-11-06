package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.servicesImpl.ReceiptAccessService;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Acceso administrativo a comprobantes privados. */
@RestController
@RequestMapping("/web/api/payments/{paymentId}/attachments")
@PreAuthorize("hasRole('ADMIN')")
public class WebReceiptController {
    private final ReceiptAccessService receipts;
    public WebReceiptController(ReceiptAccessService receipts) { this.receipts = receipts; }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<InputStreamResource> content(@PathVariable Long paymentId, @PathVariable Long attachmentId,
            @RequestParam(defaultValue = "false") boolean download) {
        ReceiptAccessService.Content file = receipts.open(paymentId, attachmentId);
        return response(file, download);
    }

    static ResponseEntity<InputStreamResource> response(ReceiptAccessService.Content file, boolean download) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mimeType))
                .contentLength(file.size).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox; default-src 'none'; frame-ancestors 'self'")
                .header(HttpHeaders.CONTENT_DISPOSITION, (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                        .filename(file.name, StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(file.stream));
    }
}
