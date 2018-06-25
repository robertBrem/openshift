package ch.adesso.openshift.backend.messages.boundary;

import ch.adesso.openshift.backend.messages.entity.Message;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@Path("messages")
public class MessagesResource {

    @Inject
    MessageProvider provider;

    @Context
    UriInfo uriInfo;

    @GET
    public JsonArray getAll() {
        return provider.getAll()
                .stream()
                .map(Message::getJson)
                .collect(Json::createArrayBuilder,
                        JsonArrayBuilder::add,
                        JsonArrayBuilder::add)
                .build();
    }

    @GET
    @Path("{id}")
    public JsonObject get(@PathParam("id") String id) {
        return provider.get(id).getJson();
    }

    @POST
    public Response create(JsonObject message) {
        Message created = provider.create(new Message(message));

        final URI uri = uriInfo
                .getRequestUriBuilder()
                .path(MessagesResource.class, "get")
                .build(created.getId());
        return Response.created(uri).build();

    }
}
