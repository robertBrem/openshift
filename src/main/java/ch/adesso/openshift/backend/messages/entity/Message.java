package ch.adesso.openshift.backend.messages.entity;

import lombok.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@NamedQueries(
        @NamedQuery(name = "findAll", query = "SELECT m FROM Message m")
)
public class Message {
    @Id
    private String id;
    private String value;

    public Message(JsonObject message) {
        this.id = UUID.randomUUID().toString();
        this.value = message.getString("value");
    }

    public JsonObject getJson() {
        return Json.createObjectBuilder()
                .add("id", id)
                .add("value", value)
                .build();
    }

}
