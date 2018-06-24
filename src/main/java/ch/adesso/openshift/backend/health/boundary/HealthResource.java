package ch.adesso.openshift.backend.health.boundary;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("health")
public class HealthResource {

    @GET
    public String healthy() {
        return System.getenv("VERSION");
    }
}
