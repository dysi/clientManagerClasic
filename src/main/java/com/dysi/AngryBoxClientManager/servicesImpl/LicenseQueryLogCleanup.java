package com.dysi.AngryBoxClientManager.servicesImpl;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LicenseQueryLogCleanup {
    private static final Logger LOG = LoggerFactory.getLogger(LicenseQueryLogCleanup.class);
    private final LicenseQueryLogService service;
    public LicenseQueryLogCleanup(LicenseQueryLogService service) { this.service = service; }

    @Scheduled(cron = "${license.query-log.cleanup-cron:0 0 3 * * *}",
            zone = "${license.query-log.time-zone:America/Argentina/Buenos_Aires}")
    public void cleanup() {
        Instant cutoff = service.retentionCutoff();
        long deleted = 0;
        int batch;
        do {
            batch = service.purgeBatch(cutoff);
            deleted += batch;
        } while (batch == 1000);
        if (deleted > 0) LOG.info("Retención de bitácora de licencias: {} registros eliminados.", deleted);
    }
}
