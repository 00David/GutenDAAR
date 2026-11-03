package fr.sorbonne_u.gutenDAAR.api.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serve the frontend for its client-side routes.
 *
 * <p>The React app (built into {@code static/}) handles its own routes, such as {@code /book/42}.
 * When such a URL is opened directly or reloaded, the browser asks the backend for it: forward it
 * to {@code index.html} so that the React router can display the page.</p>
 */
@Controller
public class FrontendController {

    @GetMapping("/book/{id}")
    public String book() {
        return "forward:/index.html";
    }
}
