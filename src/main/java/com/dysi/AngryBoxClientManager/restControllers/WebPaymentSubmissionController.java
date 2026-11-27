package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.PortalInputs;
import com.dysi.AngryBoxClientManager.dtos.PortalViews;
import com.dysi.AngryBoxClientManager.servicesImpl.PortalPaymentService;
import com.dysi.AngryBoxClientManager.servicesImpl.PortalReadService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/web/api/clients/{clientId}/payment-submissions")
@PreAuthorize("hasRole('ADMIN')")
public class WebPaymentSubmissionController {
    private final PortalReadService read;
    private final PortalPaymentService payments;
    public WebPaymentSubmissionController(PortalReadService read, PortalPaymentService payments) {
        this.read = read; this.payments = payments;
    }
    @GetMapping("/form")
    public PortalViews.FormContext form(@PathVariable Long clientId) { return read.adminForm(clientId); }
    @GetMapping("/licenses")
    public Page<PortalViews.License> licenses(@PathVariable Long clientId,
            @RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return read.adminLicenses(clientId, keyword, page, size);
    }
    @PostMapping(value = "/approve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PortalViews.Payment submitAndApprove(@PathVariable Long clientId,
            @Valid @RequestPart("data") PortalInputs.Submission input,
            @RequestPart(value = "receipts", required = false) List<MultipartFile> receipts) {
        return payments.submitAndApproveForClient(clientId, input, receipts == null ? List.of() : receipts);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PortalViews.Payment submit(@PathVariable Long clientId,
            @Valid @RequestPart("data") PortalInputs.Submission input,
            @RequestPart(value = "receipts", required = false) List<MultipartFile> receipts) {
        return payments.submitForClient(clientId, input, receipts == null ? List.of() : receipts);
    }
}
