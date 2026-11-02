package com.dysi.AngryBoxClientManager.config;

import com.dysi.AngryBoxClientManager.security.LicenseQueryLogFilter;
import com.dysi.AngryBoxClientManager.servicesImpl.LicenseQueryLogService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;

@Configuration
public class LicenseQueryLogConfig {
    @Bean public LicenseQueryLogFilter licenseQueryLogFilter(LicenseQueryLogService service) {
        return new LicenseQueryLogFilter(service);
    }
    @Bean public FilterRegistrationBean<LicenseQueryLogFilter> licenseLogRegistration(LicenseQueryLogFilter filter) {
        FilterRegistrationBean<LicenseQueryLogFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
