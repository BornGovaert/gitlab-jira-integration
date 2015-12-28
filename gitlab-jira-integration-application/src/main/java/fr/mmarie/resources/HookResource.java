package fr.mmarie.resources;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import fr.mmarie.api.gitlab.Event;
import fr.mmarie.core.IntegrationService;
import io.dropwizard.auth.Auth;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.security.Principal;
import java.util.Date;

@Path("/hook")
@Slf4j
public class HookResource {

    public static final String GITLAB_HEADER = "X-Gitlab-Event";

    private final IntegrationService service;

    private final MetricRegistry metricRegistry;

    @Inject
    public HookResource(IntegrationService service,
                        Environment environment) {
        this.service = service;
        this.metricRegistry = environment.metrics();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    public void hook(@Auth Principal principal,
                     @HeaderParam(GITLAB_HEADER) String gitLabHeader,
                     @Valid Event event) {
        new Thread(() -> {
            setThreadName(principal);
            metricRegistry.counter(principal.getName()).inc();

            log.info("Hook received > {}", event);
            switch (event.getType()) {
                case PUSH:
                case TAG_PUSH:
                    service.performPushEvent(event);
                    break;
            }
        }).start();
    }

    private void setThreadName(Principal principal) {
        final String requestId = String.format("%s-%d",
                principal.getName(),
                new Date().getTime());
        Thread.currentThread().setName(requestId);

        log.info("setThreadName(): initialized new requestId <{}>", requestId);
    }

}