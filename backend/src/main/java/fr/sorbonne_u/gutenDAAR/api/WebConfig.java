package fr.sorbonne_u.gutenDAAR.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration of the REST API.
 *
 * <p>CORS: the demo runs on several machines of the same LAN, so the frontend (e.g. the Vite dev server
 * on another machine) is not on the same origin as the backend. The allowed origins are set by the
 * {@code gutendaar.cors.allowed-origins} property. The API is read-only, so only GET is allowed.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Allowed origin patterns, e.g. {@code *} or {@code http://192.168.1.*:5173}. */
    protected final String[] allowedOrigins;

    public WebConfig(@Value("${gutendaar.cors.allowed-origins:*}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET");
    }
}
