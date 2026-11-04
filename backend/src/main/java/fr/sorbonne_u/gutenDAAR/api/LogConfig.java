package fr.sorbonne_u.gutenDAAR.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Log every HTTP request received by the server, once, before it is handled.
 *
 * <p>
 * Based on Spring's {@link CommonsRequestLoggingFilter}: logging is enabled at the info level
 * (instead of debug), and the "After request" message is disabled.
 * </p>
 */
@Configuration
public class LogConfig extends CommonsRequestLoggingFilter {

    @Override
    protected boolean shouldLog(HttpServletRequest request) {
        return logger.isInfoEnabled();
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {}
}
