package com.hbelmiro.investing.api;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class SpaRoutingFilter {

    void init(@Observes Router router) {
        router.get().handler(ctx -> {
            String path = ctx.normalizedPath();
            boolean isApiRoute = path.startsWith("/api/")
                    || path.startsWith("/brazil_stocks")
                    || path.startsWith("/us_stocks")
                    || path.startsWith("/hello")
                    || path.startsWith("/q/");
            boolean isStaticFile = path.contains(".");

            if (!isApiRoute && !isStaticFile) {
                ctx.reroute("/index.html");
            } else {
                ctx.next();
            }
        });
    }
}
