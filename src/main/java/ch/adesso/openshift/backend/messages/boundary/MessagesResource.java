package ch.adesso.openshift.backend.messages.boundary;

import ch.adesso.openshift.backend.messages.control.MessageProvider;
import ch.adesso.openshift.backend.messages.entity.Message;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@Path("messages")
public class MessagesResource {

    @Inject
    MessageProvider provider;

    @GET
    public JsonArray getAll() {
        JsonArrayBuilder messageBuilder = Json.createArrayBuilder();
        provider.getAll()
                .stream()
                .map(m -> m.getJson())
                .forEach(m -> messageBuilder.add(m));
        return messageBuilder
                .build();
    }

    @GET
    @Path("{id}")
    public JsonObject get(@PathParam("id") String id) {
        return provider.get(id).getJson();
    }

    @POST
    public void create(JsonObject message) {
        provider.create(new Message(message));
    }
}
