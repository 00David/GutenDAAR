package fr.sorbonne_u.gutenDAAR.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Log every HTTP request received by the server, once, before it is handled.
 * For instance: {@code Request received on 'GET /api/search/keyword?query=whale&rank=occurrences'}.
 *
 * <p>
 * Based on Spring's {@link CommonsRequestLoggingFilter}: logging is done at the info level
 * (instead of debug), and the "After request" message is disabled.
 * </p>
 */
@Component
public class LogConfig extends CommonsRequestLoggingFilter {

    public LogConfig() {
        setIncludeQueryString(true);
        setBeforeMessagePrefix("Request received on '");
        setBeforeMessageSuffix("'");
    }

    @Override
    protected boolean shouldLog(HttpServletRequest request) {
        return logger.isInfoEnabled();
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
        logger.info(message);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
        // The default logging message after each request is disabled.
    }
}
