package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.PaymentReviewInputs;
import com.dysi.AngryBoxClientManager.dtos.PaymentReviewViews;
import com.dysi.AngryBoxClientManager.servicesImpl.PaymentReviewService;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/api/payments/{id}")
@PreAuthorize("hasRole('ADMIN')")
public class WebPaymentReviewController {
    private final PaymentReviewService service;
    public WebPaymentReviewController(PaymentReviewService service) { this.service = service; }
    @GetMapping("/detail") public PaymentReviewViews.Detail detail(@PathVariable Long id) { return service.detail(id); }
    @GetMapping("/approval-licenses") public PaymentReviewViews.ApprovalLicenses approvalLicenses(@PathVariable Long id) {
        return service.approvalLicenses(id);
    }
    @GetMapping("/bank-accounts") public java.util.List<PaymentReviewViews.BankDestination> bankAccounts(@PathVariable Long id) {
        return service.bankAccounts(id);
    }
    @PostMapping("/correct") public PaymentReviewViews.Detail correct(@PathVariable Long id,
            @Valid @RequestBody PaymentReviewInputs.Correction input) { return service.correct(id, input); }
    @GetMapping("/comments") public Page<PaymentReviewViews.Comment> comments(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return service.comments(id, page, size); }
    @PostMapping("/comments") public PaymentReviewViews.Comment comment(@PathVariable Long id, @Valid @RequestBody PaymentReviewInputs.CommentInput input) { return service.comment(id, input); }
    @GetMapping("/attachments") public Page<PaymentReviewViews.Receipt> receipts(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return service.receipts(id, page, size); }
    @GetMapping("/history") public Page<PaymentReviewViews.History> history(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return service.history(id, page, size); }
    @GetMapping("/license-actions") public Page<PaymentReviewViews.LicenseEvent> actions(@PathVariable Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) { return service.licenseEvents(id, page, size); }
    @GetMapping("/licenses") public Page<PaymentReviewViews.LicenseOption> licenses(@PathVariable Long id,
            @RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) { return service.licenses(id, keyword, page, size); }
    @PostMapping("/approve-preview") public PaymentReviewViews.Preview preview(@PathVariable Long id, @Valid @RequestBody PaymentReviewInputs.Options input) { return service.preview(id, input); }
    @PostMapping("/approve") public PaymentReviewViews.Detail approve(@PathVariable Long id, @Valid @RequestBody PaymentReviewInputs.Approval input) { return service.approve(id, input); }
    @PostMapping("/reject") public PaymentReviewViews.Detail reject(@PathVariable Long id, @Valid @RequestBody PaymentReviewInputs.Rejection input) { return service.reject(id, input); }
}
