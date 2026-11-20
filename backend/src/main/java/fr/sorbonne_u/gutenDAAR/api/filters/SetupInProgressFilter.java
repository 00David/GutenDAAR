package fr.sorbonne_u.gutenDAAR.api.filters;

import fr.sorbonne_u.gutenDAAR.setup.DataSetup;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Reject every HTTP request with a {@code 503 Service Unavailable} while the database setup is running.
 *
 * <p>
 * The server starts listening before the {@link ApplicationRunner}s run
 * (and so before {@link DataSetup}): without this filter, clients would
 * query a partially built database.
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SetupInProgressFilter extends OncePerRequestFilter {

    /** Message shown to the user while the database is being built. */
    protected static final String SETUP_MESSAGE = "The database is being built, please try again shortly.";

    /** The injected application availability, holding the readiness state. */
    protected final ApplicationAvailability availability;

    public SetupInProgressFilter(ApplicationAvailability availability) {
        this.availability = availability;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (availability.getReadinessState() != ReadinessState.ACCEPTING_TRAFFIC) {
            // Body written directly (not with sendError): Spring's error page would hide the message,
            // while the frontend displays the "message" field of the JSON body
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\": \"" + SETUP_MESSAGE + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
