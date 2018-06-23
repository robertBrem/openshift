package ch.adesso.openshift.backend.hello.boundary;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("hello")
public class HelloResource {

    @GET
    public JsonArray getHello() {
        JsonObject helloVSHN = Json.createObjectBuilder()
                .add("hello", "vshn")
                .build();

        return Json.createArrayBuilder()
                .add(helloVSHN)
                .build();
    }
}
