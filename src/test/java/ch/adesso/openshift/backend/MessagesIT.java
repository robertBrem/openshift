package ch.adesso.openshift.backend;

import com.airhacks.rulz.jaxrsclient.JAXRSClientProvider;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.json.JsonArray;

import static com.airhacks.rulz.jaxrsclient.JAXRSClientProvider.buildWithURI;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MessagesIT {
    @Rule
    public JAXRSClientProvider provider =
            buildWithURI("http://" + System.getenv("HOST") + ":" + System.getenv("PORT") + "/backend/resources");

    @After
    public void tearUp() {
        provider.client().close();
    }

    @Test(timeout = 2_000L)
    public void a01_shouldReturnListOfMessages() {
        JsonArray coins = provider
                .target()
                .path("messages")
                .request()
                .get(JsonArray.class);
        assertThat(coins.toString(), containsString("vshn"));
    }

}