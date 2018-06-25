package ch.adesso.openshift.backend;

import com.airhacks.rulz.jaxrsclient.JAXRSClientProvider;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static com.airhacks.rulz.jaxrsclient.JAXRSClientProvider.buildWithURI;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MessagesIT {
    private static String location;
    private static String createdId;

    @Rule
    public JAXRSClientProvider provider =
            buildWithURI("http://" + System.getenv("HOST") + ":" + System.getenv("PORT") + "/backend/resources");

    @After
    public void tearUp() {
        provider.client().close();
    }

    @Test(timeout = 2_000L)
    public void a01_shouldAddMessage() throws IOException {
        JsonObjectBuilder message = Json.createObjectBuilder();
        JsonObject messageToAdd = message
                .add("value", "eduCamp")
                .build();

        Response postResponse = provider
                .target()
                .path("messages")
                .request()
                .post(Entity.json(messageToAdd));
        assertThat(postResponse.getStatus(), is(201));
        location = postResponse.getHeaderString(LOCATION);
        System.out.println("location = " + location);
    }

    @Test(timeout = 2_000L)
    public void a02_shouldReturnMessage() throws IOException {
        JsonObject message = provider
                .client()
                .target(location)
                .request()
                .get(JsonObject.class);
        createdId = message.getString("id");
        assertThat(message.getString("value"), is("eduCamp"));
    }

    @Test(timeout = 2_000L)
    public void a03_shouldReturnAllMessagesWithNewMessage() throws IOException {
        JsonArray messages = provider
                .target()
                .path("messages")
                .request()
                .get(JsonArray.class);
        assertThat(messages.toString(), containsString(createdId));
    }

}