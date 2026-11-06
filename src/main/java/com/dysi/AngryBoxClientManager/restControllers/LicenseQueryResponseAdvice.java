package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.security.LicenseQueryLogFilter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Observa el JSON sin modificar el controlador ni el contrato de la versión anterior. */
@ControllerAdvice(assignableTypes = {ClientRestController.class, LicenseV2Controller.class})
public class LicenseQueryResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) { return true; }
    @Override public Object beforeBodyWrite(Object body, MethodParameter method, MediaType type,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ObjectNode && request instanceof ServletServerHttpRequest) {
            javax.servlet.http.HttpServletRequest servlet = ((ServletServerHttpRequest) request).getServletRequest();
            if (LicenseQueryLogFilter.isLicensePath(servlet.getRequestURI().substring(servlet.getContextPath().length())))
                servlet.setAttribute(LicenseQueryLogFilter.RESPONSE, body);
        }
        return body;
    }
}
