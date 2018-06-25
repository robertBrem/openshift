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
public class WITBIT {
    private static final String THOMAS = "Thomas";

    @Rule
    public JAXRSClientProvider provider =
            buildWithURI("http://" + System.getenv("HOST") + ":" + System.getenv("PORT") + "/backend/resources");

    @After
    public void tearUp() {
        provider.client().close();
    }

    @Test(timeout = 2_000L)
    public void a01_shouldAddMessage() throws IOException {
        String whiIsTheBest = provider
                .target()
                .path("whoisthebest")
                .request()
                .get(String.class);
        assertThat(whiIsTheBest, is(THOMAS));
    }
}