package ch.adesso.openshift.backend.messages.boundary;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("messages")
public class MessagesResource {

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
