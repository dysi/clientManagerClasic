package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.security.PortalPrincipal;
import com.dysi.AngryBoxClientManager.servicesImpl.PortalAccessService;
import com.dysi.AngryBoxClientManager.servicesImpl.PortalReadService;
import com.dysi.AngryBoxClientManager.servicesImpl.PortalPaymentService;
import com.dysi.AngryBoxClientManager.dtos.PortalInputs;
import com.dysi.AngryBoxClientManager.dtos.PortalViews;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/web/portal")
public class WebPortalController {
    private final PortalAccessService access;
    private final PortalReadService read;
    private final PortalPaymentService payments;
    private final com.dysi.AngryBoxClientManager.servicesImpl.ReceiptAccessService receipts;
    public WebPortalController(PortalAccessService access, PortalReadService read, PortalPaymentService payments, com.dysi.AngryBoxClientManager.servicesImpl.ReceiptAccessService receipts) {
        this.access = access; this.read = read; this.payments = payments; this.receipts = receipts;
    }

    @GetMapping("/context")
    @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public Map<String, String> context(@AuthenticationPrincipal PortalPrincipal principal,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return access.context(principal);
    }

    @GetMapping("/overview") @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public PortalViews.Overview overview(@AuthenticationPrincipal PortalPrincipal principal) { return read.overview(principal); }

    @GetMapping("/licenses") @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public Page<PortalViews.License> licenses(@AuthenticationPrincipal PortalPrincipal principal,
            @RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) { return read.licenses(principal, keyword, page, size); }

    @GetMapping("/accounts") @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public Page<PortalViews.Account> accounts(@AuthenticationPrincipal PortalPrincipal principal,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return read.accounts(principal, page, size);
    }

    @GetMapping("/payment-form") @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public PortalViews.FormContext form(@AuthenticationPrincipal PortalPrincipal principal) { return read.form(principal); }

    @GetMapping("/payments") @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public Page<PortalViews.Payment> payments(@AuthenticationPrincipal PortalPrincipal principal,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return read.payments(principal, page, size);
    }

    @PostMapping(value = "/payments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PORTAL_SUBMIT')")
    public PortalViews.Payment submit(@AuthenticationPrincipal PortalPrincipal principal,
            @Valid @RequestPart("data") PortalInputs.Submission input,
            @RequestPart(value = "receipts", required = false) List<MultipartFile> receipts) {
        return payments.submit(principal, input, receipts == null ? List.of() : receipts);
    }

    @GetMapping("/payments/{id}") @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public PortalViews.PaymentDetail detail(@AuthenticationPrincipal PortalPrincipal principal, @PathVariable Long id) {
        return read.detail(principal, id);
    }

    @GetMapping("/payments/{id}/attachments") @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public Page<PortalViews.Receipt> attachments(@AuthenticationPrincipal PortalPrincipal principal, @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return read.receipts(principal, id, page, size);
    }

    @GetMapping("/payments/{id}/attachments/{attachmentId}") @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> attachment(
            @AuthenticationPrincipal PortalPrincipal principal, @PathVariable Long id, @PathVariable Long attachmentId,
            @RequestParam(defaultValue = "false") boolean download) {
        return WebReceiptController.response(receipts.openOwn(principal, id, attachmentId), download);
    }

    @PostMapping(value = "/payments/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PORTAL_SUBMIT')")
    public Map<String, Boolean> append(@AuthenticationPrincipal PortalPrincipal principal, @PathVariable Long id,
            @Valid @RequestPart("data") PortalInputs.AdditionalReceipts input,
            @RequestPart(value = "receipts", required = false) List<MultipartFile> files) {
        payments.append(principal, id, input, files == null ? List.of() : files); return Map.of("saved", true);
    }

    @GetMapping("/payments/{id}/comments") @PreAuthorize("hasAuthority('PORTAL_VIEW')")
    public Page<PortalViews.Comment> comments(@AuthenticationPrincipal PortalPrincipal principal, @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return read.comments(principal, id, page, size);
    }

    @PostMapping("/payments/{id}/comments") @PreAuthorize("hasAuthority('PORTAL_COMMENT')")
    public PortalViews.Comment comment(@AuthenticationPrincipal PortalPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody PortalInputs.PublicComment input) { return payments.comment(principal, id, input); }
}
